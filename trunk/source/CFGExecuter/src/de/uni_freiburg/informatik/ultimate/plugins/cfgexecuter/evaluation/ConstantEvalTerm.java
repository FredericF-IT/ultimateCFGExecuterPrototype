package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation;

import java.util.HashMap;

public class ConstantEvalTerm extends EvalTerm {
	public static final ConstantEvalTerm trivialTrue = new ConstantEvalTerm(true, DataStructure.bool);
	public static final ConstantEvalTerm trivialFalse = new ConstantEvalTerm(false, DataStructure.bool);
	
	private Object value;
	
	
	public ConstantEvalTerm(Object mValue, DataStructure mOutType) { 
		super(mOutType);
		assert mOutType.isInstance(mValue);
		type = formulaType.constant;
		value = mValue;
	}
	
	@Override
	public Object eval() {
		return value;
	}
	
	@Override
	public String toString(String indent) {
		return indent + "Constant " + value;
	}

	@Override
	public boolean containsAssignment() {
		return false;
	}

	@Override
	public void setToArgs(HashMap<String, VariableEvalTerm> inVars) { return; }
	@Override
	public void updateArgs(HashMap<String, VariableEvalTerm> inVars) { return; }

	@Override
	public boolean containsVariable(boolean isOutVar, boolean isAssignable, String mName) { return false; }

	@Override
	public String toStringThorough() {
		return value.toString();
	}

	/*@Override
	public EvalTerm getPreCondition() {
		if(outType == Boolean.class) {
			return this;
		}
		return null;
	}*/

	@Override
	public Guard getGuard() {
		return Guard.trivialTrue;
	}
}