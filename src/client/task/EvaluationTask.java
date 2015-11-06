package client.task;

import clausal_discovery.configuration.Configuration;
import clausal_discovery.test.OptimizationTester;
import client.setting.EvaluationParameters;
import client.setting.Goal;
import client.setting.SettingParameters;
import log.Log;
import pair.TypePair;
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
		if(evaluation.splitSize.get().isEmpty()) {
			Log.LOG.saveState().off();
			OptimizationTester tester = new OptimizationTester(getConfiguration(), getConfiguration());
			Log.LOG.revert();
			evaluate(tester, "", "");
		} else {
			for(Double split : evaluation.splitSize.get()) {
				Log.LOG.saveState().off();
				TypePair<Configuration> configurations = getConfiguration().split(split);
				OptimizationTester tester = new OptimizationTester(configurations.one(), configurations.two());
				Log.LOG.revert();
				evaluate(tester, "split | ", String.format("%.3f | ", split));
			}
		}
		return null;
	}

	private void evaluate(OptimizationTester tester, String titlePrefix, String prefix) {
		Log.LOG.printLine("Seed: " + Randomness.getSeed());
		Log.LOG.printLine(titlePrefix + "prefs | error | runs  | mean  | stDev |  min  |  max ");
		for(Double prefSize : this.evaluation.prefSize.get()) {
			for(Double errorSize : this.evaluation.errorSize.get()) {
				Log.LOG.saveState().off();
				double[] scores = new double[this.runs];
				for(int i = 0; i < this.runs; i++) {
					scores[i] = tester.test(getParameters().model.get(), prefSize, errorSize);
				}
				Log.LOG.revert();
				Statistics stats = new Statistics(scores);
				Log.LOG.formatLine("%s%.3f | %.3f | %-5d | %.3f | %.3f | %.3f | %.3f", prefix, prefSize, errorSize,
						stats.getSize(), stats.getMean(), stats.getStdDev(), stats.getMin(), stats.getMax());
			}
		}
	}
}
