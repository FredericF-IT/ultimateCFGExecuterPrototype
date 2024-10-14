package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.ConstantTerm;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.CFGExecuterObserver;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.EvalTerm.DataStructure;

public class TermParser {

	public static EvalTerm createEvalTerm(
			Term myTerm, 
			HashMap<String, VariableEvalTerm> inVariables) throws Exception {
		EvalTerm result = parseTerm(myTerm, inVariables, true);
		
		if(result.type == EvalTerm.formulaType.variable) {
			VariableEvalTerm varResult = (VariableEvalTerm) result;
			if(varResult.isAssignable) {
				ArrayList<EvalTerm> arguments = new ArrayList<>();
				arguments.add(result);
				arguments.add(ConstantEvalTerm.trivialTrue);
				result = new ApplicationEvalTerm(arguments, DataStructure.bool, "=");
			} else {
				ArrayList<EvalTerm> arguments = new ArrayList<>();
				arguments.add(result);
				arguments.add(ConstantEvalTerm.trivialTrue);
				result = new ApplicationEvalTerm(arguments, DataStructure.bool, "==");
			}
		}
		
		if(result.type == EvalTerm.formulaType.constant) {
			ArrayList<EvalTerm> arguments = new ArrayList<>();
			arguments.add(result);
			arguments.add(ConstantEvalTerm.trivialTrue);
			result = new ApplicationEvalTerm(arguments, DataStructure.bool, "==");
		}
		
		ArrayList<VariableEvalTerm> havocedInVars = new ArrayList<>();
		ArrayList<VariableEvalTerm> havocedOutVars = new ArrayList<>();
		for(VariableEvalTerm variable : inVariables.values()) {
			if(variable.isHavocedIn) {
				CFGExecuterObserver.printLnIf(variable.localName + " is havoced in");
				havocedInVars.add(variable);
			} else if(variable.isHavocedOut) {
				CFGExecuterObserver.printLnIf(variable.localName + " is havoced out");
				havocedOutVars.add(variable);
				
			}
		}
		
		if(havocedInVars.size() > 0 || havocedOutVars.size() > 0) {
			ArrayList<EvalTerm> arguments = new ArrayList<>();
			
			for(VariableEvalTerm havocedVar : havocedInVars) {
				ArrayList<EvalTerm> subArgs = new ArrayList<>();
				subArgs.add(havocedVar);
				arguments.add(new ApplicationEvalTerm(subArgs, DataStructure.bool, "havoc"));
			}
			
			arguments.add(result);
			
			for(VariableEvalTerm havocedVar : havocedOutVars) {
				ArrayList<EvalTerm> subArgs = new ArrayList<>();
				subArgs.add(havocedVar);
				arguments.add(new ApplicationEvalTerm(subArgs, DataStructure.bool, "havoc"));
			}
			
			result = new ApplicationEvalTerm(arguments, DataStructure.bool, "and");
		}
		
		return result;
	}
	
	private static EvalTerm parseTerm(
			Term myTerm, 
			HashMap<String, VariableEvalTerm> inVariables, 
			boolean isRoot
			) throws Exception {
		switch(myTerm.getClass().getName()) {
		case "de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm":
			ApplicationTerm termTrans = (ApplicationTerm) myTerm;
			ArrayList<EvalTerm> arguments = new ArrayList<>();
			String symbol = termTrans.getFunction().getName();
			EvalTerm expr1 = null;
			EvalTerm expr2 = null;
			//Function<ArrayList<EvalTerm>, Object> function = opToFunction.getOrDefault(symbol, null);
			Term[] parameter = termTrans.getParameters();
			switch (symbol) {
				case "=":
					expr1 = parseTerm(parameter[0], inVariables, false);
					expr2 = parseTerm(parameter[1], inVariables, false);
					
					boolean p1IsVar = expr1.type == EvalTerm.formulaType.variable;
					VariableEvalTerm var1 = p1IsVar ? (VariableEvalTerm) expr1 : null;
					boolean isAssignable1 = p1IsVar && var1.isAssignable;
					
					boolean p2IsVar = expr2.type == EvalTerm.formulaType.variable;
					VariableEvalTerm var2 = p2IsVar ? (VariableEvalTerm) expr2 : null;
					boolean isAssignable2 = p2IsVar && var2.isAssignable;
					
					boolean containsVariable1 = expr1.containsVariable(true, true, null);
					boolean containsVariable2 = expr2.containsVariable(true, true, null);
					
					VariableEvalTerm variable;
					EvalTerm newValue;
					
					// only one of the terms is a variable that can be assigned
					if(isAssignable1 ^ isAssignable2) {
						variable = isAssignable1 ? var1 : var2;
						newValue = !isAssignable1 ? expr1 : expr2;
					}
					// both terms are variables that are assignable
					else if (isAssignable1 && isAssignable2) {
						// assume order is correct
						CFGExecuterObserver.printLnIf("Warning: Variable assigned to variable, order unknown in term " + termTrans.toString());
						variable = var1;
						newValue = expr2;
					} 
					// one or both terms contain a variable that is assignable and affects the next state
					else if(containsVariable1 || containsVariable2) {
						CFGExecuterObserver.printLnIf("Warning: Target of assignment not in top level of equality.\nAttempting reshape...");
						
						arguments.add(expr1); arguments.add(expr2);
						ApplicationEvalTerm variableContainer = new ApplicationEvalTerm(arguments, DataStructure.bool, symbol);
						ApplicationEvalTerm targetVariable = variableContainer;
						VariableEvalTerm result = null;
						
						while(result == null/**/) {
							for(EvalTerm arg : targetVariable.args) {
								if(arg.type == EvalTerm.formulaType.variable) {
									VariableEvalTerm possibleTarget = (VariableEvalTerm) arg;
									// Found variable to solve for
									if(possibleTarget.isAssignable) {
										result = possibleTarget;
									}
									continue;
								} 
								// Explore subterm that contains an assignable outvar
								if(arg.containsVariable(true, true, null)) {
									targetVariable = (ApplicationEvalTerm) arg;
									break;
								}
								assert false;
							}
						}
						
						return ApplicationEvalTerm.reverseAssignmentToVariable(result, variableContainer);
					}
					else {
						CFGExecuterObserver.printLnIf("WARNING!\nAssignment has no variable. Evaluate equality instead.\n--(" + symbol + "\n" + expr1.toString("  --"));
						CFGExecuterObserver.printLnIf(expr2.toString("  --")+"\n--)");
						//throw new Exception("Neither subterm is assignable in assignment term " + termTrans.toString());
						return compareTerm(parameter, inVariables, "==");
					}

					arguments.add(variable); arguments.add(newValue);
					
					return new ApplicationEvalTerm(arguments, DataStructure.bool, symbol);
				case "+":
					expr1 = parseTerm(parameter[0], inVariables, false);
					expr2 = parseTerm(parameter[1], inVariables, false);
					
					arguments.add(expr1); arguments.add(expr2);

					return new ApplicationEvalTerm(arguments, DataStructure.integer, symbol);
				case "-":
					expr1 = parseTerm(parameter[0], inVariables, false);
					expr2 = parseTerm(parameter[1], inVariables, false);
					
					arguments.add(expr1); arguments.add(expr2);

					return new ApplicationEvalTerm(arguments, DataStructure.integer, symbol);
				case "*":
					expr1 = parseTerm(parameter[0], inVariables, false);
					expr2 = parseTerm(parameter[1], inVariables, false);
					
					arguments.add(expr1); arguments.add(expr2);
					
					return new ApplicationEvalTerm(arguments, DataStructure.integer, symbol);
				case "==":
				case "<=":
				case ">=":
				case "<":
				case ">":
					return compareTerm(parameter, inVariables, symbol);
				case "not":
					expr1 = parseTerm(parameter[0], inVariables, false);
					
					assert expr1.outType.equals(DataStructure.bool);
					
					arguments.add(expr1);
					
					if(expr1.type == EvalTerm.formulaType.variable) {
						ArrayList<EvalTerm> args2 = new ArrayList<>();
						args2.add(expr1);
						args2.add(ConstantEvalTerm.trivialFalse);
						VariableEvalTerm arg2 = (VariableEvalTerm) expr1;
					    if(isRoot && arg2.isAssignable) {
							return new ApplicationEvalTerm(args2, DataStructure.bool, "=");
						}
						return new ApplicationEvalTerm(args2, DataStructure.bool, "==");
					}
					
					return new ApplicationEvalTerm(arguments, DataStructure.bool, symbol);
				case "and":
					for(int i = 0; i < parameter.length; i++) {
						EvalTerm arg = parseTerm(parameter[i], inVariables, false);
						assert arg.outType.equals(DataStructure.bool);
						
						if(arg.type == EvalTerm.formulaType.variable) {
							ArrayList<EvalTerm> args2 = new ArrayList<>();
							args2.add(arg);
							args2.add(ConstantEvalTerm.trivialTrue);
							VariableEvalTerm arg2 = (VariableEvalTerm) arg;
							if(isRoot && arg2.isAssignable) {
								arg = new ApplicationEvalTerm(args2, DataStructure.bool, "=");
							} else {
								arg = new ApplicationEvalTerm(args2, DataStructure.bool, "==");
							}
						}
						
						arguments.add(arg);
					}
					
					Collections.sort(arguments, (a, b) -> compareExpression(a, b));
					
					return new ApplicationEvalTerm(arguments, DataStructure.bool, symbol);
				case "or":
					for(int i = 0; i < parameter.length; i++) {
						EvalTerm arg = parseTerm(parameter[i], inVariables, false);
						assert arg.outType.equals(DataStructure.bool);
						
						if(arg.type == EvalTerm.formulaType.variable) {
							ArrayList<EvalTerm> args2 = new ArrayList<>();
							args2.add(arg);
							args2.add(ConstantEvalTerm.trivialTrue);
							VariableEvalTerm arg2 = (VariableEvalTerm) arg;
							if(isRoot && arg2.isAssignable) {
								arg = new ApplicationEvalTerm(args2, DataStructure.bool, "=");
							} else {
								arg = new ApplicationEvalTerm(args2, DataStructure.bool, "==");
							}
						}
						
						arguments.add(arg);
					}

					Collections.sort(arguments, (a, b) -> compareExpression(a, b));
					
					return new ApplicationEvalTerm(arguments, DataStructure.bool, symbol);
				case "true":
					return new ApplicationEvalTerm(arguments, DataStructure.bool, "true");
//					public static final ConstantEvalTerm trivialFalse = new ConstantEvalTerm(false, DataStructure.bool);
				case "false":
					return new ApplicationEvalTerm(arguments, DataStructure.bool, "false");
				case "store":
					ArrayVarEvalTerm array1 = (ArrayVarEvalTerm) parseTerm(parameter[0], inVariables, false);
					
					expr1 = parseTerm(parameter[1], inVariables, false);
					// The pointer should be an int
					assert DataStructure.integer.equals(expr1.outType);
					
					expr2 = parseTerm(parameter[2], inVariables, false);
					// The new value should be the same type as the arrays entries
					assert expr1.outType.equals(expr2.outType);
					
					arguments.add(array1);
					arguments.add(expr1);
					arguments.add(expr2);
					
					return new ApplicationEvalTerm(arguments, DataStructure.array, symbol);
				case "select":
					expr1 = parseTerm(parameter[0], inVariables, false);
					
					expr2 = parseTerm(parameter[1], inVariables, false);
					
					arguments.add(expr1);
					arguments.add(expr2);

					return new ApplicationEvalTerm(arguments, expr1.outType, symbol);
				default:
					System.out.println("Warning: Unknown fomula!\n(" + termTrans.getFunction().getName());
					for (int i = 0; i < parameter.length; i++) {
						System.out.println("  " + parameter[i].toString());
					}
					System.out.println(")");
					break;
			}
			break;
			
		case "de.uni_freiburg.informatik.ultimate.logic.ConstantTerm":
			ConstantTerm termConst = (ConstantTerm) myTerm;
			Object value = termConst.getValue();
			if(Rational.class.isInstance(value)) {
				value = castRational((Rational) value);
			}
			DataStructure outType = DataStructure.fromString(value.getClass().getName());
			return new ConstantEvalTerm(value, outType); 
			
		case "de.uni_freiburg.informatik.ultimate.logic.TermVariable":
			TermVariable termVar = (TermVariable) myTerm;
			String varName = termVar.getName();
			return inVariables.get(varName);
			
		default:
			System.out.println("Unknown " + myTerm.getClass().getName());
		}
		return null;
	}
	
	private static int compareTo(EvalTerm a, EvalTerm b) {
		switch(a.outType) {
			case bitSet:
				// TODO
				//return ((BitSet) a.eval()).compareTo((BitSet)(b.eval()));
				break;
			case bool:
				return ((Boolean) a.eval()).compareTo((Boolean)(b.eval()));
			case integer:
				return ((Integer) a.eval()).compareTo((Integer)(b.eval()));
			default:
				break;
		}
		assert false;
		return 0;
	}
	
	public static final Map<String, Function<ArrayList<EvalTerm>, Object>> opToFunction = Map.ofEntries(
		entry("=", (args) -> { 
			VariableEvalTerm var = (VariableEvalTerm) args.get(0); 
			Object value = args.get(1).eval();
			var.assign(value); 
			return true; 
		}),

		entry("==", (args) -> {
			return args.get(0).eval().equals(args.get(1).eval());
		}),

		entry("<=", (args) -> {
			return compareTo(args.get(0), args.get(1)) <= 0;
		}),

		entry(">=", (args) -> {
			return compareTo(args.get(0), args.get(1)) >= 0;
		}),

		entry("<", (args) -> {
			return compareTo(args.get(0), args.get(1)) < 0;
		}),

		entry(">", (args) -> {
			return compareTo(args.get(0), args.get(1)) > 0;
		}),

		entry("not", (args) -> {
			return !((Boolean) args.get(0).eval());
		}),

		entry("*", (args) -> { 
			return ((int) args.get(0).eval()) * ((int) args.get(1).eval()); 
		}),

		entry("+", (args) -> { 
			return ((int) args.get(0).eval()) + ((int) args.get(1).eval()); 
		}),

		entry("-", (args) -> { 
			return ((int) args.get(0).eval()) - ((int) args.get(1).eval()); 
		}),
		
		entry("and", (args) -> {
			for(EvalTerm arg : args) {
				if(!(boolean) arg.eval()) {
					return false;
				}
			}
			return true;
		}),
		
		entry("or", (args) -> {
			for(EvalTerm arg : args) {
				if((boolean) arg.eval()) {
					return true;
				}
			}
			return false;
		}),
		
		entry("store", (args) -> {
			ArrayVarEvalTerm array = (ArrayVarEvalTerm) args.get(0);
			int position = (int) args.get(1).eval();
			Object val2 = args.get(2).eval();
			array.assignAt(val2, position);
			
			return array;
		}),
		
		entry("select", (args) -> {
			int position = (int) args.get(1).eval();
			
			return ((ArrayVarEvalTerm) args.get(0)).evalAt(position);
		}),
		
		entry("havoc", (args) -> {
			VariableEvalTerm havocedVar = (VariableEvalTerm) args.get(0);
			HavocGen.havocVar(havocedVar);
	
			return true;
		}),
		
		entry("true", (args) -> { return true; }),
		
		entry("false", (args) -> { return false; })
	);
	
	private static int compareExpression(EvalTerm arg0, EvalTerm arg1) {
		EvalTerm expr1 = arg0;
		boolean hasAssignment1 = false;
		EvalTerm expr2 = arg1;
		boolean hasAssignment2 = false;
		
		if(ApplicationEvalTerm.class.isInstance(expr1)) {
			hasAssignment1 = ((ApplicationEvalTerm) expr1).containsAssignment();
		}
		if(ApplicationEvalTerm.class.isInstance(expr2)) {
			hasAssignment2 = ((ApplicationEvalTerm) expr2).containsAssignment();
		}
		
		// we know term 1 changes the state and term 2 is only evaluated.
		// Should term 2 be false, term 1 should not be executed, so we reverse the order.
		if(hasAssignment1 && !hasAssignment2) {
			return 1;
		}
		if(!hasAssignment1 && hasAssignment2) {
			return -1;
		}
		return 0;
	}
	
	private static EvalTerm compareTerm(
			Term[] parameter,
			HashMap<String, VariableEvalTerm> inVariables,
			String symbol) throws Exception {
		ArrayList<EvalTerm> arguments = new ArrayList<>();
		
		EvalTerm expr1 = parseTerm(parameter[0], inVariables, false);
		EvalTerm expr2 = parseTerm(parameter[1], inVariables, false);
		
		arguments.add(expr1); arguments.add(expr2);
		
		//Function<ArrayList<EvalTerm>, Object> function = opToFunction.get(symbol);
		
		return new ApplicationEvalTerm(arguments, DataStructure.bool, symbol);
	}
	
	public static Integer castRational(Rational numb) {
		int result = (numb.numerator().divide(numb.denominator())).intValueExact();
		CFGExecuterObserver.printLnIf("Cast Rational "+numb.toString()+" to int " + result);
		return result;
	}
}