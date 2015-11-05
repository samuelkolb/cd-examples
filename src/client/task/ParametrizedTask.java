package client.task;

import clausal_discovery.configuration.Configuration;
import clausal_discovery.core.LogicBase;
import client.setting.SettingParameters;
import log.Log;
import logic.theory.Theory;
import parse.PrintString;
import parse.PrintStringParser;
import vector.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by samuelkolb on 28/10/15.
 *
 * @author Samuel Kolb
 */
public abstract class ParametrizedTask implements Task {

	interface Extractor<T> {

		Object extract(int index, T object, String key);
	}

	private final SettingParameters parameters;

	public SettingParameters getParameters() {
		return parameters;
	}

	/**
	 * Creates a new task
	 * @param parameters	The setting parameters
	 */
	public ParametrizedTask(SettingParameters parameters) {
		this.parameters = parameters;
	}

	protected <T> void printResult(String printString, List<T> results, Extractor<T> extractor) {
		PrintString compiledString = new PrintStringParser().compile(printString);
		for(int i = 0; i < results.size(); i++) {
			Map<String, Object> map = new HashMap<>();
			for(String key : compiledString.getKeySet()) {
				map.put(key, extractor.extract(i, results.get(i), key));
			}
			Log.LOG.printLine(compiledString.format(map));
		}
	}

	protected Configuration getConfiguration() {LogicBase logicBase = getParameters().problem.get().getLogicBase();
		Vector<Theory> theories = getParameters().problem.get().getBackgroundTheories();
		int variables = (int) (long) getParameters().variables.get();
		int literals = (int) (long) getParameters().literals.get();
		return new Configuration(logicBase, theories, variables, literals);
	}
}
