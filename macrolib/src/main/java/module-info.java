module uk.co.bithatch.macrolib {
	requires transitive com.sshtools.jfreedesktop;
	requires transitive com.sun.jna;
	requires transitive uk.co.bithatch.linuxio;
	requires com.google.gson;
	requires transitive org.slf4j;
	requires transitive org.freedesktop.dbus;
	opens uk.co.bithatch.macrolib;
	exports uk.co.bithatch.macrolib;
	exports uk.co.bithatch.macrolib.bamf;
	exports uk.co.bithatch.macrolib.wnck;
}