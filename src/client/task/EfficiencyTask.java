package client.task;

import log.Log;
import time.Stopwatch;

import java.util.Arrays;

/**
 * Created by samuelkolb on 28/10/15.
 *
 * @author Samuel Kolb
 */
public class EfficiencyTask implements Task {

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
		for(int i = 0; i < runs; i++) {
			stopwatch.start();
			task.run();
			times[i] = stopwatch.stop();
		}
		Log.LOG.printLine(Arrays.toString(times));
	}
}
