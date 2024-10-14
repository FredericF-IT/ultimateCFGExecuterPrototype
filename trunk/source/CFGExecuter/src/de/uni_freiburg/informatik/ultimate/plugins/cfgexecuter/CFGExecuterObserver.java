package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter;

import java.io.IOException;
import static java.util.Map.entry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;

import de.uni_freiburg.informatik.ultimate.core.model.models.IElement;
import de.uni_freiburg.informatik.ultimate.core.model.models.ModelType;
import de.uni_freiburg.informatik.ultimate.core.model.observers.IUnmanagedObserver;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.BoogieIcfgContainer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.BoogieIcfgLocation;

import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IcfgEdge;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.transitions.UnmodifiableTransFormula;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.ConstantTerm;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;

import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.EvalTerm;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.ExecutableTerm;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.ArrayVarEvalTerm;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.ConstantEvalTerm;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.VariableEvalTerm;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.ApplicationEvalTerm;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.EvaluationState;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.examples.Tests;

public class CFGExecuterObserver implements IUnmanagedObserver {
	private BoogieIcfgContainer rootICFG;

	public CFGExecuterObserver(final IUltimateServiceProvider services) {
		printLnIf("here");
	}

	@Override
	public boolean process(final IElement root) throws IOException {
		System.out.println("\n\n\n");
		assert BoogieIcfgContainer.class.isInstance(root);
		rootICFG = (BoogieIcfgContainer) root;
		
		Map<String, BoogieIcfgLocation> elements = rootICFG.getProcedureEntryNodes();

		String[] keys = elements.keySet().toArray(new String[0]);
		double ExecutionsPerEntryNode = 3.0;

		for(int i = 0; i < ExecutionsPerEntryNode * keys.length; i++) {
			String key = keys[(int) Math.floor(i / ExecutionsPerEntryNode)];

			visited.clear();
			
			print = i % ExecutionsPerEntryNode == 0;
			//print = false;
			printLnIf("------ CREATE EXECUTABLE TERMS ------");
			ArrayList<ExecutableTerm> execs = exploreEdges(elements.get(key));
			print = false;

			EvaluationState evStateRoot = new EvaluationState(execs.get(0).transitionTerm.outVars);
			EvaluationState evState = evStateRoot;
			
			printLnIf("Initial Vars: " + VariableEvalTerm.stringifyVarEvalTerms(evStateRoot.getVariables()));
			boolean state = false;
			String lastState = execs.get(0).source;
			ArrayList<String> pathTaken = new ArrayList<>();
			pathTaken.add(lastState + " " + VariableEvalTerm.stringifyVarEvalTerms(evState.getVariables()));
			havocRecord = new HashMap<>();

			print = true;
			printLnIf("------ EXECUTE EXECUTABLE TERMS ------");
			print = false;
			for(int j = 0; j < execs.size(); j++) {
				state = false;
				ExecutableTerm exec = execs.get(j);

				@SuppressWarnings("unchecked")
				HashMap<String, Object> havocRecordCopy = (HashMap<String, Object>) havocRecord.clone();

				boolean success = exec.execute(evState.getVariables());
				if(success) {
					havocRecord.putAll(havocRecordCopy);

					lastState = exec.target;
					String usedVars = VariableEvalTerm.stringifyVarEvalTerms(evState.getVariables());
					printLnIf("Trying " + exec.toString() + " with Vars:\n" + 
						usedVars + 
						"\nPath successfull");

					state = true;
					HashMap<String, VariableEvalTerm> updateVars = evState.getVariables();
					exec.transitionTerm.updateArgs(updateVars);
					
					EvaluationState nextEvState = new EvaluationState(updateVars);
					nextEvState.setParent(evState);
					
					pathTaken.add(" -> " + VariableEvalTerm.stringifyVarEvalTerms(updateVars) + "\n" + exec.target);

					if(exec.children.isEmpty()) { 
						print = true;
						printLnIf("Sucessfully reached leaf node!\nPath: " + String.join("\n", pathTaken));
						lastState = exec.target;
						break;
					}
					j = -1;
					
					evState = nextEvState;
					execs = exec.children;
				} else {
					printLnIf("Trying " + exec.toString() + " with Vars:\n" + 
						VariableEvalTerm.stringifyVarEvalTerms(evState.getVariables()) + 
						"\nPath cannot be taken");
				}
			}
			if(!state) {
				print = true;
				printIf("No path can be taken at this point!\nPath:\n" +
					String.join("\n", pathTaken) +
					"\nCurrent Vars:" +
					VariableEvalTerm.stringifyVarEvalTerms(evState.getVariables()));
			}
			printLnIf("------ TEST AGAINST IMPLEMENTATION ------");
			print = true;
			assert Tests.testForFile(rootICFG.getFilename(), evStateRoot, lastState);
			System.out.println("\n");
		}
		return false;
	}
	
	private HashMap<IcfgEdge, ExecutableTerm> visited = new HashMap<>();
	private HashMap<IcfgLocation, ArrayList<ExecutableTerm>> allEdges = new HashMap<>();
	
	private ArrayList<ExecutableTerm> exploreEdges(BoogieIcfgLocation location) {
		ArrayList<ExecutableTerm> executeEdges = new ArrayList<>();
		IcfgLocation source = location.getLabel();

		for(IcfgEdge edge : location.getOutgoingEdges()) {
			IcfgLocation target = edge.getTarget();
			
			ExecutableTerm targetExec = visited.getOrDefault(edge, null);
			if(targetExec != null) { 
				executeEdges.add(targetExec);
				continue;
			}

			printLnIf("------ CREATE EXECUTABLE TERMS ------");
			printLnIf("From " + source + " to " + target);

			UnmodifiableTransFormula transFormula = edge.getTransformula();
			
			targetExec = transFormulaToEvaluatable(transFormula, source, target);
			visited.put(edge, targetExec);

			ArrayList<ExecutableTerm> childrenExecs = exploreEdges((BoogieIcfgLocation) target);
			targetExec.addChildren(childrenExecs);
			executeEdges.add(targetExec);
		}
		allEdges.put(source, executeEdges);
		return executeEdges;
	}
	
	private static boolean print = false;
	public static void printLnIf(Object text) { if(print) System.out.println(text); }
	public static void printIf(Object text) { if(print) System.out.print(text); }
	
	private static VariableEvalTerm baseValueAny(String trueName, boolean isAssignable, boolean isHavoced, boolean isOutVar, TermVariable inTerm, UnmodifiableTransFormula transFormula) {
		Sort sort = inTerm.getSort();
		VariableEvalTerm newVer = null;
		String name = inTerm.getName();
		switch(sort.getName()) {
			case "Bool":
				printLnIf("Var " + name + " is bool");
				newVer = new VariableEvalTerm(name, trueName, Boolean.class, havocBool(), isAssignable, isOutVar, isHavoced);
				break;
			case "Rational":
				printLnIf("Var " + name + " is rational");
				newVer = new VariableEvalTerm(name, trueName, Rational.class, havocRational(havocBool()), isAssignable, isOutVar, isHavoced);
				break;
			case "Int":
				printLnIf("Var " + name + " is int");
				newVer = new VariableEvalTerm(name, trueName, Integer.class, havocInt(havocBool()), isAssignable, isOutVar, isHavoced);
				break;
			case "Array":
				Sort arraySort = inTerm.getDeclaredSort();
				Sort indexType = arraySort.getArguments()[0];
				Sort valueType = arraySort.getArguments()[1];
				printLnIf("Var " + name + " is Array["+indexType.getName()+"]"+valueType.getName());
				switch(valueType.getName()) {
					case "Bool":
						newVer = new ArrayVarEvalTerm(name, trueName, havocBool(), Boolean.class, isAssignable, isOutVar);
						break;
					case "Int":
						newVer = new ArrayVarEvalTerm(name, trueName, havocInt(havocBool()), Integer.class, isAssignable, isOutVar);
						break;
					case "Rational":
						newVer = new ArrayVarEvalTerm(name, trueName, havocRational(havocBool()), Rational.class, isAssignable, isOutVar);
						break;
				}
				break;
		}
		
		if(newVer == null) {
			printLnIf("Unknown: "+ sort.getName());
		}
		
		return newVer;
	}
	
	private static Set<TermVariable> computeAssignedVars(final Map<IProgramVar, TermVariable> inVars,
			final Map<IProgramVar, TermVariable> outVars) {
		final HashSet<TermVariable> assignedVars = new HashSet<>();
		for (final Entry<IProgramVar, TermVariable> entry : outVars.entrySet()) {
			assert entry.getValue() != null;
			if (entry.getValue() != inVars.get(entry.getKey())) {
				assignedVars.add(entry.getValue());
			}
		}
		for (final Entry<IProgramVar, TermVariable> pv : inVars.entrySet()) {
			if (!outVars.containsKey(pv.getKey())) {
				assignedVars.add(pv.getValue());
			}
		}
		return assignedVars;
	}
	
	private static HashMap<UnmodifiableTransFormula, Set<TermVariable>> assignedVarsCache = new HashMap<>(); 
	
	private static VariableEvalTerm baseValue(IProgramVar inVar, TermVariable inTerm, UnmodifiableTransFormula transFormula) {
		String trueName = inVar.getGloballyUniqueId();
		Set<TermVariable> assignedVariables = assignedVarsCache.computeIfAbsent(transFormula, (key) -> computeAssignedVars(key.getInVars(), key.getOutVars()));
		boolean isAssignable = /*transFormula.getAssignedVars()*/assignedVariables.contains(inTerm);
		boolean isHavoced = transFormula.isHavocedIn(inVar) && transFormula.isHavocedOut(inVar);
		boolean isOutVar = transFormula.getOutVars().containsValue(inTerm);

		return baseValueAny(trueName, isAssignable, isHavoced, isOutVar, inTerm, transFormula);
	}
	
	private static VariableEvalTerm baseValueAux(TermVariable inTerm, UnmodifiableTransFormula transFormula) {
		return baseValueAny(inTerm.getName(), true, false, false, inTerm, transFormula);
	}
	
	public static HashMap<String, VariableEvalTerm> copyMap(HashMap<String, VariableEvalTerm> inVars) {
		HashMap<String, VariableEvalTerm> inVarsCopy = new HashMap<>();
		for(Entry<String, VariableEvalTerm> inVar : inVars.entrySet()) {
			inVarsCopy.put(inVar.getKey(), inVar.getValue().copy());
		}
		return inVarsCopy;
	}
	
	public ExecutableTerm transFormulaToEvaluatable(UnmodifiableTransFormula transFormula, IcfgLocation source, IcfgLocation target) {
		printLnIf(transFormula);
		
		HashMap<String, VariableEvalTerm> allVariables = new HashMap<>();
		HashMap<String, VariableEvalTerm> outVariables = new HashMap<>();
		HashMap<String, VariableEvalTerm> inVariables = new HashMap<>();
		for (Entry<IProgramVar, TermVariable> inVar : transFormula.getInVars().entrySet()) {
	    	VariableEvalTerm outVar = baseValue(inVar.getKey(), inVar.getValue(), transFormula);
			allVariables.put(inVar.getValue().getName(), outVar);
			inVariables.put(inVar.getValue().getName(), outVar);
		}
	    for (Entry<IProgramVar, TermVariable> inVar : transFormula.getOutVars().entrySet()) {
	    	VariableEvalTerm outVar = baseValue(inVar.getKey(), inVar.getValue(), transFormula);
			allVariables.put(inVar.getValue().getName(), outVar);
			outVariables.put(inVar.getValue().getName(), outVar);
		}
		for (TermVariable inVar : transFormula.getAuxVars()) {
			allVariables.put(inVar.getName(), baseValueAux(inVar, transFormula));
		}
		
		printIf("Outvars:");
		for(String wanted : outVariables.keySet()) {
			VariableEvalTerm wantedVar = allVariables.get(wanted);
			printIf(" " + wantedVar.trueName + "=" + wantedVar.localName);
		}
		printLnIf("");
		
		EvalTerm transitionTerm;
		try {
			transitionTerm = evaluater(transFormula.getFormula(), allVariables);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		printLnIf("Executable Term:\n" + transitionTerm.toString("") + "\n");
		return new ExecutableTerm(source.toString(), target.toString(), transitionTerm, inVariables, outVariables);
	}
	
	public static HashMap<String, Object> havocRecord;
	
	private static void havocVar(VariableEvalTerm variable) {
		if(!variable.isHavoced) {
			return;
		}
		if(ArrayVarEvalTerm.class.isInstance(variable)) {
			return;
		}

		boolean wasNegative;
		switch(variable.outType.getSimpleName()) {
			case "Integer":
				wasNegative = ((int) variable.eval()) < 0;
				variable.assign(havocInt(!wasNegative));
				break;
			case "Boolean":
				variable.assign(havocBool());
				break;
			case "Rational":
				wasNegative = ((Rational) variable.eval()).isNegative();
				variable.assign(havocRational(!wasNegative));
				break;
			default:
				System.out.println("Unknown variable type " + variable.outType.getSimpleName());
				break;
		}
		
		havocRecord.put(variable.localName, variable.eval());
		
		boolean lastPrint = print;
		print = true;
		printLnIf("havoced " + variable.localName + " to " + variable.eval());
		print = lastPrint;
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

	public static int castRational(Rational numb) {
		int result = (numb.numerator().divide(numb.denominator())).intValueExact();
		printLnIf("Cast Rational "+numb.toString()+" to int " + result);
		return result;
	}
	

	 
	public EvalTerm evaluater(
			Term myTerm, 
			HashMap<String, VariableEvalTerm> inVariables) throws Exception {
		
		EvalTerm result = parseTerm(myTerm, inVariables, true);
		if(result.type == EvalTerm.formulaType.variable) {
			VariableEvalTerm varResult = (VariableEvalTerm) result;
			if(varResult.isAssignable) {
				ArrayList<EvalTerm> arguments = new ArrayList<>();
				arguments.add(result);
				arguments.add(ApplicationEvalTerm.trivialTrue);
				result = new ApplicationEvalTerm(arguments, opToFunction.get("="), Boolean.class, "=");
			} else {
				ArrayList<EvalTerm> arguments = new ArrayList<>();
				arguments.add(result);
				arguments.add(ApplicationEvalTerm.trivialTrue);
				result = new ApplicationEvalTerm(arguments, opToFunction.get("=="), Boolean.class, "==");
			}
		}
		if(result.type == EvalTerm.formulaType.constant) {
			ArrayList<EvalTerm> arguments = new ArrayList<>();
			arguments.add(result);
			arguments.add(ApplicationEvalTerm.trivialTrue);
			result = new ApplicationEvalTerm(arguments, opToFunction.get("=="), Boolean.class, "==");
		}
		ArrayList<VariableEvalTerm> havocedVars = new ArrayList<>();
		for(VariableEvalTerm variable : inVariables.values()) {
			if(variable.isHavoced) {
				printLnIf(variable.localName + " is havoced");
				havocedVars.add(variable);
			}
		}
		if(havocedVars.size() > 0) {
			ArrayList<EvalTerm> arguments = new ArrayList<>();
			arguments.add(result);
			
			for(VariableEvalTerm havocedVar : havocedVars) {
				ArrayList<EvalTerm> subArgs = new ArrayList<>();
				subArgs.add(havocedVar);
				arguments.add(new ApplicationEvalTerm(subArgs, opToFunction.get("havoc"), Boolean.class, "havoc"));
			}
		
			result = new ApplicationEvalTerm(arguments, opToFunction.get("and"), Boolean.class, "and");
		}
		
		return result;
	}
	
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
			return evalToRational(args.get(0)).compareTo(evalToRational(args.get(1))) <= 0;
		}),

		entry(">=", (args) -> {
			return evalToRational(args.get(0)).compareTo(evalToRational(args.get(1))) >= 0;
		}),

		entry("<", (args) -> {
			return evalToRational(args.get(0)).compareTo(evalToRational(args.get(1))) < 0;
		}),

		entry(">", (args) -> {
			return evalToRational(args.get(0)).compareTo(evalToRational(args.get(1))) > 0;
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
			havocVar(havocedVar);
	
			return true;
		})
	);

	private static Rational evalToRational(EvalTerm arg) {
		Object val = arg.eval();
		return Rational.class.isInstance(val) ? (Rational) val : Rational.valueOf((int) val, 1);
	}
	
	private EvalTerm parseTerm(
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
			Function<ArrayList<EvalTerm>, Object> function = opToFunction.getOrDefault(symbol, null);
			Term[] parameter = termTrans.getParameters();
			switch (symbol) {
				case "=":
					expr1 = parseTerm(parameter[0], inVariables, false);
					expr2 = parseTerm(parameter[1], inVariables, false);
					
					boolean p1IsVar = expr1.type == EvalTerm.formulaType.variable;
					VariableEvalTerm var1 = p1IsVar ? (VariableEvalTerm) expr1 : null;
					boolean isAssignable1 = p1IsVar && var1.isAssignable;
					boolean containsVariable1 = p1IsVar && var1.containsVariable(false, false, null);
					boolean isOutVar1 = p1IsVar && var1.isOutVar;
					
					boolean p2IsVar = expr2.type == EvalTerm.formulaType.variable;
					VariableEvalTerm var2 = p2IsVar ? (VariableEvalTerm) expr2 : null;
					boolean isAssignable2 = p2IsVar && var2.isAssignable;
					boolean containsVariable2 = p2IsVar && var2.containsVariable(false, false, null);
					boolean isOutVar2 = p2IsVar && var2.isOutVar;
					
					VariableEvalTerm variable;
					EvalTerm newValue;
					if(p1IsVar ^ p1IsVar) {
						variable = p1IsVar ? var1 : var2;
						newValue = !p1IsVar ? expr1 : expr2;
					} else if(isAssignable1 ^ isAssignable2) {
						variable = isAssignable1 ? var1 : var2;
						newValue = !isAssignable1 ? expr1 : expr2;
					} else if(isOutVar1 ^ isOutVar2) {
						variable = isOutVar1 ? var1 : var2;
						newValue = !isOutVar1 ? expr1 : expr2;
					} else if(containsVariable1 ^ containsVariable2) {
						printLnIf("Warning: Target of assignment not in top level of equality.\nAttempting reshape...");
						arguments.add(expr1); arguments.add(expr2);
						ApplicationEvalTerm variableContainer = new ApplicationEvalTerm(arguments, function, Boolean.class, symbol);
						ApplicationEvalTerm targetVariable = variableContainer;
						VariableEvalTerm result = null;
						while(result == null/**/) {
							for(EvalTerm arg : targetVariable.args) {
								if(arg.type == EvalTerm.formulaType.variable) {
									VariableEvalTerm possibleTarget = (VariableEvalTerm) arg;
									if(possibleTarget.isAssignable) {
										result = possibleTarget;
									}
									continue;
								} 
								if(arg.containsVariable(true, true, null)) {
									targetVariable = (ApplicationEvalTerm) arg;
									break;
								}
								assert false;
							}
						}
						
						return ApplicationEvalTerm.reverseAssignmentToVariable(result, variableContainer);
					} else if (isAssignable1 && isAssignable2) {
						// assume order is correct
						printLnIf("Warning: Variable assigned to variable, order unknown in term " + termTrans.toString());
						variable = var1;
						newValue = expr2;
					} else {
						printLnIf("WARNING!\nAssignment has no variable. Evaluate equality instead.\n--(" + symbol + "\n" + expr1.toString("  --"));
						printLnIf(expr2.toString("  --")+"\n--)");
						//throw new Exception("Neither subterm is assignable in assignment term " + termTrans.toString());
						return compareTerm(parameter, inVariables, "==");
					}

					arguments.add(variable); arguments.add(newValue);
					
					return new ApplicationEvalTerm(arguments, function, Boolean.class, symbol);
				case "+":
					expr1 = parseTerm(parameter[0], inVariables, false);
					expr2 = parseTerm(parameter[1], inVariables, false);
					
					arguments.add(expr1); arguments.add(expr2);

					return new ApplicationEvalTerm(arguments, function, Integer.class, symbol);
				case "-":
					expr1 = parseTerm(parameter[0], inVariables, false);
					expr2 = parseTerm(parameter[1], inVariables, false);
					
					arguments.add(expr1); arguments.add(expr2);

					return new ApplicationEvalTerm(arguments, function, Integer.class, symbol);
				case "*":
					expr1 = parseTerm(parameter[0], inVariables, false);
					expr2 = parseTerm(parameter[1], inVariables, false);
					
					arguments.add(expr1); arguments.add(expr2);
					
					return new ApplicationEvalTerm(arguments, function, Integer.class, symbol);
				case "<=":
				case ">=":
				case "<":
				case ">":
				case "==":
					return compareTerm(parameter, inVariables, symbol);
				case "not":
					expr1 = parseTerm(parameter[0], inVariables, false);
					
					assert expr1.outType.equals(Boolean.class);
					
					arguments.add(expr1);
					
					if(expr1.type == EvalTerm.formulaType.variable) {
						ArrayList<EvalTerm> args2 = new ArrayList<>();
						args2.add(expr1);
						args2.add(ApplicationEvalTerm.trivialFalse);
						VariableEvalTerm arg2 = (VariableEvalTerm) expr1;
					    if(isRoot && arg2.isAssignable) {
							return new ApplicationEvalTerm(args2, opToFunction.get("="), Boolean.class, "=");
						}
						return new ApplicationEvalTerm(args2, opToFunction.get("=="), Boolean.class, "==");
					}
					
					return new ApplicationEvalTerm(arguments, function, Boolean.class, symbol);
				case "and":
					for(int i = 0; i < parameter.length; i++) {
						EvalTerm arg = parseTerm(parameter[i], inVariables, false);
						assert arg.outType.equals(Boolean.class);
						
						if(arg.type == EvalTerm.formulaType.variable) {
							ArrayList<EvalTerm> args2 = new ArrayList<>();
							args2.add(arg);
							args2.add(ApplicationEvalTerm.trivialTrue);
							VariableEvalTerm arg2 = (VariableEvalTerm) arg;
							if(isRoot && arg2.isAssignable) {
								arg = new ApplicationEvalTerm(args2, opToFunction.get("="), Boolean.class, "=");
							} else {
								arg = new ApplicationEvalTerm(args2, opToFunction.get("=="), Boolean.class, "==");
							}
						}
						
						arguments.add(arg);
					}
					
					Collections.sort(arguments, (a, b) -> compareExpression(a, b));
					
					return new ApplicationEvalTerm(arguments, function, Boolean.class, symbol);
				case "or":
					for(int i = 0; i < parameter.length; i++) {
						EvalTerm arg = parseTerm(parameter[i], inVariables, false);
						assert arg.outType.equals(Boolean.class);
						
						if(arg.type == EvalTerm.formulaType.variable) {
							ArrayList<EvalTerm> args2 = new ArrayList<>();
							args2.add(arg);
							args2.add(ApplicationEvalTerm.trivialTrue);
							VariableEvalTerm arg2 = (VariableEvalTerm) arg;
							if(isRoot && arg2.isAssignable) {
								arg = new ApplicationEvalTerm(args2, opToFunction.get("="), Boolean.class, "=");
							} else {
								arg = new ApplicationEvalTerm(args2, opToFunction.get("=="), Boolean.class, "==");
							}
						}
						
						arguments.add(arg);
					}

					Collections.sort(arguments, (a, b) -> compareExpression(a, b));
					
					return new ApplicationEvalTerm(arguments, function, Boolean.class, symbol);
				case "true":
					return ApplicationEvalTerm.trivialTrue; 
				case "false":
					return ApplicationEvalTerm.trivialFalse; 
				case "store":
					ArrayVarEvalTerm array1 = (ArrayVarEvalTerm) parseTerm(parameter[0], inVariables, false);
					
					expr1 = parseTerm(parameter[1], inVariables, false);
					// The pointer should be an int
					assert Integer.class.equals(expr1.outType);
					
					expr2 = parseTerm(parameter[2], inVariables, false);
					// The new value should be the same type as the arrays entries
					assert expr1.outType.equals(expr2.outType);
					
					arguments.add(array1);
					arguments.add(expr1);
					arguments.add(expr2);
					
					return new ApplicationEvalTerm(arguments, function, ArrayVarEvalTerm.class, symbol);
				case "select":
					expr1 = parseTerm(parameter[0], inVariables, false);
					
					expr2 = parseTerm(parameter[1], inVariables, false);
					// The pointer should be an int
					assert Integer.class.equals(expr2.outType);
					
					arguments.add(expr1);
					arguments.add(expr2);

					return new ApplicationEvalTerm(arguments, function, expr1.outType, symbol);
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
			
			return new ConstantEvalTerm(value, value.getClass()); 
			
		case "de.uni_freiburg.informatik.ultimate.logic.TermVariable":
			TermVariable termVar = (TermVariable) myTerm;
			String varName = termVar.getName();
			return inVariables.get(varName);
			
		default:
			System.out.println("Unknown " + myTerm.getClass().getName());
		}
		return null;
	}
	
	private EvalTerm compareTerm(
			Term[] parameter,
			HashMap<String, VariableEvalTerm> inVariables,
			String symbol) throws Exception {
		ArrayList<EvalTerm> arguments = new ArrayList<>();
		
		EvalTerm expr1 = parseTerm(parameter[0], inVariables, false);
		EvalTerm expr2 = parseTerm(parameter[1], inVariables, false);
		
		arguments.add(expr1); arguments.add(expr2);
		
		Function<ArrayList<EvalTerm>, Object> function = opToFunction.get(symbol);
		
		return new ApplicationEvalTerm(arguments, function, Boolean.class, symbol);
	}

	public BoogieIcfgContainer getRoot() {
		return rootICFG;
	}

	@Override
	public void finish() {
		// not needed
	}

	@Override
	public void init(final ModelType modelType, final int currentModelIndex, final int numberOfModels) {
		// not needed
	}

	@Override
	public boolean performedChanges() {
		return false;
	}
}
