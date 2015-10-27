package client;

import basic.FileUtil;
import clausal_discovery.configuration.Configuration;
import clausal_discovery.core.ClausalDiscovery;
import clausal_discovery.core.ClausalOptimization;
import clausal_discovery.core.Preferences;
import clausal_discovery.core.score.ClauseFunction;
import clausal_discovery.validity.ValidatedClause;
import idp.IdpExpressionPrinter;
import log.Log;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import parse.PreferenceParser;
import parse.PrintStringParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by samuelkolb on 26/10/15.
 *
 * @author Samuel Kolb
 */
public class FileClient {

	private File file;

	/**
	 * FileClient constructor
	 * @param file	The file to load and parse
	 */
	public FileClient(File file) {
		this.file = file;
	}

	public void run() {
		try {
			JSONObject jsonObject = (JSONObject) JSONValue.parse(new FileReader(this.file));
			JSONObject configurationsObject = (JSONObject) jsonObject.get("configurations");
			Map<String, Configuration> configurations = new HashMap<>();
			for(Object key : configurationsObject.keySet()) {
				String name = (String) key;
				configurations.put(name, parseConfiguration((JSONObject) configurationsObject.get(key)));
			}

			JSONArray runObject = (JSONArray) jsonObject.get("tasks");
			for(Object task : runObject) {
				runTask((JSONObject) task, configurations);
			}
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	protected Configuration parseConfiguration(JSONObject object) {
		File file = FileUtil.getLocalFile(getClass().getResource((String) object.get("logic")));
		Optional<File> background = object.containsKey("background")
				? Optional.empty()
				: Optional.of(FileUtil.getLocalFile(getClass().getResource((String) object.get("background"))));
		int variables = (int) object.get("variables");
		int literals = (int) object.get("literals");
		return Configuration.fromFile(file, background, variables, literals);
	}

	protected void runTask(JSONObject object, Map<String, Configuration> configurations) {
		@SuppressWarnings("RedundantCast")
		Configuration configuration = configurations.get((String) object.get("configuration"));
		String type = (String) object.get("type");
		switch(type.toLowerCase()) {
			case "optimization":
				runOptimization(object, configuration);
				break;
			case "hard constraints":
				runHardConstraints(object, configuration);
				break;
			case "soft constraints":
				runSoftConstraints(object, configuration);
				break;
			default:
		}
	}
	protected void runOptimization(JSONObject object, Configuration configuration) {
		PreferenceParser preferenceParser = new PreferenceParser(configuration.getLogicBase().getExamples());
		File file = FileUtil.getLocalFile(getClass().getResource((String) object.get("preferences")));
		Preferences preferences = preferenceParser.parse(FileUtil.readFile(file));
		ClausalOptimization clausalOptimization = new ClausalOptimization(configuration);
		ClauseFunction function = clausalOptimization.getClauseFunction(preferences, (double) object.get("c-factor"));
		String printString = (String) object.get("print");
		for(int i = 0; i < function.getWeights().length; i++) {
			Map<String, Object> values = new HashMap<>();
			ValidatedClause clause = clausalOptimization.getSoftConstraints().get(i);
			values.put("number", i + 1);
			values.put("support", clause.getValidCount() / configuration.getLogicBase().getExamples().size());
			values.put("weight", function.getWeights().get(i));
			values.put("clause", IdpExpressionPrinter.print(clause.getClause().getFormula()));
			Log.LOG.printLine(getPrintString(printString, values));
		}
	}


	protected void runHardConstraints(JSONObject object, Configuration configuration) {
		List<ValidatedClause> clauses = new ClausalDiscovery(configuration).findHardConstraints();
		String printString = (String) object.get("print");
		for(int i = 0; i < clauses.size(); i++) {
			Map<String, Object> values = new HashMap<>();
			values.put("number", i + 1);
			values.put("clause", IdpExpressionPrinter.print(clauses.get(i).getClause().getFormula()));
			Log.LOG.printLine(getPrintString(printString, values));
		}
	}

	protected void runSoftConstraints(JSONObject object, Configuration configuration) {
		double threshold = (double) object.get("threshold");
		List<ValidatedClause> clauses = new ClausalDiscovery(configuration).findSoftConstraints(threshold);
		String printString = (String) object.get("print");
		for(int i = 0; i < clauses.size(); i++) {
			Map<String, Object> values = new HashMap<>();
			values.put("number", i + 1);
			values.put("threshold", threshold);
			values.put("support", clauses.get(i).getValidCount() / configuration.getLogicBase().getExamples().size());
			values.put("clause", IdpExpressionPrinter.print(clauses.get(i).getClause().getFormula()));
			Log.LOG.printLine(getPrintString(printString, values));
		}
	}

	protected String getPrintString(String string, Map<String, Object> values) {
		return new PrintStringParser().parse(string, values);
	}
}
