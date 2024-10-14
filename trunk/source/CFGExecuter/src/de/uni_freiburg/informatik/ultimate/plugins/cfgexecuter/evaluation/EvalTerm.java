package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation;

import java.util.BitSet;
import java.util.HashMap;


public abstract class EvalTerm {
	public final DataStructure outType;
	public final HashMap<String, VariableEvalTerm> outVars;
	
	public EvalTerm(DataStructure mOutType) {
		outType = mOutType;
		outVars = new HashMap<>();
	}
	
	public enum formulaType {
		constant,
		variable,
		formula
	}
	
	public enum DataStructure {
		integer,
		bitSet,
		bool,
		array;
		
		public Class<?> getType() {
			switch(this) {
				case array:
					return ArrayVarEvalTerm.class;
				case bitSet:
					return BitSet.class;
				case bool:
					return Boolean.class;
				case integer:
					return Integer.class;
				default:
					return null;
			}
		}

		public static DataStructure fromString(String str) {
			switch(str) {
				case "array":
					return array;
				case "bitVector":
					return bitSet;
				case "java.lang.Boolean":
					return bool;
				case "java.lang.Integer":
					return integer;
				default:
					System.out.println("No "+str);
					assert false;
					return null;
			}
		}

		public boolean isInstance(Object obj) {
			return getType().isInstance(obj);
		}
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
	//public abstract EvalTerm getPreCondition();
	public abstract Guard getGuard();

	public abstract boolean containsVariable(boolean requireOutVar, boolean requireAssignable, String name);
} 