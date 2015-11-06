package client.setting;

import clausal_discovery.core.Preferences;
import clausal_discovery.core.score.ClauseFunction;
import configuration.Parameters;

/**
 * Contains parameters about various settings.
 *
 * @author Samuel Kolb
 */
public class SettingParameters extends Parameters {

	public final Key<Long> variables = makeKey();

	public final Key<Long> literals = makeKey();

	public final Key<Double> threshold = makeKey();

	public final Key<Double> cValue = makeKey();

	public final Key<ClauseFunction> model = makeKey();

	public final Key<Problem> problem = makeKey();

	public final Key<String> printString = makeKey();

	public final Key<Goal> goal = makeKey();

	public final Key<Preferences> preferences = makeKey();

	/**
	 * Creates a new setting parameters object
	 */
	public SettingParameters() {
	}

	/**
	 * Creates a new setting parameters object, initialized with parameters from the given object
	 * @param parameters	The initial parameters
	 */
	public SettingParameters(SettingParameters parameters) {
		super(parameters);
	}
}
