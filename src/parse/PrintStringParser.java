package parse;

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

		@Override
		public List<ScopeParser<State>> getParsers() {
			return Collections.singletonList(new KeyParser());
		}

		@Override
		public boolean activatesWith(String string, State parseState) throws ParsingError {
			return true;
		}

		@Override
		public boolean endsWith(String string, State parseState) throws ParsingError {
			if(!string.isEmpty()) {
				parseState.builder.append(string);
				return false;
			}
			return true;
		}
	}

	private class KeyParser extends ScopeParser<State> {

		@Override
		public List<ScopeParser<State>> getParsers() {
			return Collections.emptyList();
		}

		@Override
		public boolean activatesWith(String string, State parseState) throws ParsingError {
			return string.startsWith("{");
		}

		@Override
		public boolean endsWith(String string, State parseState) throws ParsingError {
			if(string.endsWith("}")) {
				String[] parts = string.split(",");
				String key = parts[0].trim();
				String format = parts.length >= 2 ? parts[1].trim() : "%@";
				parseState.builder.append(format);
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
		List<ScopeParser<State>> parsers = Collections.singletonList(new StringParser());
		ParseCursor parseCursor = new ParseCursor(string);
		State state = new BaseScopeParser<>(parsers).parse(parseCursor, new State());
		this.values = null;
		return String.format(state.builder.toString(), state.objects.toArray());
	}
}
