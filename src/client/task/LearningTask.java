package client.task;

import clausal_discovery.configuration.Configuration;
import clausal_discovery.core.ClausalDiscovery;
import clausal_discovery.core.ClausalOptimization;
import clausal_discovery.core.Preferences;
import clausal_discovery.core.score.ClauseFunction;
import clausal_discovery.validity.ValidatedClause;
import client.setting.Goal;
import client.setting.SettingParameters;
import log.Log;
import pair.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Represents a learning task.
 *
 * @author Samuel Kolb
 */
public class LearningTask extends ParametrizedTask implements Goal.GoalVisitor<Void> {

	/**
	 * Creates a new learning task
	 * @param parameters	The setting parameters
	 */
	public LearningTask(SettingParameters parameters) {
		super(parameters);
	}

	@Override
	public void run() {
		getParameters().goal.get().accept(this);
	}

	@Override
	public Void visitConstraints() {
		Log.LOG.saveState();
		if(!getParameters().logging.get()) {
			Log.LOG.off();
		}
		List<ValidatedClause> clauses = new ClausalDiscovery(getConfiguration()).findHardConstraints();
		Log.LOG.revert();
		printResult(getParameters().printString.get(), clauses, (index, object, key) -> {
			if("number".equals(key)) {
				return index + 1;
			} else if("clause".equals(key)) {
				return object.getClause().toString();
			}
			throw new NoSuchElementException("No value for key: " + key);
		});
		return null;
	}

	@Override
	public Void visitSoftConstraints() {
		Log.LOG.saveState();
		if(!getParameters().logging.get()) {
			Log.LOG.off();
		}
		final double threshold = getParameters().threshold.get();
		final Configuration configuration = getConfiguration();
		final List<ValidatedClause> clauses = new ClausalDiscovery(configuration).findSoftConstraints(threshold);
		Log.LOG.revert();
		printResult(getParameters().printString.get(), clauses, (index, object, key) -> {
			if("number".equals(key)) {
				return index + 1;
			} else if("clause".equals(key)) {
				return object.getClause().toString();
			} else if("threshold".equals(key)) {
				return threshold;
			} else if("support".equals(key)) {
				return object.getSupport();
			}
			throw new NoSuchElementException("No value for key: " + key);
		});
		return null;
	}

	@Override
	public Void visitOptimization() {
		Log.LOG.saveState();
		if(!getParameters().logging.get()) {
			Log.LOG.off();
		}
		Preferences preferences = getParameters().preferences.get();
		ClausalOptimization clausalOptimization = new ClausalOptimization(getConfiguration());
		ClauseFunction function = clausalOptimization.getClauseFunction(preferences, getParameters().cValue.get());
		List<Pair<Double, ValidatedClause>> result = new ArrayList<>();
		for(int i = 0; i < clausalOptimization.getHardConstraints().size(); i++) {
			result.add(Pair.of(Double.POSITIVE_INFINITY, clausalOptimization.getHardConstraints().get(i)));
		}
		for(int i = 0; i < clausalOptimization.getSoftConstraints().size(); i++) {
			result.add(Pair.of(function.getWeights().get(i), clausalOptimization.getSoftConstraints().get(i)));
		}
		Log.LOG.revert();
		printResult(getParameters().printString.get(), result, (index, object, key) -> {
			if("number".equals(key)) {
				return index + 1;
			} else if("clause".equals(key)) {
				return object.getSecond().getClause().toString();
			} else if("support".equals(key)) {
				return object.getSecond().getSupport();
			} else if("weight".equals(key)) {
				return object.getFirst();
			}
			throw new NoSuchElementException("No value for key: " + key);
		});
		return null;
	}
}
