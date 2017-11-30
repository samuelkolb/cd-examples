package parse;

import vector.SafeList;
import vector.Vector;

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

	public class State {
		public final StringBuilder builder = new StringBuilder();
		public final List<String> keys = new ArrayList<>();
	}

	private class StringParser extends ScopeParser<State> {

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
				parseState.keys.add(key);
				return true;
			}
			return false;
		}
	}

	/**
	 * Parses the string using the provided values
	 * @param string	The print string to be parsed
	 * @param values	The values to substitute
	 * @return	A formatted string
	 */
	@SuppressWarnings("unused")
	public String parse(String string, Map<String, Object> values) {
		return compile(string).format(values);
	}

	/**
	 * Compiles the given string to a print string
	 * @param string	The string to be compiled
	 * @return	A print string
	 */
	public PrintString compile(String string) {
		ParseCursor parseCursor = new ParseCursor(string);
		State state = new StringParser().parse(parseCursor, new State());
		return new PrintString(state.builder.toString(), new SafeList<>(state.keys));
	}
}
