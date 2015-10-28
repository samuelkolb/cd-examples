package parse;

import log.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A parser for print strings (e.g. "Hello {name}, your size is {size, %.2f}").
 *
 * @author Samuel Kolb
 */
public class PrintStringParser {

	private class State {
		public StringBuilder builder = new StringBuilder();
		public List<Object> objects = new ArrayList<>();
	}

	private class StringParser extends ScopeParser<State> {

		// TODO extend parser
		@Override
		public List<ScopeParser<State>> getParsers() {
			return Collections.singletonList(new KeyParser());
		}

		@Override
		public boolean activatesWith(String string, State parseState) throws ParsingError {
			return false;
		}

		@Override
		public boolean endsWith(String string, State parseState) throws ParsingError {
			return false;
		}
	}

	private class KeyParser extends MatchParser<State> {

		@Override
		public boolean matches(String string, State parseState) throws ParsingError {
			if(string.matches(".*\\{[^\\}]*\\}")) {
				int bracketIndex = string.indexOf('{');
				parseState.builder.append(string.substring(0, bracketIndex));
				String match = string.substring(bracketIndex + 1, string.length() - 1);
				String[] parts = match.split(",");
				String key = parts[0].trim();
				String format = parts.length >= 2 ? parts[1].trim() : "%s";
				parseState.builder.append(format);
				if(!values.containsKey(key)) {
					throw new IllegalStateException("Missing key: " + key);
				}
				parseState.objects.add(values.get(key));
				return true;
			}
			return false;
		}
	}

	private Map<String, Object> values;

	/**
	 * Parses the string using the provided values
	 * @param string	The print string to be parsed
	 * @param values	The values to substitute
	 * @return	A formatted string
	 */
	public String parse(String string, Map<String, Object> values) {
		this.values = values;
		ParseCursor parseCursor = new ParseCursor(string);
		State state = new StringParser().parse(parseCursor, new State());
		this.values = null;
		return String.format(state.builder.toString(), state.objects.toArray());
	}
}
