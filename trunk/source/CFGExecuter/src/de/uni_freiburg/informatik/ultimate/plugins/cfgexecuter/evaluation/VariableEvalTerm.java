package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation;

import java.util.ArrayList;
import java.util.HashMap;

import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.CFGExecuterObserver;

public class VariableEvalTerm extends EvalTerm {
	public final String localName;
	public final String trueName;
	protected Object value;
	public final boolean isAssignable;
	public final boolean isOutVar;
	public final boolean isHavoced;
	
	public VariableEvalTerm(String mName, String mTrueName, Class<?> mOutType, Object mValue, boolean mIsAssignable, boolean mIsOutVar, boolean mIsHavoced) { 
		super(mOutType);
		assert mOutType.isInstance(mValue);
		type = formulaType.variable;
		trueName = mTrueName;
		localName = mName;
		value = mValue;
		isAssignable = mIsAssignable;
		isOutVar = mIsOutVar;
		isHavoced = mIsHavoced;
		if(isOutVar) {
			outVars.put(trueName, this);
		}
	}

	public VariableEvalTerm copy() {
		return new VariableEvalTerm(localName, trueName, outType, value, isAssignable, isOutVar, isHavoced);
	}

	/**
	 * Returns current value of the variable
	 * @returns Object of the type stored in {@link EvalTerm#outType}
	 */
	@Override
	public Object eval() {
		return value;
	}
	
	public void assign(Object newValue) {
		if(!outType.isInstance(newValue)) {
			System.out.println("Warning: Wrong value type "+ newValue.getClass().getSimpleName() +" assigned to variable "+ localName +". Attempting cast.");
			if(Rational.class.isInstance(newValue) && outType == Integer.class) {
				newValue = CFGExecuterObserver.castRational((Rational) newValue);
			} else {
				newValue = outType.cast(newValue);
			}
		}
		value = newValue;
	}
	
	@Override
	public boolean containsAssignment() {
		return false;
	}
	
	@Override
	public String toString(String indent) {
		return indent + "Variable " + localName + (isAssignable ? " <= Can be assigned" : "");
	}

	@Override
	public void setToArgs(HashMap<String, VariableEvalTerm> inVars) {
		assign(inVars.getOrDefault(trueName, this).value);
		inVars.put(trueName, this);
	}

	@Override
	public void updateArgs(HashMap<String, VariableEvalTerm> inVars) {
		if(!isOutVar) {
			return;
		}
		inVars.put(trueName, this);
	}

	@Override
	public String toStringThorough() {
		return trueName + "="  + localName + "=" + value;
	}


    public static String stringifyVarEvalTerms(HashMap<String, VariableEvalTerm> terms) {
		ArrayList<String> lines = new ArrayList<>();
		for(VariableEvalTerm term : terms.values()) {
			lines.add(term.toStringThorough());
		}
		return "[" + String.join("  ", lines) + "]";
	}

	@Override
	public boolean containsVariable(boolean mIsOutVar, boolean mIsAssignable, String mName) {
		return (!mIsOutVar || isOutVar) && (!mIsAssignable || isAssignable) && (mName == null || mName.equals(localName));
	}


	@Override
	public EvalTerm getPreCondition() {
		if(outType == Boolean.class) {
			return this;
		}
		return null;
	}
}