package parse;

import vector.SafeList;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A compiled print string.
 *
 * @author Samuel Kolb
 */
public class PrintString {

	private final String formatString;

	private final SafeList<String> keys;

	public SafeList<String> getKeys() {
		return keys;
	}

	public Set<String> getKeySet() {
		return new HashSet<>(getKeys());
	}

	/**
	 * Creates a print string
	 * @param formatString	The format string
	 * @param keys			The keys
	 */
	public PrintString(String formatString, SafeList<String> keys) {
		this.formatString = formatString;
		this.keys = keys;
	}

	/**
	 * Formats this print string with the given mappings
	 * @param map	The mapping from keys to objects
	 * @return	A string
	 */
	public String format(Map<String, Object> map) {
		Object[] objects = new Object[getKeys().size()];
		for(int i = 0; i < getKeys().size(); i++) {
			if(!map.containsKey(getKeys().get(i))) {
				throw new IllegalArgumentException("Key not found: " + getKeys().get(i));
			}
			objects[i] = map.get(getKeys().get(i));
		}
		return String.format(formatString, objects);
	}
}
