package client.setting;

import clausal_discovery.core.LogicBase;
import logic.theory.Theory;
import vector.Vector;

import java.io.File;
import java.util.Optional;

/**
 * Created by samuelkolb on 28/10/15.
 *
 * @author Samuel Kolb
 */
public class Problem {

	private final LogicBase logicBase;

	public LogicBase getLogicBase() {
		return logicBase;
	}

	private final Vector<Theory> backgroundTheories;

	public Vector<Theory> getBackgroundTheories() {
		return backgroundTheories;
	}

	/**
	 * Creates a problem
	 * @param logicBase				The logic base
	 * @param backgroundTheories	A vector of background theories
	 */
	public Problem(LogicBase logicBase, Vector<Theory> backgroundTheories) {
		this.logicBase = logicBase;
		this.backgroundTheories = backgroundTheories;
	}
}
