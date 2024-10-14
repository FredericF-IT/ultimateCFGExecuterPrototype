package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;

import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.CFGExecuterObserver;

import static java.util.Map.entry;

public class ApplicationEvalTerm extends EvalTerm {
	public static ApplicationEvalTerm trivialTrue = new ApplicationEvalTerm(new ArrayList<>(), (args) -> true, Boolean.class, "true");
	public static ApplicationEvalTerm trivialFalse = new ApplicationEvalTerm(new ArrayList<>(), (args) -> false, Boolean.class, "false");
	
	public ArrayList<EvalTerm> args;
	private Function<ArrayList<EvalTerm>, Object> formula;
	private String symbol;
	public static final HashSet<String> compareSymbols = new HashSet<>(Arrays.asList("==", "<=", ">=", "<", ">", "not"));
	
	public static ApplicationEvalTerm reverseAssignmentToVariable(VariableEvalTerm variableToSolveFor, ApplicationEvalTerm equivalence) throws Exception {
		if(!equivalence.symbol.equals("=")) {
			throw new Exception("This is not an assignment!");
		}
		
		CFGExecuterObserver.printLnIf("Target Variable: " + variableToSolveFor);
		CFGExecuterObserver.printLnIf("Current Equality:\n" + equivalence);
		while(!hasTopLevelVarNamed(variableToSolveFor, equivalence)) {
			boolean varIsLeft = equivalence.args.get(0).containsVariable(false, false, variableToSolveFor.localName);
			ApplicationEvalTerm varContainer = (ApplicationEvalTerm) (varIsLeft ?  equivalence.args.get(0) : equivalence.args.get(1));
			EvalTerm otherPart = (!varIsLeft ?  equivalence.args.get(0) : equivalence.args.get(1));
			Function<ArrayList<EvalTerm>, Object> function;
			ArrayList<EvalTerm> arguments = new ArrayList<>();
			ArrayList<EvalTerm> argumentsEquiv = new ArrayList<>();
			EvalTerm innerVarContainer;
			EvalTerm innerOtherPart;
			switch(varContainer.symbol) {
				case "+":
					varIsLeft = varContainer.args.get(0).containsVariable(false, false, variableToSolveFor.localName);
					innerVarContainer = (varIsLeft ?  varContainer.args.get(0) : varContainer.args.get(1));
					innerOtherPart = (!varIsLeft ?  varContainer.args.get(0) : varContainer.args.get(1));
					// varContainer=otherPart  
					// <=> innerVarContainer+innerOtherPart=otherPart 
					// <=> innerVarContainer=otherPart-innerOtherPart
					
					arguments.add(otherPart); arguments.add(innerOtherPart);
					function = (args) -> { return ((int) args.get(0).eval()) - ((int) args.get(1).eval()); };
					EvalTerm minusTerm = new ApplicationEvalTerm(arguments, function, Integer.class, "-");

					argumentsEquiv.add(innerVarContainer);
					argumentsEquiv.add(minusTerm);
					equivalence = new ApplicationEvalTerm(argumentsEquiv, equivalence.formula, Boolean.class, "=");
					break;
				case "-":
					varIsLeft = varContainer.args.get(0).containsVariable(false, false, variableToSolveFor.localName);
					innerVarContainer = (varIsLeft ?  varContainer.args.get(0) : varContainer.args.get(1));
					innerOtherPart = (!varIsLeft ?  varContainer.args.get(0) : varContainer.args.get(1));
					// varContainer=otherPart  
					// <=> innerVarContainer-innerOtherPart=otherPart 
					// <=> innerVarContainer=otherPart+innerOtherPart
					
					arguments.add(otherPart); arguments.add(innerOtherPart);
					function = (args) -> { return ((int) args.get(0).eval()) + ((int) args.get(1).eval()); };
					EvalTerm plusTerm = new ApplicationEvalTerm(arguments, function, Integer.class, "+");

					argumentsEquiv.add(innerVarContainer);
					argumentsEquiv.add(plusTerm);
					equivalence = new ApplicationEvalTerm(argumentsEquiv, equivalence.formula, Boolean.class, "=");
					break;
			}
			CFGExecuterObserver.printLnIf("New Equality:\n" + equivalence);
		}
		return equivalence;
	}
	
	private static boolean hasTopLevelVarNamed(VariableEvalTerm variableToSolveFor, ApplicationEvalTerm equivalence) {
		for(EvalTerm arg : equivalence.args) {
			if(arg.type.equals(formulaType.variable) && ((VariableEvalTerm) arg).localName.equals(variableToSolveFor.localName)) {
				return true;
			}
		}
		return false;
	}
	
	public ApplicationEvalTerm(ArrayList<EvalTerm> mArgs, Function<ArrayList<EvalTerm>, Object> mFormula, Class<?> mOutType, String mSymbol) {  
		super(mOutType);
		type = formulaType.formula;
		
		args = mArgs;
		for(EvalTerm arg : args) {
			outVars.putAll(arg.outVars);
		}
		
		formula = mFormula;
		symbol = mSymbol;
	}
	
	@Override
	public Object eval() {
		Object result = null;
		try {
			result = formula.apply(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assert outType.isInstance(result);
		
		return outType.cast(result);
	}
	
	public boolean isAssignment() {
		return symbol == "=" || symbol == "store";
	}

	@Override
	public boolean containsAssignment() {
		if(isAssignment()) { return true;}
		for(EvalTerm arg : args) {
			if(arg.containsAssignment()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString(String indent) {
		String[] varText = new String[args.size()];
		for(int i = 0; i < args.size(); i++) {
			varText[i] = args.get(i).toString(indent + "  ");
		}
		
		String center = String.join(",\n", varText);
		if(args.size() > 0) {
			center = "\n" + center + "\n" + indent;
		} else {
			center = "";
		}
		return indent + "(" + symbol + center + ")";
	}
	
	@Override
	public void setToArgs(HashMap<String, VariableEvalTerm> inVars) {
		for(EvalTerm arg : args) {
			arg.setToArgs(inVars);
		}
	}

	@Override
	public void updateArgs(HashMap<String, VariableEvalTerm> inVars) {
		for(EvalTerm arg : args) {
			arg.updateArgs(inVars);
		}
	}

	@Override
	public boolean containsVariable(boolean isOutVar, boolean isAssignable, String mName) {
		for(EvalTerm arg : args) {
			if(arg.containsVariable(isOutVar, isAssignable, mName)) {
				return true;
			}
		}
		return true;
	}
	
	@Override
	public String toStringThorough() {
		ArrayList<String> lines = new ArrayList<>();
		for(EvalTerm var : args) {
			lines.add(var.toStringThorough());
		}
		return symbol + "[" + String.join(", ", lines) + "]";
	}

	/**
	 * Pre condition if:
	 * - Is not an assigment
	 * - Does not contain assignment
	 */
	@Override
	public EvalTerm getPreCondition() {
		if(symbol == "and") {
			ArrayList<EvalTerm> newArgs = new ArrayList<>();
			for(EvalTerm arg : args) {
				EvalTerm newArg = arg.getPreCondition();
				
				if(newArg == null) {
					continue;
				}
				
				if(newArg.type.equals(EvalTerm.formulaType.formula)) {
					ApplicationEvalTerm newAppArg = (ApplicationEvalTerm) newArg;
					if(newAppArg.symbol == "and") {
						for(EvalTerm argIn : newAppArg.args) {
							EvalTerm  newArgIn = argIn.getPreCondition();
							if(newArgIn == null) {
								continue;
							}
							newArgs.add(newArgIn);
							continue;
						}
					}
				}
				newArgs.add(newArg);
			}
			if(newArgs.size() == 0) {
				return null;
			} else if (newArgs.size() >= 2) {
				return new ApplicationEvalTerm(newArgs, formula, outType, symbol);
			} else {
				return newArgs.get(0);
			}
		}
		if(containsAssignment()) {
			return null;
		}
		return this;
	}

	public boolean isInverse(EvalTerm obj) {
		if(!obj.getClass().isInstance(this)) {
			return false;
		}
		ApplicationEvalTerm objConverted = (ApplicationEvalTerm) obj;
		if(inverseSymbol(objConverted.symbol)) {
			args.equals(objConverted.args);
		} else if(inverseSymbol(reverseSymbol.getOrDefault(objConverted.symbol, ""))) {
			return args.get(0).equals(objConverted.args.get(1)) && 
			args.get(1).equals(objConverted.args.get(0));
		}

		// em <= lm
		// ==
		// em > lm
		// ==
		// lm < em
		return false;
	}
	
	private boolean inverseSymbol(String mSymbol) {
		switch(mSymbol) {
			case "<":
				return symbol == ">=";
			case ">":
				return symbol == "<=";
			case "<=":
				return symbol == ">";
			case ">=":
				return symbol == "<";
			case "=":
				return symbol == "=";
			case "==":
				return symbol == "==";
		}
		return false;
	}
	
	public static final Map<String, String> reverseSymbol = Map.ofEntries(
			entry("<", ">"),
			entry(">", "<"),
			entry(">=", "<="),
			entry("<=", ">="),
			entry("==", "=="),
			entry("=", "=")
		);
	
	public ApplicationEvalTerm reverseTerm() {
		String newSymbol = reverseSymbol.getOrDefault(symbol, null);
		if(newSymbol == null) {
			return this;
		}
		ArrayList<EvalTerm> newArgs = new ArrayList<>();
		newArgs.add(args.get(1));
		newArgs.add(args.get(0));
		return new ApplicationEvalTerm(newArgs, CFGExecuterObserver.opToFunction.get(newSymbol), outType, newSymbol);
	}
}