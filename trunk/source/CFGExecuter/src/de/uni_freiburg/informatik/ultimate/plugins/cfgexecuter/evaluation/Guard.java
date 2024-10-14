package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation;

import java.util.ArrayList;

import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.EvalTerm.DataStructure;

public class Guard {
	public static final Guard trivialTrue = new Guard(ConstantEvalTerm.trivialTrue, true);
	public static final Guard trivialFalse = new Guard(ConstantEvalTerm.trivialFalse, true);
	private final EvalTerm term;
	public final boolean trivial;
	
	private Guard(EvalTerm mTerm, boolean mTrivial) {
		assert mTerm.outType.equals(DataStructure.bool);
		term = mTerm;
		trivial = mTrivial;
	}
	
	public static Guard getGuard(ApplicationEvalTerm mTerm) {
		return new Guard(mTerm, false).simplify();
	}

	//public Guard(ArrayList<EvalTerm> mArgs, Class<?> mOutType, String mSymbol) {
	//	term = new ApplicationEvalTerm(mArgs, mOutType, mSymbol);
	//}

	public boolean isFullfilled(EvaluationState state) {
		term.setToArgs(state.getVariables());
		//System.out.println(term);
		return (boolean) term.eval();
	}
	
	private Guard simplify() {
		if(trivial) {
			return this;
		}
		ApplicationEvalTerm mTerm = ApplicationEvalTerm.class.cast(term);
		EvalTerm result;
		//System.out.println("Simplify!");
		//System.out.println(mTerm.toString());
		switch(mTerm.symbol) {
			case "not":
				if(mTerm.args.get(0).getGuard().trivial) {
					return (boolean) mTerm.args.get(0).getGuard().term.eval() ? trivialFalse : trivialTrue;
				}
				result = mTerm;
				// TODO: (not (x < y)) => (y <= x)
				// TODO: (not (x <= y)) => (y < x)
				break;
			case ">=":
				System.out.println(mTerm.toStringThorough());
				result = mTerm.reverseTerm();
				System.out.println(mTerm.toStringThorough());
				break;
			case ">":
				System.out.println(mTerm.toStringThorough());
				result = mTerm.reverseTerm();
				System.out.println(mTerm.toStringThorough());
				break;
			case "and":
				ArrayList<EvalTerm> nonTrivial = new ArrayList<>();
				for(EvalTerm arg : mTerm.args) {
					Guard guard = arg.getGuard();
					if(!guard.trivial) {
						nonTrivial.add(guard.term);
					} else if(!(boolean) guard.term.eval()) { 
						// (and ... false ...) is equivalent to false
						//System.out.println("trivially false");
						return trivialFalse;
					}
				}
				if(nonTrivial.size() == 0) {
					//System.out.println("trivially true");
					return trivialTrue;
				}
				if(nonTrivial.size() == 1) {
					result = nonTrivial.get(0);
				} else {
					result = new ApplicationEvalTerm(nonTrivial, DataStructure.bool, "and");
				}
				break;
			case "or":
				ArrayList<EvalTerm> nonTrivial2 = new ArrayList<>();
				for(EvalTerm arg : mTerm.args) {
					Guard guard = arg.getGuard();
					if(!guard.trivial) {
						nonTrivial2.add(guard.term);
					} else if((boolean) guard.term.eval()) { 
						// (or ... true ...) is equivalent to true
						//System.out.println("trivially true");
						return trivialTrue;
					}
				}
				if(nonTrivial2.size() == 0) {
					//System.out.println("trivially true");
					return trivialTrue;
				}
				if(nonTrivial2.size() == 1) {
					result = nonTrivial2.get(0);
				} else {
					result = new ApplicationEvalTerm(nonTrivial2, DataStructure.bool, "and");
				}
				result = new ApplicationEvalTerm(nonTrivial2, DataStructure.bool, "or");
				break;
			case "<":
			case "<=":
			case "==":
				result = mTerm;
				break;
			case "=":
			case "havoc":
			case "select":
			case "store":
			case "true":
				//System.out.println("trivially true");
				return trivialTrue;
			default:
				System.out.println("Unknown term type " + mTerm.symbol);
			//$FALL-THROUGH$
			case "false":
					return trivialFalse;
		}
		//System.out.println(result.toString());
		return new Guard(result, false);
	}

	/*public final static Map<String, Function<ArrayList<EvalTerm>, Guard>> guards = Map.ofEntries(
		entry("=", (args) -> Guard.trivialTrue),
		entry("select", (args) -> Guard.trivialTrue),
		entry("store", (args) -> Guard.trivialTrue),
		entry("or", (args) -> new Guard(new ApplicationEvalTerm(args, Boolean.class, "or")).simplify()),
		entry("and", (args) -> new Guard(new ApplicationEvalTerm(args, Boolean.class, "and")).simplify()),
		entry("<=", (args) -> new Guard(new ApplicationEvalTerm(args, Boolean.class, "<="))),
		entry(">=", (args) -> new Guard(new ApplicationEvalTerm(args, Boolean.class, ">=")).simplify()),
		entry("<", (args) -> new Guard(new ApplicationEvalTerm(args, Boolean.class, "<"))),
		entry(">", (args) -> new Guard(new ApplicationEvalTerm(args, Boolean.class, ">")).simplify()),
		entry("==", (args) -> new Guard(new ApplicationEvalTerm(args, Boolean.class, "=="))),
		entry("not", (args) -> new Guard(new ApplicationEvalTerm(args, Boolean.class, "not")))
		
	);*/
 }