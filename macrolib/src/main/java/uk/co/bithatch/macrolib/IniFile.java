package uk.co.bithatch.macrolib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The Class IniFile.
 */
@SuppressWarnings("serial")
public class IniFile extends LinkedHashMap<String, Map<String, String>> {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		IniFile ifile = new IniFile();
		ifile.set("DEFAULT", "key1", "val1");
		ifile.set("DEFAULT", "key2", "val2");
		ifile.set("m1", "key3", "val3");
		ifile.set("m1", "key4", "val4");
		ifile.set("m2", "key5", "val5");
		ifile.save(System.out);
	}
	private Pattern _keyValue = Pattern.compile("\\s*([^=]*)=(.*)");
	private Pattern _section = Pattern.compile("\\s*\\[([^]]*)\\]\\s*");

	private boolean removeEmptySections = true;

	/**
	 * Instantiates a new ini file.
	 */
	public IniFile() {
	}

	/**
	 * Instantiates a new ini file.
	 *
	 * @param path the path
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public IniFile(Path path) throws IOException {
		load(path);
	}

	/**
	 * Contains option.
	 *
	 * @param sectionName the section name
	 * @param key the key
	 * @return true, if successful
	 */
	public boolean containsOption(String sectionName, String key) {
		return containsKey(sectionName) && get(sectionName).containsKey(key);
	}

	/**
	 * Gets the boolean.
	 *
	 * @param section the section
	 * @param key the key
	 * @param defaultvalue the defaultvalue
	 * @return the boolean
	 */
	public boolean getBoolean(String section, String key, boolean defaultvalue) {
		Map<String, String> kv = get(section);
		if (kv == null || !kv.containsKey(key)) {
			return defaultvalue;
		}
		return "true".equalsIgnoreCase(kv.get(key));
	}

	/**
	 * Gets the double.
	 *
	 * @param section the section
	 * @param key the key
	 * @param defaultvalue the defaultvalue
	 * @return the double
	 */
	public double getDouble(String section, String key, double defaultvalue) {
		Map<String, String> kv = get(section);
		if (kv == null || !kv.containsKey(key)) {
			return defaultvalue;
		}
		return Double.parseDouble(kv.get(key));
	}

	/**
	 * Gets the float.
	 *
	 * @param section the section
	 * @param key the key
	 * @param defaultvalue the defaultvalue
	 * @return the float
	 */
	public float getFloat(String section, String key, float defaultvalue) {
		Map<String, String> kv = get(section);
		if (kv == null || !kv.containsKey(key)) {
			return defaultvalue;
		}
		return Float.parseFloat(kv.get(key));
	}

	/**
	 * Gets the int.
	 *
	 * @param section the section
	 * @param key the key
	 * @param defaultvalue the defaultvalue
	 * @return the int
	 */
	public int getInt(String section, String key, int defaultvalue) {
		Map<String, String> kv = get(section);
		if (kv == null || !kv.containsKey(key)) {
			return defaultvalue;
		}
		return Integer.parseInt(kv.get(key));
	}

	/**
	 * Gets the or create.
	 *
	 * @param sectionName the section name
	 * @return the or create
	 */
	public Map<String, String> getOrCreate(String sectionName) {
		Map<String, String> section = get(sectionName);
		if (section == null) {
			section = new LinkedHashMap<>();
			put(sectionName, section);
		}
		return section;
	}

	/**
	 * Gets the string.
	 *
	 * @param section the section
	 * @param key the key
	 * @param defaultvalue the defaultvalue
	 * @return the string
	 */
	public String getString(String section, String key, String defaultvalue) {
		Map<String, String> kv = get(section);
		if (kv == null || !kv.containsKey(key)) {
			return defaultvalue;
		}
		return kv.get(key);
	}

	/**
	 * Gets the string list.
	 *
	 * @param section the section
	 * @param key the key
	 * @param separator the separator
	 * @param defaultvalues the defaultvalues
	 * @return the string list
	 */
	public String[] getStringList(String section, String key, String separator, String... defaultvalues) {
		Map<String, String> kv = get(section);
		if (kv == null || !kv.containsKey(key)) {
			return defaultvalues;
		} else {
			String s = kv.get(key);
			return s.split(separator);
		}
	}

	/**
	 * Checks if is removes the empty sections.
	 *
	 * @return true, if is removes the empty sections
	 */
	public boolean isRemoveEmptySections() {
		return removeEmptySections;
	}

	/**
	 * Key set.
	 *
	 * @param section the section
	 * @return the sets the
	 */
	public Set<String> keySet(String section) {
		return containsKey(section) ? get(section).keySet() : Collections.emptySet();
	}

	/**
	 * Load.
	 *
	 * @param reader the reader
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void load(BufferedReader reader) throws IOException {
		clear();
		String line;
		String section = null;
		while ((line = reader.readLine()) != null) {
			Matcher m = _section.matcher(line);
			if (m.matches()) {
				section = m.group(1).trim();
			} else if (section != null) {
				m = _keyValue.matcher(line);
				if (m.matches()) {
					String key = m.group(1).trim();
					String value = m.group(2).trim();
					Map<String, String> kv = get(section);
					if (kv == null) {
						put(section, kv = new HashMap<>());
					}
					kv.put(key, value);
				}
			}
		}
	}

	/**
	 * Load.
	 *
	 * @param path the path
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void load(Path path) throws IOException {
		try (BufferedReader br = Files.newBufferedReader(path)) {
			load(br);
		}
	}

	/**
	 * Removes the key.
	 *
	 * @param sectionName the section name
	 * @param key the key
	 * @return the string
	 */
	public String removeKey(String sectionName, String key) {
		Map<String, String> section = get(sectionName);
		if (section != null) {
			String v = section.remove(key);
			if (section.isEmpty() && removeEmptySections)
				remove(sectionName);
			return v;
		}
		return null;
	}

	/**
	 * Save.
	 *
	 * @param out the out
	 */
	public void save(OutputStream out) {
		save(new OutputStreamWriter(out));
	}

	/**
	 * Save.
	 *
	 * @param out the out
	 */
	public void save(Writer out) {
		PrintWriter pw = new PrintWriter(out, false);
		int secNo = 0;
		for (String sec : keySet()) {
			if(secNo > 0)
				pw.println();
			pw.println("[" + sec + "]");
			for (Map.Entry<String, String> en : get(sec).entrySet()) {
				pw.println(en.getKey() + "=" + (en.getValue() == null ? "" : en.getValue()));
			}
			secNo++;
		}
		pw.flush();
	}

	/**
	 * Sets the.
	 *
	 * @param sectionName the section name
	 * @param key the key
	 * @param value the value
	 */
	public void set(String sectionName, String key, Object value) {
		getOrCreate(sectionName).put(key, value == null ? null : String.valueOf(value));
	}

	/**
	 * Sets the removes the empty sections.
	 *
	 * @param removeEmptySections the new removes the empty sections
	 */
	public void setRemoveEmptySections(boolean removeEmptySections) {
		this.removeEmptySections = removeEmptySections;
	}
}