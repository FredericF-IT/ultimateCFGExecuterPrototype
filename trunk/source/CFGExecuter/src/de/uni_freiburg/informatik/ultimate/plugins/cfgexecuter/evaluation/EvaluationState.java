package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation;

import java.util.HashMap;

import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.CFGExecuterObserver;

public class EvaluationState {
	private final HashMap<String, VariableEvalTerm> variablesCurrent;
	private EvaluationState parent = null;
	private EvaluationState child = null;

	public EvaluationState(HashMap<String, VariableEvalTerm> mVariablesCurrent) {
		variablesCurrent = CFGExecuterObserver.copyMap(mVariablesCurrent);
	}

	public EvaluationState getParent() {
		return parent;
	}

	public void setParent(EvaluationState mParent) {
		parent = mParent;
		parent.setChild(this);
	}

	public EvaluationState getChild() {
		return child;
	}

	private void setChild(EvaluationState mChild) {
		child = mChild;
	}

	public HashMap<String, VariableEvalTerm> getVariables() {
		return CFGExecuterObserver.copyMap(variablesCurrent);
	}

	public EvaluationState getFinalChild() {
		return child == null ? this : child.getFinalChild();
	}
}