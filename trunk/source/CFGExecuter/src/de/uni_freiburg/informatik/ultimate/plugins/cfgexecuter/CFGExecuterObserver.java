package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;

import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.EvalTerm;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.EvalTerm.DataStructure;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.ExecutableTerm;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.Guard;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.HavocGen;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.TermParser;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.ArrayVarEvalTerm;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.VariableEvalTerm;
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
			ArrayList<ExecutableTerm> edges = exploreEdges(elements.get(key));
			print = false;

			EvaluationState evStateRoot = new EvaluationState(edges.get(0).transitionTerm.outVars);
			EvaluationState evState = evStateRoot;
			
			printLnIf("Initial Vars: " + VariableEvalTerm.stringifyVarEvalTerms(evStateRoot.getVariables()));
			boolean state = false;
			String lastState = edges.get(0).source;
			ArrayList<String> pathTaken = new ArrayList<>();
			pathTaken.add(lastState + " " + VariableEvalTerm.stringifyVarEvalTerms(evState.getVariables()));
			havocRecord = new HashMap<>();

			print = true;
			printLnIf("------ EXECUTE EXECUTABLE TERMS ------");
			print = false;
			for(int j = 0; j < edges.size(); j++) {
				state = false;
				ExecutableTerm exec = edges.get(j);
				Guard guard = exec.transitionTerm.getGuard();
				if(!guard.isFullfilled(evState)) {
					printLnIf("Cannot take edge " + exec.toString() + " with Vars:\n" + 
							VariableEvalTerm.stringifyVarEvalTerms(evState.getVariables()));
					continue;
				}

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
					edges = exec.children;
				} else {
					print = true;
					printLnIf("Trying " + exec.toString() + " with Vars:\n" + 
						VariableEvalTerm.stringifyVarEvalTerms(evState.getVariables()) + 
						"\nPath cannot be taken");
					assert false;
					// should not arrive here due to guard
				}
			}
			if(!state) {
				print = true;
				printLnIf("No path can be taken at this point!\nPath:\n" +
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
	
	private enum SortNames {
		Bool, Int, Array
	}
	private static boolean print = false;
	public static void printLnIf(Object text) { if(print) System.out.println(text); }
	public static void printIf(Object text) { if(print) System.out.print(text); }
	
	private static VariableEvalTerm baseValueAny(String trueName, 
			boolean isAssignable, 
			boolean isHavocedIn, 
			boolean isHavocedOut, 
			boolean isOutVar, 
			TermVariable inTerm) {
		Sort sort = inTerm.getSort();
		VariableEvalTerm newVer = null;
		String name = inTerm.getName();
		switch(SortNames.valueOf(sort.getName())) {
			case Bool:
				printLnIf("Var " + name + " is bool");
				newVer = new VariableEvalTerm(name, trueName, DataStructure.bool, HavocGen.havocBool(), isAssignable, isOutVar, isHavocedIn, isHavocedOut);
				break;
			//case "Rational":
			//	printLnIf("Var " + name + " is rational");
			//	newVer = new VariableEvalTerm(name, trueName, Rational.class, havocRational(havocBool()), isAssignable, isOutVar, isHavocedIn, isHavocedOut);
			//	break;
			case Int:
				printLnIf("Var " + name + " is int");
				newVer = new VariableEvalTerm(name, trueName, DataStructure.integer, HavocGen.havocInt(HavocGen.havocBool()), isAssignable, isOutVar, isHavocedIn, isHavocedOut);
				break;
			case Array:
				Sort arraySort = inTerm.getDeclaredSort();
				String indexType = arraySort.getArguments()[0].getName();
				DataStructure indexTypeDS = null;
				switch(SortNames.valueOf(indexType)) {
					case Bool:
						indexTypeDS = DataStructure.bool; 
						break;
					case Int:
						indexTypeDS = DataStructure.integer; 
						break;
					case Array: // array cannot have array as key
					default:
						assert false;
				}
				String valueType = arraySort.getArguments()[1].getName();
				printLnIf("Var " + name + " is Array["+indexType+"]"+valueType);
				switch(SortNames.valueOf(valueType)) {
					case Bool:
						newVer = new ArrayVarEvalTerm(name, trueName, HavocGen.havocBool(), DataStructure.bool, indexTypeDS, isAssignable, isOutVar);
						break;
					case Int:
						newVer = new ArrayVarEvalTerm(name, trueName, HavocGen.havocInt(HavocGen.havocBool()), DataStructure.integer, indexTypeDS, isAssignable, isOutVar);
						break;
					case Array:
						assert false;
						// TODO Array of arrays
						break;
				default:
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
		boolean isHavocedIn = transFormula.isHavocedIn(inVar);
		boolean isHavocedOut = transFormula.isHavocedOut(inVar);
		boolean isOutVar = transFormula.getOutVars().containsValue(inTerm);

		return baseValueAny(trueName, isAssignable, isHavocedIn, isHavocedOut, isOutVar, inTerm);
	}
	
	private static VariableEvalTerm baseValueAux(TermVariable inTerm) {
		return baseValueAny(inTerm.getName(), true, false, false, false, inTerm);
	}
	
	public static HashMap<String, VariableEvalTerm> copyMap(HashMap<String, VariableEvalTerm> inVars) {
		HashMap<String, VariableEvalTerm> inVarsCopy = new HashMap<>();
		for(Entry<String, VariableEvalTerm> inVar : inVars.entrySet()) {
			inVarsCopy.put(inVar.getKey(), inVar.getValue().copy());
		}
		return inVarsCopy;
	}
	
	public static ExecutableTerm transFormulaToEvaluatable(UnmodifiableTransFormula transFormula, IcfgLocation source, IcfgLocation target) {
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
			allVariables.put(inVar.getName(), baseValueAux(inVar));
		}
		
		printIf("Outvars:");
		for(String wanted : outVariables.keySet()) {
			VariableEvalTerm wantedVar = allVariables.get(wanted);
			printIf(" " + wantedVar.trueName + "=" + wantedVar.localName);
		}
		printLnIf("");
		
		EvalTerm transitionTerm;
		try {
			transitionTerm = TermParser.createEvalTerm(transFormula.getFormula(), allVariables);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		printLnIf("Executable Term:\n" + transitionTerm.toString("") + "\n");
		return new ExecutableTerm(source.toString(), target.toString(), transitionTerm, inVariables, outVariables);
	}
	
	public static HashMap<String, Object> havocRecord;

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
