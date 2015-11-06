package client.setting;

import clausal_discovery.core.LogicBase;
import clausal_discovery.core.score.ClauseFunction;
import clausal_discovery.validity.ValidityTable;
import logic.expression.formula.Formula;
import logic.theory.Vocabulary;
import parse.ConstraintParser;
import util.Weighted;
import vector.SafeList;

import java.util.List;
import java.util.function.Function;

/**
 * Represents a model, by using the raw representation it is independent of a specific logic base.
 *
 * @author Samuel Kolb
 */
public class Model {

	private final SafeList<Weighted<String>> weightedStrings;

	/**
	 * Creates a new model
	 * @param weightedStrings	The weighted formula strings
	 */
	public Model(List<Weighted<String>> weightedStrings) {
		this.weightedStrings = SafeList.from(weightedStrings);
	}

	/**
	 * Creates a scoring function
	 * @param logicBase	The logic base to use
	 * @return A scoring function over the given logic base
	 */
	public ClauseFunction getFunction(LogicBase logicBase) {
		SafeList<Double> weights = weightedStrings.map(Weighted::getWeight);
		Vocabulary vocabulary = logicBase.getVocabulary();
		Function<Weighted<String>, Formula> f = p -> ConstraintParser.parseClause(vocabulary, p.getObject());
		return new ClauseFunction(weights, ValidityTable.create(logicBase, weightedStrings.map(f)));

	}
}
