package client.setting;

import clausal_discovery.core.Preferences;
import clausal_discovery.core.score.ClauseFunction;
import parse.PrintString;

/**
 * Created by samuelkolb on 03/11/15.
 *
 * @author Samuel Kolb
 */
public class SettingParameters extends Parameters {

	public final Key<Integer> variables = new Key<>();

	public final Key<Integer> literals = new Key<>();

	public final Key<Double> threshold = new Key<>();

	public final Key<Double> cValue = new Key<>();

	public final Key<ClauseFunction> model = new Key<>();

	public final Key<Problem> problem = new Key<>();

	public final Key<String> printString = new Key<>();

	public final Key<Goal> goal = new Key<>();

	public final Key<Preferences> preferences = new Key<>();

	/**
	 * Creates a new setting parameters object
	 */
	public SettingParameters() {
	}

	/**
	 * Creates a new setting parameters object, initialized with parameters from the given object
	 * @param parameters	The initial parameters
	 */
	public SettingParameters(Parameters parameters) {
		super(parameters);
	}
}
