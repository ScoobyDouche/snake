package uk.co.bithatch.snake.lib.backend.openrazer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;

/**
 * Raw Razer feature reports over /dev/hidrawN (interface 0). Same 90 byte
 * report openrazer uses. Needs read/write access to the hidraw node (udev rule).
 */
public class RazerHidraw implements Closeable {

	public interface CLib extends Library {
		CLib INSTANCE = Native.load("c", CLib.class);

		int open(String path, int flags);

		int close(int fd);

		int ioctl(int fd, NativeLong request, byte[] buf);
	}

	public static final int STATUS_BUSY = 0x01;
	public static final int STATUS_OK = 0x02;
	public static final int STATUS_FAIL = 0x03;
	public static final int STATUS_TIMEOUT = 0x04;
	public static final int STATUS_UNSUPPORTED = 0x05;

	private static final int O_RDWR = 2;
	private static final int REPORT = 91; /* report id + 90 */

	/** Reply to a command: status byte and the argument bytes. */
	public record Reply(int status, byte[] args) {
		public boolean ok() {
			return status == STATUS_OK;
		}
	}

	private final int fd;
	private final String path;
	private int transactionId = 0x1f;

	private RazerHidraw(String path, int fd) {
		this.path = path;
		this.fd = fd;
	}

	/** Find interface 0 of the given device and open it, or return null if not found. */
	public static RazerHidraw open(int vid, int pid) throws IOException {
		String node = find(vid, pid);
		if (node == null)
			return null;
		int fd = CLib.INSTANCE.open(node, O_RDWR);
		if (fd < 0)
			throw new IOException("Cannot open " + node + " (errno " + Native.getLastError()
					+ "). Is the Snake udev rule installed?");
		return new RazerHidraw(node, fd);
	}

	static String find(int vid, int pid) throws IOException {
		String want = String.format("%08X:%08X", vid, pid);
		Path root = Path.of("/sys/class/hidraw");
		if (!Files.isDirectory(root))
			return null;
		try (Stream<Path> s = Files.list(root)) {
			for (Path h : s.sorted().toList()) {
				Path dev = h.resolve("device");
				String ue;
				try {
					ue = Files.readString(dev.resolve("uevent")).toUpperCase();
				} catch (IOException e) {
					continue;
				}
				String iface = dev.toRealPath().getParent().getFileName().toString();
				if (ue.contains(want) && iface.endsWith(":1.0"))
					return "/dev/" + h.getFileName();
			}
		}
		return null;
	}

	/** Razer devices use one of a few transaction ids (0x1f, 0x3f, 0xff). */
	public void setTransactionId(int transactionId) {
		this.transactionId = transactionId;
	}

	public int getTransactionId() {
		return transactionId;
	}

	public String getPath() {
		return path;
	}

	private static NativeLong ioc(int nr) {
		return new NativeLong((3L << 30) | ((long) REPORT << 16) | ('H' << 8) | nr);
	}

	/**
	 * Send a command and wait for its reply. Replies are matched on class, id
	 * and the first {@code echo} argument bytes, because other software
	 * (openrazer, Snake's own effects) talks to the same interface.
	 */
	public Reply send(int cls, int id, int size, byte[] args, int echo) throws IOException {
		int[] m = new int[Math.min(echo, args.length)];
		for (int i = 0; i < m.length; i++)
			m[i] = i;
		return sendMatching(cls, id, size, args, m);
	}

	/** Like {@link #send}, but only the argument bytes at {@code match} must be echoed. */
	public synchronized Reply sendMatching(int cls, int id, int size, byte[] args, int[] echoIdx) throws IOException {
		byte[] r = new byte[REPORT];
		r[2] = (byte) transactionId;
		r[6] = (byte) size;
		r[7] = (byte) cls;
		r[8] = (byte) id;
		System.arraycopy(args, 0, r, 9, args.length);
		int crc = 0;
		for (int i = 3; i < 89; i++)
			crc ^= r[i];
		r[89] = (byte) crc;
		Reply last = null;
		for (int attempt = 0; attempt < 3; attempt++) {
			if (CLib.INSTANCE.ioctl(fd, ioc(0x06), r) < 0)
				throw new IOException("Write to " + path + " failed (errno " + Native.getLastError() + ")");
			for (int i = 0; i < 60; i++) {
				try {
					Thread.sleep(i == 0 ? 20 : i < 10 ? 5 : 20);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException("Interrupted");
				}
				byte[] out = new byte[REPORT];
				if (CLib.INSTANCE.ioctl(fd, ioc(0x07), out) < 0)
					throw new IOException("Read from " + path + " failed (errno " + Native.getLastError() + ")");
				int status = out[1] & 0xff;
				if (status == STATUS_BUSY || out[7] != (byte) cls || out[8] != (byte) id)
					continue;
				boolean match = true;
				for (int j : echoIdx)
					if (out[9 + j] != args[j])
						match = false;
				if (!match)
					continue;
				byte[] a = new byte[size];
				System.arraycopy(out, 9, a, 0, Math.min(size, 80));
				last = new Reply(status, a);
				if (status != 0x00)
					return last;
			}
		}
		return last == null ? new Reply(-1, new byte[size]) : last;
	}

	@Override
	public void close() {
		CLib.INSTANCE.close(fd);
	}
}
