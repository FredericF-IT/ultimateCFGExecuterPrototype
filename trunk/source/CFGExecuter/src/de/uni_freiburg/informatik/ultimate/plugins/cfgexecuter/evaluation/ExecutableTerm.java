package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation;

import java.util.ArrayList;
import java.util.HashMap;

import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.CFGExecuterObserver;

public class ExecutableTerm{
	public final ArrayList<ExecutableTerm> children = new ArrayList<>();
	public final HashMap<String, VariableEvalTerm> inVars = new HashMap<>();
	public final HashMap<String, VariableEvalTerm> outVars = new HashMap<>();
	public final String source;
	public final String target;
	public final EvalTerm transitionTerm;
	private EvalTerm preCondition;
	private EvalTerm postCondition = ApplicationEvalTerm.trivialTrue;
	
	public ExecutableTerm(String mSource, 
			String mTarget, 
			EvalTerm mTransitionTerm, 
			HashMap<String, VariableEvalTerm> mInVars, 
			HashMap<String, VariableEvalTerm> mOutVars) {
		for(VariableEvalTerm inVar : mInVars.values()) {
			inVars.put(inVar.trueName, inVar);
		}
		for(VariableEvalTerm outVar : mOutVars.values()) {
			outVars.put(outVar.trueName, outVar);
		}
		source = mSource;
		target = mTarget;
		transitionTerm = mTransitionTerm;
	}
	
	private void evaluatePreCondition() {
		EvalTerm pre = transitionTerm.getPreCondition();
		pre = pre == null ? ApplicationEvalTerm.trivialTrue : pre;
		preCondition = pre;
		
		for(ExecutableTerm child : children) {
			EvalTerm postPart = child.transitionTerm.getPreCondition();
			if(postPart == null) {
				continue;
			}
		}
	}
	
	private static int indexOfInverse(ArrayList<EvalTerm> args, EvalTerm obj) {
		if(obj.type != EvalTerm.formulaType.formula) {
			return -1;
		}
		return args.indexOf(((ApplicationEvalTerm) obj).reverseTerm());
	}
	
	private void setPostCondition() {
		ArrayList<EvalTerm> args = new ArrayList<>();
		for(ExecutableTerm child : children) {
			EvalTerm postPart = child.transitionTerm.getPreCondition();
			if(postPart == null) {
				continue;
			}
			if(args.contains(postPart)) {
				continue;
			}
			int i = indexOfInverse(args, postPart);
			if(i != -1) {
				//args.remove((int) i);
				System.out.println("Removed\n"+args.remove(i).toString()+"\nwhich equals\n" + postPart.toString());
				continue;
			}
			args.add(postPart);
		}

		if (args.size() == 0) {
			return;
		}
		postCondition = new ApplicationEvalTerm(args, CFGExecuterObserver.opToFunction.get("or"), Boolean.class, "or");
	}// ApplicationEvalTerm(ArrayList<EvalTerm> mArgs, Function<ArrayList<EvalTerm>, Object> mFormula, Class<?> mOutType, String mSymbol) {  
	
	public void addChildren(ArrayList<ExecutableTerm> newChildren) {
		children.addAll(newChildren);
		setPostCondition();
		evaluatePreCondition();
		//printMe();
	}
	
	public void printMe() {
		System.out.println("EvalTerm " + toString());
		System.out.println("Precondition\n" + preCondition.toString("  "));
		System.out.println("Postcondition\n" + postCondition.toString("  "));
	}
	
	public void smartHavoc() {
		// TODO
		for(VariableEvalTerm inVar : inVars.values()) {
			if(!inVar.isHavoced) {
				continue;
			}
			if(ArrayVarEvalTerm.class.isInstance(inVar)) {
				continue;
			}

			boolean wasNegative;
			switch(inVar.outType.getSimpleName()) {
				case "Integer":
					wasNegative = ((int) inVar.eval()) < 0;
					inVar.assign(CFGExecuterObserver.havocInt(!wasNegative));
					break;
				case "Boolean":
					inVar.assign(CFGExecuterObserver.havocBool());
					break;
				case "Rational":
					wasNegative = ((Rational) inVar.eval()).isNegative();
					inVar.assign(CFGExecuterObserver.havocRational(!wasNegative));
					break;
				default:
					System.out.println("Unknown variable type " + inVar.outType.getSimpleName());
					break;
			}

			CFGExecuterObserver.printLnIf("havoced " + inVar.localName + " to " + inVar.eval());
		}
	}
	
	public boolean execute(HashMap<String, VariableEvalTerm> allVars) {
		transitionTerm.setToArgs(allVars);
		Object result = transitionTerm.eval();
		transitionTerm.updateArgs(outVars);
		return Boolean.class.isInstance(result) && (boolean) result == true;
	}
	
	@Override
	public String toString() {
		return toStringShort() + " via\n" + transitionTerm.toStringThorough();
	}
	
	public String toStringShort() {
		return "From " + source + " to " + target;
	}
}