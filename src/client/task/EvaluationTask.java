package client.task;

import client.setting.Goal;
import client.setting.SettingParameters;
import log.Log;

/**
 * Created by samuelkolb on 03/11/15.
 *
 * @author Samuel Kolb
 */
public class EvaluationTask extends ParametrizedTask implements Goal.GoalVisitor<Double> {

	/**
	 * Creates an optimization task.
	 * @param parameters	The setting parameters
	 */
	public EvaluationTask(SettingParameters parameters) {
		super(parameters);
	}

	@Override
	public void run() {
		Log.LOG.printLine(getParameters().goal.get().accept(this));
	}

	@Override
	public Double visitConstraints() {
		throw new UnsupportedOperationException("Evaluation of constraint learning currently not supported");
	}

	@Override
	public Double visitSoftConstraints() {
		throw new UnsupportedOperationException("Evaluation of soft constraint learning currently not supported");
	}

	@Override
	public Double visitOptimization() {
		return null; // TODO implement
	}
}
