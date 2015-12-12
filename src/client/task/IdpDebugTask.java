package client.task;

import clausal_discovery.core.LogicBase;
import client.setting.SettingParameters;
import idp.IdpExpressionPrinter;
import idp.IdpProgramPrinter;
import log.Log;
import logic.example.Example;
import logic.theory.KnowledgeBase;
import logic.theory.Structure;
import vector.SafeList;

/**
 * Provides insight into the generated IDP code.
 *
 * @author Samuel Kolb
 */
public class IdpDebugTask extends ParametrizedTask {

	/**
	 * Creates and IDP debug task.
	 * @param parameters	The parameters
	 */
	public IdpDebugTask(SettingParameters parameters) {
		super(parameters);
	}

	@Override
	public void run() {
		LogicBase logicBase = getParameters().problem.get().getLogicBase();
		SafeList<Structure> structures = logicBase.getExamples().map(Example::getStructure);
		KnowledgeBase base = new KnowledgeBase(logicBase.getVocabulary(), new SafeList<>(), structures);
		Log.LOG.printLine(new IdpProgramPrinter.Cached().print(base));
	}
}
