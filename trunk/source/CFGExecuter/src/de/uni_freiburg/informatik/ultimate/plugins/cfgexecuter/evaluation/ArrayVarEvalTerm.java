package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class ArrayVarEvalTerm extends VariableEvalTerm {
	protected HashMap<Object, Object> data;
	private final DataStructure keyType;
	
	public ArrayVarEvalTerm(String mName, String mTrueName, Object mInitialValue, DataStructure mValueType, DataStructure mKeyType, boolean mIsAssignable, boolean mIsOutVar) {
		super(mName, mTrueName, mValueType, mInitialValue, mIsAssignable, mIsOutVar, false, false);
		data = new HashMap<>();
		keyType = mKeyType;
		assert !mKeyType.equals(DataStructure.array);
	}
	
	@Override
	public void assign(Object newValue) {
		assert ArrayVarEvalTerm.class.isInstance(newValue);
		ArrayVarEvalTerm array = (ArrayVarEvalTerm) newValue;
		assign(array.data);
	}
	
	public void assign(HashMap<Object, Object> newValue) {
		data = newValue;
	}
	
	public Object evalAt(Integer position) {
		return data.getOrDefault(position, value);
	}
	
	public void assignAt(Object newValue, Object position) {
		assert outType.isInstance(newValue);
		assert keyType.isInstance(position);
		data.put(position, newValue);
	}

	@Override
	public boolean containsAssignment() {
		return false;
	}

	@Override
	public String toString(String indent) {
		return indent + outType.toString() + "[] " + localName;
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
		ArrayVarEvalTerm copyArray = new ArrayVarEvalTerm(localName, trueName, value, outType, keyType, isAssignable, isOutVar); 
		for(Entry<Object, Object> entry : data.entrySet()) {
			copyArray.data.put(entry.getKey(), entry.getValue());
		}
		return copyArray;
	}
	
	@Override
	public String toStringThorough() {
		ArrayList<String> values = new ArrayList<>();
		for(Entry<Object, Object> entry : data.entrySet()) {
			values.add(entry.getKey() + "->" + entry.getValue().toString());
		}
		return trueName + "="  + localName + "=[" + String.join(", ", values) + "]";
	}

	@Override
	public HashMap<Object, Object> eval() {
		return data;
	}
}