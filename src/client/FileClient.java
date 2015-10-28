package client;

import basic.FileUtil;
import clausal_discovery.configuration.Configuration;
import clausal_discovery.core.ClausalDiscovery;
import clausal_discovery.core.ClausalOptimization;
import clausal_discovery.core.Preferences;
import clausal_discovery.core.score.ClauseFunction;
import clausal_discovery.validity.ValidatedClause;
import idp.IdpExpressionPrinter;
import log.LinkTransformer;
import log.Log;
import log.PrefixFilter;
import logic.example.Example;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import parse.PreferenceParser;
import parse.PrintStringParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A client to execute run files.
 *
 * @author Samuel Kolb
 */
public class FileClient {

	/**
	 * Execute the file client from command line.
	 * @param args	{filename}
	 */
	public static void main(String[] args) {
		new FileClient(new File(args[0])).run();
	}

	private File file;

	/**
	 * FileClient constructor
	 * @param file	The file to load and parse
	 */
	public FileClient(File file) {
		this.file = file;
	}

	/**
	 * Execute this clients file
	 */
	public void run() {
		Log.LOG.saveState().addMessageFilter(PrefixFilter.ignore("INFO")).addTransformer(new LinkTransformer());
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
		} finally {
			Log.LOG.revert();
		}
	}

	protected Configuration parseConfiguration(JSONObject object) {
		File file = new File(this.file.getParentFile(), (String) object.get("logic"));
		Optional<File> background = object.containsKey("background")
				? Optional.of(new File(this.file.getParentFile(), (String) object.get("background")))
				: Optional.empty();
		JSONObject parametersObject = (JSONObject) object.get("parameters");
		int variables = (int) (long) parametersObject.get("variables");
		int literals = (int) (long) parametersObject.get("literals");
		Configuration parsed = Configuration.fromFile(file, background, variables, literals);
		if(object.containsKey("examples")) {
			Set<String> exampleSet = new HashSet<>();
			for(Object o : ((JSONArray) object.get("examples"))) {
				exampleSet.add((String) o);
			}
			Predicate<Example> p = e -> exampleSet.contains(e.getName());
			return parsed.copy(parsed.getLogicBase().filterExamples(p));
		} else {
			return parsed;
		}
	}

	protected void runTask(JSONObject object, Map<String, Configuration> configurations) {
		@SuppressWarnings("RedundantCast")
		Configuration configuration = configurations.get((String) object.get("configuration"));
		String type = ((String) object.get("type")).toLowerCase();
		if("optimization".equals(type)) {
			runOptimization(object, configuration);
		} else if("constraints".equals(type)) {
			if(object.containsKey("threshold")) {
				runSoftConstraints(object, configuration, (double) object.get("threshold"));
			} else {
				runHardConstraints(object, configuration);
			}
		}
	}
	protected void runOptimization(JSONObject object, Configuration configuration) {
		PreferenceParser preferenceParser = new PreferenceParser(configuration.getLogicBase().getExamples());
		File file = new File(this.file.getParentFile(), (String) object.get("preferences"));
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
			values.put("clause", clause);
			Log.LOG.printLine(getPrintString(printString, values));
		}
	}


	protected void runHardConstraints(JSONObject object, Configuration configuration) {
		List<ValidatedClause> clauses = new ClausalDiscovery(configuration).findHardConstraints();
		String printString = (String) object.get("print");
		for(int i = 0; i < clauses.size(); i++) {
			Map<String, Object> values = new HashMap<>();
			values.put("number", i + 1);
			values.put("clause", clauses.get(i).getClause());
			Log.LOG.printLine(getPrintString(printString, values));
		}
	}

	protected void runSoftConstraints(JSONObject object, Configuration configuration, double threshold) {
		List<ValidatedClause> clauses = new ClausalDiscovery(configuration).findSoftConstraints(threshold);
		String printString = (String) object.get("print");
		for(int i = 0; i < clauses.size(); i++) {
			ValidatedClause clause = clauses.get(i);
			Map<String, Object> values = new HashMap<>();
			values.put("number", i + 1);
			values.put("threshold", threshold);
			values.put("support", (double) clause.getValidCount() / configuration.getLogicBase().getExamples().size());
			values.put("clause", IdpExpressionPrinter.print(clause.getClause().getFormula()));
			Log.LOG.printLine(getPrintString(printString, values));
		}
	}

	protected String getPrintString(String string, Map<String, Object> values) {
		return new PrintStringParser().parse(string, values);
	}
}