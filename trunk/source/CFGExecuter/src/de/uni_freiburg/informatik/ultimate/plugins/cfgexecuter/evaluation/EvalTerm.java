package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation;
//import java.util.ArrayList;
import java.util.HashMap;

public abstract class EvalTerm {
	public final Class<?> outType;
	public final HashMap<String, VariableEvalTerm> outVars;
	
	public EvalTerm(Class<?> mOutType) {
		outType = mOutType;
		outVars = new HashMap<>();
	}
	
	public enum formulaType {
		constant,
		variable,
		formula
	}
	
	public abstract Object eval();
	
	public formulaType type;
	
	public abstract String toString(String indent);
	public abstract String toStringThorough();
	
	@Override
	public String toString() {
		return toString("");
	}
	
	public abstract void setToArgs(HashMap<String, VariableEvalTerm> inVars);
	public abstract void updateArgs(HashMap<String, VariableEvalTerm> inVars);
	
	/**
	Checks whether this term or any sub-terms are assignments.
	*/
	public abstract boolean containsAssignment();
	public abstract EvalTerm getPreCondition();

	public abstract boolean containsVariable(boolean isOutVar, boolean isAssignable, String name);
} 