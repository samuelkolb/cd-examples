package client;

import basic.FileUtil;
import build.Builder;
import clausal_discovery.configuration.Configuration;
import clausal_discovery.core.LogicBase;
import clausal_discovery.core.Preferences;
import clausal_discovery.core.score.ClauseFunction;
import client.setting.Goal;
import client.setting.Model;
import client.setting.Parameters;
import client.setting.Problem;
import client.setting.SettingParameters;
import client.task.EfficiencyTask;
import client.task.EvaluationTask;
import client.task.LearningTask;
import client.task.Task;
import log.LinkTransformer;
import log.Log;
import log.PrefixFilter;
import logic.example.Example;
import logic.theory.FileTheory;
import logic.theory.Theory;
import parse.LogicParser;
import parse.ParseException;
import util.Weighted;
import logic.theory.Vocabulary;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import parse.PreferenceParser;
import vector.SafeList;
import vector.SafeListBuilder;
import vector.Vector;
import vector.WriteOnceVector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * A client to execute run files.
 *
 * @author Samuel Kolb
 */
public class FileClient {

	private class State {
		Map<String, Problem> problems = new HashMap<>();
		Map<String, SettingParameters> settings = new HashMap<>();
		Map<String, Model> models = new HashMap<>();
		Map<String, Task> tasks = new HashMap<>();
	}

	private class DelayedEfficiencyTask implements Task {

		private final State state;

		private final String taskName;

		private final int runs;

		public DelayedEfficiencyTask(State state, String taskName, int runs) {
			this.state = state;
			this.taskName = taskName;
			this.runs = runs;
		}

		@Override
		public void run() {
			new EfficiencyTask(state.tasks.get(taskName), runs).run();
		}
	}

	/**
	 * Execute the file client from command line.
	 * @param args	{filename}
	 */
	public static void main(String[] args) {
		try {
			new FileClient(new File(args[0])).run();
		} catch(IllegalStateException e) {
			Log.LOG.error().printLine(e.getMessage());
		}
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
			State state = new State();
			state.problems = parse(jsonObject, "problems", state, this::parseProblem);
			state.models = parse(jsonObject, "models", state, this::parseModel);
			state.settings = parse(jsonObject, "settings", state, this::parseSetting);
			state.tasks = parse(jsonObject, "tasks", state, this::parseTask);
		} catch(FileNotFoundException e) {
			throw new IllegalStateException(e);
		} finally {
			Log.LOG.revert();
		}
	}

	protected <T, J> Map<String, T> parse(JSONObject parent, String key, State state, BiFunction<J, State, T> function) {
		JSONObject object = (JSONObject) parent.get(key);
		Map<String, T> map = new HashMap<>();
		for(Object k : object.keySet()) {
			//noinspection unchecked
			map.put((String) k, function.apply((J) object.get(k), state));
		}
		return map;
	}

	protected Problem parseProblem(JSONObject object, State state) {
		File file = new File(this.file.getParentFile(), (String) object.get("logic"));
		try {
			return new Problem(getLogicBase(object, file), getBackground(object));
		} catch(ParseException e) {
			String message = String.format("Error parsing file %s\n%s", file.getName(), e.getMessage());
			throw new IllegalStateException(message, e);
		}
	}

	private LogicBase getLogicBase(JSONObject object, File file) throws ParseException {
		LogicBase logicBase = new LogicParser().parse(FileUtil.readFile(file));
		if(object.containsKey("examples")) {
			Set<String> exampleSet = new HashSet<>();
			//noinspection Convert2streamapi
			for(Object o : ((JSONArray) object.get("examples"))) {
				exampleSet.add((String) o);
			}
			Predicate<Example> p = e -> exampleSet.contains(e.getName());
			logicBase = logicBase.filterExamples(p);
		}
		return logicBase;
	}

	private Vector<Theory> getBackground(JSONObject object) {Optional<File> background = object.containsKey("background")
			? Optional.of(new File(this.file.getParentFile(), (String) object.get("background")))
			: Optional.empty();

		return background.isPresent() ? new Vector<>(new FileTheory(background.get())) : new Vector<>();
	}

	protected Model parseModel(JSONArray object, State state) {
		SafeListBuilder<Weighted<String>> builder = SafeList.build(object.size());
		for(Object value : object) {
			JSONArray tuple = (JSONArray) value;
			builder.add(new Weighted<>((Double) tuple.get(0), (String) tuple.get(1)));
		}
		return new Model(builder.sample());
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	protected SettingParameters parseSetting(JSONObject object, State state) {
		SettingParameters parameters = object.containsKey("parent")
			? new SettingParameters(state.settings.get(object.get("parent")))
		    : new SettingParameters();

		// C-Value & Threshold
		parameters.cValue.setFromJson(object, "c-value");
		parameters.threshold.setFromJson(object, "threshold");

		// Type
		parameters.goal.set(Goal.valueOf(((String) object.get("type")).toUpperCase()));

		// Variables & Literals
		parameters.variables.setFromJson(object, "variables");
		parameters.literals.setFromJson(object, "literals");

		// Problem
		Problem problem = state.problems.get(object.get("problem"));
		parameters.problem.set(problem);

		// Model
		String modelKey = "model";
		if(object.containsKey(modelKey)) {
			parameters.model.set(state.models.get(object.get(modelKey)).getFunction(problem.getLogicBase()));
		}

		// Preferences
		String preferencesKey = "preferences";
		if(object.containsKey(preferencesKey)) {
			PreferenceParser preferenceParser = new PreferenceParser(problem.getLogicBase().getExamples());
			File file = new File(this.file.getParentFile(), (String) object.get("preferences"));
			Preferences preferences = preferenceParser.parse(FileUtil.readFile(file));
			parameters.preferences.set(preferences);
		}

		// Print string
		parameters.printString.setFromJson(object, "print-string");

		return parameters;
	}

	protected Task parseTask(String taskString, State state) {
		String[] parts = taskString.split("\\(");
		parts[parts.length - 1] = parts[parts.length - 1].substring(0, parts[parts.length - 1].length() - 1);
		String type = parts[0];
		if("speed".equals(type)) {
			String[] args = parts[1].split(",");
			return new DelayedEfficiencyTask(state, args[0], Integer.parseInt(args[1]));
		} else {
			//noinspection SuspiciousMethodCalls
			SettingParameters setting = state.settings.get(parts[1]);
			if("learn".equals(type)) {
				return new LearningTask(setting);
			} else if("evaluate".equals(type)) {
				return new EvaluationTask(setting);
			}
		}
		throw new IllegalStateException("Unknown type of task: " + type);
	}
}