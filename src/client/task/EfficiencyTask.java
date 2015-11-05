package client.task;

import log.Log;
import time.Stopwatch;
import util.Statistics;

import java.util.Arrays;

/**
 * Created by samuelkolb on 28/10/15.
 *
 * @author Samuel Kolb
 */
public class EfficiencyTask implements Task {

	public static final double MS = 1000.0;
	private final Task task;

	private final int runs;

	/**
	 * Creates a new efficiency task
	 * @param task	The task to run
	 * @param runs	The number of runs
	 */
	public EfficiencyTask(Task task, int runs) {
		this.task = task;
		this.runs = runs;
	}

	@Override
	public void run() {
		Stopwatch stopwatch = new Stopwatch();
		double[] times = new double[runs];
		Log.LOG.saveState().off();
		for(int i = 0; i < runs; i++) {
			stopwatch.start();
			task.run();
			times[i] = stopwatch.stop() / MS;
		}
		Log.LOG.revert().printLine(Arrays.toString(times));
		Statistics stats = new Statistics(times);
		Log.LOG.formatLine("Runs: %d, Mean: %f, StdDev: %f, Max: %f, Min: %f",
				stats.getSize(), stats.getMean(), stats.getStdDev(), stats.getMax(), stats.getMin()).revert();
	}
}
