package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class ArrayVarEvalTerm extends VariableEvalTerm {
	protected HashMap<Integer, Object> data;
	
	public ArrayVarEvalTerm(String mName, String mTrueName, Object mInitialValue, Class<?> mValueType, boolean mIsAssignable, boolean mIsOutVar) {
		super(mName, mTrueName, mValueType, mInitialValue, mIsAssignable, mIsOutVar, false);
		data = new HashMap<>();
	}
	
	@Override
	public void assign(Object newValue) {
//			System.out.println(newValue.toString());
		assert ArrayVarEvalTerm.class.isInstance(newValue);
		ArrayVarEvalTerm array = (ArrayVarEvalTerm) newValue;
		data = array.data;
	}
	
	public void assign(HashMap<Integer, Object> newValue) {
		data = newValue;
	}
	
	public Object evalAt(Integer position) {
		return data.getOrDefault(position, value);
	}
	
	public void assignAt(Object newValue, Integer position) {
		assert outType.isInstance(newValue);
		data.put(position, newValue);
	}

	@Override
	public boolean containsAssignment() {
		return false;
	}

	@Override
	public String toString(String indent) {
		return indent + outType.getSimpleName() + "[] " + localName;
	}


	@Override
	public void setToArgs(HashMap<String, VariableEvalTerm> inVars) {
		//System.out.println(localName);
		//printEvalTerms(inVars);
		assign(((ArrayVarEvalTerm) inVars.getOrDefault(trueName, this)).copy().data);
	}

	@Override
	public void updateArgs(HashMap<String, VariableEvalTerm> inVars) {
		if(!isOutVar) {
			return;
		}
		inVars.put(trueName, this);
	}
	
	@Override
	public ArrayVarEvalTerm copy() {
		ArrayVarEvalTerm copyArray = new ArrayVarEvalTerm(localName, trueName, value, outType, isAssignable, isOutVar); 
		for(Entry<Integer, Object> entry : data.entrySet()) {
			copyArray.data.put(entry.getKey(), entry.getValue());
		}
		//copyArray.data = copyMap(data.);
		return copyArray;
	}
	
	@Override
	public String toStringThorough() {
		ArrayList<String> values = new ArrayList<>();
		for(Entry<Integer, Object> entry : data.entrySet()) {
			values.add(entry.getKey() + "->" + entry.getValue().toString());
		}
		return trueName + "="  + localName + "=[" + String.join(", ", values) + "]";
	}

	@Override
	public HashMap<Integer, Object> eval() {
		return data;
	}
}