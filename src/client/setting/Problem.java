package client.setting;

import clausal_discovery.core.LogicBase;
import logic.theory.Theory;
import vector.SafeList;
import vector.Vector;

/**
 * Contains problem specific knowledge.
 *
 * @author Samuel Kolb
 */
public class Problem {

	private final LogicBase logicBase;

	public LogicBase getLogicBase() {
		return logicBase;
	}

	private final SafeList<Theory> backgroundTheories;

	public SafeList<Theory> getBackgroundTheories() {
		return backgroundTheories;
	}

	/**
	 * Creates a problem
	 * @param logicBase				The logic base
	 * @param backgroundTheories	A vector of background theories
	 */
	public Problem(LogicBase logicBase, SafeList<Theory> backgroundTheories) {
		this.logicBase = logicBase;
		this.backgroundTheories = backgroundTheories;
	}
}
