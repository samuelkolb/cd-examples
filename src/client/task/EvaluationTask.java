package client.task;

import clausal_discovery.run.OptimizationTestClient;
import clausal_discovery.test.OptimizationTester;
import client.setting.EvaluationParameters;
import client.setting.Goal;
import client.setting.SettingParameters;
import log.Log;
import util.Randomness;
import util.Statistics;

/**
 * The evaluation task evaluates the accuracy an experiment.
 *
 * @author Samuel Kolb
 */
public class EvaluationTask extends ParametrizedTask implements Goal.GoalVisitor<Void> {

	private final EvaluationParameters evaluation;

	private final int runs;

	/**
	 * Creates an optimization task.
	 * @param parameters	The setting parameters
	 * @param runs			The number of times an experiment is run
	 * @param evaluation	The evaluation parameters
	 */
	public EvaluationTask(SettingParameters parameters, EvaluationParameters evaluation, int runs) {
		super(parameters);
		this.runs = runs;
		this.evaluation = evaluation;
	}

	@Override
	public void run() {
		getParameters().goal.get().accept(this);
	}

	@Override
	public Void visitConstraints() {
		throw new UnsupportedOperationException("Evaluation of constraint learning currently not supported");
	}

	@Override
	public Void visitSoftConstraints() {
		throw new UnsupportedOperationException("Evaluation of soft constraint learning currently not supported");
	}

	@Override
	public Void visitOptimization() {
		OptimizationTestClient client = new OptimizationTestClient(getConfiguration(), getParameters().model.get());
		if(evaluation.splitSize.get().isEmpty()) {
			Log.LOG.saveState().off();
			OptimizationTester tester = client.getTester();
			Log.LOG.revert();
			evaluate(client, tester, "", "");
		} else {
			for(Double split : evaluation.splitSize.get()) {
				Log.LOG.saveState().off();
				OptimizationTester tester = client.getTester(split);
				Log.LOG.revert();
				evaluate(client, tester, "split | ", String.format("%.3f | ", split));
			}
		}
		return null;
	}

	private void evaluate(OptimizationTestClient client, OptimizationTester tester, String titlePrefix, String prefix) {
		Log.LOG.printLine("Seed: " + Randomness.getSeed());
		Log.LOG.printLine(titlePrefix + "prefs | error | runs  | mean  | stDev |  min  |  max ");
		for(Double prefSize : this.evaluation.prefSize.get()) {
			for(Double errorSize : this.evaluation.errorSize.get()) {
				Log.LOG.saveState().off();
				double[] scores = new double[this.runs];
				for(int i = 0; i < this.runs; i++) {
					scores[i] = client.score(tester, prefSize, errorSize);
				}
				Log.LOG.revert();
				Statistics stats = new Statistics(scores);
				Log.LOG.formatLine("%s%.3f | %.3f | %-5d | %.3f | %.3f | %.3f | %.3f", prefix, prefSize, errorSize,
						stats.getSize(), stats.getMean(), stats.getStdDev(), stats.getMin(), stats.getMax());
			}
		}
	}
}
