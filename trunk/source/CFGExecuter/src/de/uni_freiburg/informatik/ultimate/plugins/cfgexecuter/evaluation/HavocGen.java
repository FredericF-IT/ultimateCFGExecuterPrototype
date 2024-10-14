package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation;

import de.uni_freiburg.informatik.ultimate.logic.Rational;

public class HavocGen {
	public static void havocVar(VariableEvalTerm variable) {
		if(!variable.isHavocedIn) {
			return;
		}
		if(ArrayVarEvalTerm.class.isInstance(variable)) {
			return;
		}

		boolean wasNegative;
		switch(variable.outType) {
			case integer:
				wasNegative = ((int) variable.eval()) < 0;
				variable.assign(havocInt(!wasNegative));
				break;
			case bool:
				variable.assign(havocBool());
				break;
			//case "Rational":
			//	wasNegative = ((Rational) variable.eval()).isNegative();
			//	variable.assign(havocRational(!wasNegative));
			//	break;
			default:
				System.out.println("Unknown variable type " + variable.outType.toString());
				break;
		}
		
		//havocRecord.put(variable.localName, variable.eval());
		
		System.out.println("havoced " + variable.localName + " to " + variable.eval());
	}
	
	private final static int havocScale = 6;
	public final static int havocMax = 1 << havocScale; // 2 to the power of havocScale, so havocScale = 8 => havocMax = 256
	
	public static int havocInt(boolean makeNegative) {
		double num1 = Math.random();
		int newIntValue = (int) Math.round(num1 * havocMax);
		if(makeNegative) {
			newIntValue = -1 * newIntValue;
		}
		return newIntValue;
		// value between -havocMax and havocMax
	}
	
	public static Rational havocRational(boolean makeNegative) {
		int newNumerValue = (int) Math.round(Math.random() * havocMax);
		int newDenomValue = (int) Math.round(Math.random() * (havocMax - 1) + 1);

		if(makeNegative) {
			newNumerValue = -1 * newNumerValue;
		}
		
		return Rational.valueOf(newNumerValue, newDenomValue);
		// numerator between -havocMax and havocMax
		// denominator between 1 and havocMax
	}
	
	public static boolean havocBool() {
		return Math.random() > 0.5;
	}
}