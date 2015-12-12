package client;

import basic.ArrayUtil;
import basic.FileUtil;
import clausal_discovery.core.LogicBase;
import clausal_discovery.core.Preferences;
import client.setting.EvaluationParameters;
import client.setting.Goal;
import client.setting.Model;
import client.setting.Problem;
import client.setting.SettingParameters;
import client.task.EfficiencyTask;
import client.task.EvaluationTask;
import client.task.IdpDebugTask;
import client.task.LearningTask;
import client.task.Task;
import log.LinkTransformer;
import log.Log;
import log.PrefixFilter;
import logic.example.Example;
import logic.theory.FileTheory;
import logic.theory.Theory;
import math.Range;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import parse.LogicParser;
import parse.ParseException;
import parse.PreferenceParser;
import time.Stopwatch;
import util.Weighted;
import vector.SafeList;
import vector.SafeListBuilder;
import vector.Vector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
		final Map<String, Problem> problems = new HashMap<>();
		final Map<String, SettingParameters> settings = new HashMap<>();
		final Map<String, Model> models = new HashMap<>();
		final Map<String, EvaluationParameters> evaluations = new HashMap<>();
		final Map<String, Task> tasks = new LinkedHashMap<>();
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

	private class DelayedTask implements Task {

		private final State state;

		private final String taskName;

		public DelayedTask(State state, String taskName) {
			this.state = state;
			this.taskName = taskName;
		}

		@Override
		public void run() {
			state.tasks.get(taskName).run();
		}
	}

	/**
	 * Execute the file client from command line.
	 * @param args	{filename}
	 */
	public static void main(String[] args) {
		try {
			Optional<String[]> tasks = args.length > 1 ? Optional.of(ArrayUtil.removeElement(args, 0)) : Optional.empty();
			FileClient client = new FileClient(new File(args[0]), tasks);
			client.run();
		} catch(IllegalStateException e) {
			Log.LOG.error().printLine(e.getMessage());
			e.printStackTrace();
		}
	}

	private final File file;

	private final Optional<Set<String>> tasks;

	/**
	 * FileClient constructor
	 * @param file    The file to load and parse
	 * @param tasks    The tasks to run
	 */
	public FileClient(File file, Optional<String[]> tasks) {
		this.file = file;
		this.tasks = tasks.map(array -> new HashSet<>(Arrays.asList(array)));
	}

	/**
	 * Execute this clients file
	 */
	public void run() {
		try {
			Log.LOG.addMessageFilter(PrefixFilter.ignore("INFO").and(PrefixFilter.ignore("CLIENT INFO")));
			Log.LOG.addTransformer(new LinkTransformer());
			JSONObject jsonObject = (JSONObject) JSONValue.parse(new FileReader(this.file));
			State state = new State();
			parse(jsonObject, "problems", state, state.problems, this::parseProblem);
			parse(jsonObject, "models", state, state.models, this::parseModel);
			parse(jsonObject, "evaluations", state, state.evaluations, this::parseEvaluation);

			if(jsonObject.containsKey("settings")) {
				JSONObject object = (JSONObject) jsonObject.get("settings");
				for(Object keyObject : object.keySet()) {
					String key = (String) keyObject;
					if(!state.settings.containsKey(key)) {
						parseSetting(key, object, state);
					}
				}
			}

			parse(jsonObject, "tasks", state, state.tasks, this::parseTask);
			Stopwatch stopwatch = new Stopwatch();
			for(String taskName : this.tasks.orElse(state.tasks.keySet())) {
				Log.LOG.formatLine("--- Running task: %s ---", taskName);
				stopwatch.start();
				if(state.tasks.containsKey(taskName)) {
					state.tasks.get(taskName).run();
				} else {
					Task task = parseTask(taskName, state);
					task.run();
				}
				Log.LOG.formatLine("--- Took %.3f seconds ---", stopwatch.stop() / 1000).newLine();
			}
		} catch(FileNotFoundException e) {
			throw new IllegalStateException(e);
		} finally {
			Log.LOG.revert();
		}
	}

	protected <T, J> void parse(JSONObject parent, String key, State state, Map<String, T> map,
										  BiFunction<J, State, T> function) {
		Log.LOG.printLine("CLIENT INFO\tParsing " + key);
		if(parent.containsKey(key)) {
			JSONObject object = (JSONObject) parent.get(key);
			for(Object k : object.keySet()) {
				Log.LOG.printLine("CLIENT INFO\t\tParsing " + k);
				//noinspection unchecked
				map.put((String) k, function.apply((J) object.get(k), state));
			}
		}
	}

	@SuppressWarnings("UnusedParameters")
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

	private SafeList<Theory> getBackground(JSONObject object) {
		Optional<File> background = object.containsKey("background")
			? Optional.of(new File(this.file.getParentFile(), (String) object.get("background")))
			: Optional.empty();
		return background.isPresent() ? new SafeList<>(new FileTheory(background.get())) : new SafeList<>();
	}

	@SuppressWarnings("UnusedParameters")
	protected Model parseModel(JSONArray object, State state) {
		SafeListBuilder<Weighted<String>> builder = SafeList.build(object.size());
		for(Object value : object) {
			JSONArray tuple = (JSONArray) value;
			builder.add(new Weighted<>((Double) tuple.get(0), (String) tuple.get(1)));
		}
		return new Model(builder.sample());
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	protected void parseSetting(String key, JSONObject parentObject, State state) {
		JSONObject object = (JSONObject) parentObject.get(key);
		SettingParameters parameters;
		if(object.containsKey("parent")) {
			Object parent = object.get("parent");
			if(!state.settings.containsKey(parent)) {
				if(parentObject.containsKey(parent)) {
					parseSetting((String) parent, parentObject, state);
				} else {
					throw new IllegalStateException(String.format("Parent does not exist: %s", parent));
				}
			}
			parameters = new SettingParameters(state.settings.get(parent));
		} else {
			parameters = new SettingParameters();
		}

		// C-Value & Threshold
		parameters.cValue.setOptional(fromJson(object, "c-value"));
		parameters.threshold.setOptional(fromJson(object, "threshold"));

		// Type
		if(object.containsKey("goal")) {
			parameters.goal.set(Goal.valueOf(((String) object.get("goal")).toUpperCase()));
		}

		// Variables & Literals
		parameters.variables.setOptional(fromJson(object, "variables"));
		parameters.literals.setOptional(fromJson(object, "literals"));

		// Problem
		if(object.containsKey("problem")) {
			parameters.problem.set(state.problems.get(object.get("problem")));
		}

				// Model
		String modelKey = "model";
		if(object.containsKey(modelKey)) {
			Problem problem = parameters.problem.get();
			parameters.model.set(state.models.get(object.get(modelKey)).getFunction(problem.getLogicBase()));
		}

		// Preferences
		String preferencesKey = "preferences";
		if(object.containsKey(preferencesKey)) {
			Problem problem = parameters.problem.get();
			PreferenceParser preferenceParser = new PreferenceParser(problem.getLogicBase().getExamples());
			File file = new File(this.file.getParentFile(), (String) object.get("preferences"));
			Preferences preferences = preferenceParser.parse(FileUtil.readFile(file));
			parameters.preferences.set(preferences);
		}

		Optional<Boolean> logging = fromJson(object, "logging");
		parameters.logging.set(logging.isPresent() && logging.get());

		// Print string
		parameters.printString.setOptional(fromJson(object, "print"));

		state.settings.put(key, parameters);
	}

	protected <T> Optional<T> fromJson(JSONObject object, String key) {
		if(object.containsKey(key)) {
			//noinspection unchecked
			return Optional.of((T) object.get(key));
		} else {
			return Optional.empty();
		}
	}

	@SuppressWarnings("UnusedParameters")
	protected EvaluationParameters parseEvaluation(JSONObject object, State state) {
		EvaluationParameters evaluation = new EvaluationParameters();
		evaluation.splitSize.set(parseRange(object, "split-size"));
		evaluation.prefSize.set(parseRange(object, "pref-size"));
		evaluation.errorSize.set(parseRange(object, "error-size"));
		return evaluation;
	}

	protected Range<Double> parseRange(JSONObject object, String key) {
		if(object.containsKey(key)) {
			Object value = object.get(key);
			if(value instanceof Double) {
				return Range.singleton((Double) value);
			} else if(value instanceof String){
				String[] parts = ((String) value).split(":");
				Double start = Double.parseDouble(parts[0]);
				if(parts.length == 1) {
					return Range.singleton(start);
				}
				Double step = Double.parseDouble(parts[1]);
				Double max = Double.parseDouble(parts[2]);
				return Range.doubleRange(start, step, max);
			}
		}
		return Range.empty();
	}

	protected Task parseTask(String taskString, State state) {
		String[] parts = taskString.split("\\(", 2);
		parts[parts.length - 1] = parts[parts.length - 1].substring(0, parts[parts.length - 1].length() - 1);
		String type = parts[0];
		String[] args = parts[1].split(",");
		if("speed".equals(type)) {
			String taskName = args[0];
			Task task = taskName.contains("(") ? parseTask(taskName, state) : new DelayedTask(state, taskName);
			return new EfficiencyTask(task, Integer.parseInt(args[1].trim()));
		} else {
			//noinspection SuspiciousMethodCalls
			SettingParameters setting = state.settings.get(args[0]);
			if("learn".equals(type)) {
				return new LearningTask(setting);
			} else if("idp-debug".equals(type)) {
				return new IdpDebugTask(setting);
			} else if("evaluate".equals(type)) {
				EvaluationParameters evaluation = state.evaluations.get(args[1]);
				int runs = Integer.parseInt(args[2]);
				return new EvaluationTask(setting, evaluation, runs);
			}
		}
		throw new IllegalStateException("Unknown type of task: " + type);
	}
}