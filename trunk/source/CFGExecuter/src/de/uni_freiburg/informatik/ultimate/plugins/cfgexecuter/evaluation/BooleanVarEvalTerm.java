package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation;

public class BooleanVarEvalTerm extends VariableEvalTerm {
	public BooleanVarEvalTerm(String mName, String mTrueName, boolean mValue, boolean mIsAssignTrue, boolean mIsOutVar, boolean mIsHavoced, boolean mIsHavocedOut) { 
		super(mName, mTrueName, DataStructure.bool, mValue, mIsAssignTrue, mIsOutVar, mIsHavoced, mIsHavocedOut);
	}
	
	@Override
	public Object eval() {
		if(!isAssignable) {
			return value;
		}
		value = true;
		return true;
	}
	
	public Object evalNot() {
		if(!isAssignable) {
			return value;
		}
		value = false;
		return true;
	}
	
	@Override
	public boolean containsAssignment() {
		// boolean terms on their own can be assignments.
		// (and bool_var, (< int_x, int_y))
		// can both mean
		// bool_var && (int_x < int_y)
		// and
		// if(int_x < int_y) {bool_var = true;} 
		return isAssignable;
	}
	

	@Override
	public String toString(String indent) {
		String baseText = super.toString(indent);
		if(isAssignable) {
			baseText += " (<= assigns truth value here)";
		}
		return baseText;
	}
}