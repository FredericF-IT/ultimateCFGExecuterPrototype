
package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.examples;

import java.util.HashMap;

import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.CFGExecuterObserver;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.EvaluationState;
import de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter.evaluation.VariableEvalTerm;

public class Tests{
	
	public static boolean testForFile(String fileName, 
			EvaluationState evStateRoot,
			String finalState) {
		CFGExecuterObserver.printLnIf("Attempting test of file " + fileName);

		switch(fileName) {
			case "divisibilityWithArray.bpl": 
				return testDivisibilityWithArray(evStateRoot, finalState);
			case "bank.bpl": 
				return bank(evStateRoot, finalState);
		}
		CFGExecuterObserver.printLnIf("Found no test method for file " + fileName);
		return true;
	}

	private static void printCorrectState(String state) {
		CFGExecuterObserver.printLnIf("Execution arrived at expected state " + state + ".");
	}
	
	private static void printIncorrectState(String falseState, String expectedSTate) {
		CFGExecuterObserver.printLnIf("Execution arrived at unexpected state: " + falseState + " is not " + expectedSTate);
	}
	
	private static boolean testDivisibilityWithArray(EvaluationState evStateRoot, 
			String finalState) {
		String state = "Unknown state";
		
		HashMap<String, VariableEvalTerm> InVars = evStateRoot.getVariables();
		HashMap<String, VariableEvalTerm> OutVars = evStateRoot.getFinalChild().getVariables();
		
		int n = (int) InVars.get("main_n").eval();
		int i = 0;
		int iEvaluated = 0;
		int nEvaluated = (int) OutVars.get("main_n").eval();
		if(n >= 0) {
			// variable i will not be initialized if n < 0
			iEvaluated = (int) OutVars.get("main_i").eval();
			
			int[] a = new int[n * 4 + 1];
			a[n * 4] = 23;
			state = "L26-1";
			while(a[i] != 23) {
				i += 4;
				if(i > n * 4) {
					state = "mainErr0ASSERT_VIOLATIONASSERT";
					break;
				}
			}
			
			state = "mainEXIT";
		} else {
			state = "$Ultimate##0";
		}
		
		boolean nCorrect = n == nEvaluated;
		boolean iCorrect = i == iEvaluated;	
		boolean stateCorrect = finalState.equals(state);
		
		if(stateCorrect) {
			printCorrectState(state);
		} else {
			printIncorrectState(finalState, state);
		}
		
		if(nCorrect && iCorrect) {
			CFGExecuterObserver.printLnIf("Variable values match expected outcome:\n   i = " + i + "\n   n = " + n);
		} else {
			CFGExecuterObserver.printLnIf("Variable values dont match.");
			if(nCorrect) {
				CFGExecuterObserver.printLnIf("i should be " + i + " but equals " + iEvaluated);
			} else {
				CFGExecuterObserver.printLnIf("n should be " + n + " but equals " + nEvaluated);
			}
			return false;
		}
		
		return stateCorrect;
	}
	
	private static boolean bank(EvaluationState evStateRoot, 
			String finalState) {
		HashMap<String, VariableEvalTerm> InVars = evStateRoot.getChild().getVariables();
		HashMap<String, VariableEvalTerm> OutVars = evStateRoot.getFinalChild().getVariables();
		
		boolean isOverdraft = (boolean) InVars.get("isOverdraft").eval();
		int balance = (int) InVars.get("main_balance").eval();
		int value = (int) InVars.get("main_value").eval();
		int functionValue = (int) InVars.get("main_functionValue").eval();

		String state = "mainEXIT";
		balance = 0;
		if(functionValue > 0) {
			if(value >= 0) {
				if(isOverdraft) {
					if(!(balance < 0)) {
						state = "mainErr0ASSERT_VIOLATIONASSERT";
					}
				} else {
					balance = balance - value;
					if(balance < 0) {
						isOverdraft = true;
					}
				}
			}
		} else if(functionValue <= 0) {
			if(value >= 0) {
				balance = balance + value;
			}
		}
		
		int balanceEvaluated = (int) OutVars.get("main_balance").eval();
		int valueEvaluated = (int) OutVars.get("main_value").eval();
		int functionValueEvaluated = (int) OutVars.get("main_functionValue").eval();
		boolean balanceCorrect = balance == balanceEvaluated;
		boolean valueCorrect = value == valueEvaluated;
		boolean functionValueCorrect = functionValue == functionValueEvaluated;

		boolean stateCorrect = finalState.equals(state);
		
		if(stateCorrect) {
			printCorrectState(state);
		} else {
			printIncorrectState(finalState, state);
		}
		
		if(balanceCorrect && valueCorrect && functionValueCorrect) {
			CFGExecuterObserver.printLnIf("Variable values match expected outcome:\n   balance = " + balance 
					+ "\n   value = " + value
					+ "\n   functionValue = " + functionValue);
		} else {
			CFGExecuterObserver.printLnIf("Variable values dont match.");
			if(!balanceCorrect) {
				CFGExecuterObserver.printLnIf("balance should be " + balance + " but equals " + balanceEvaluated);
			} else if (!valueCorrect) {
				CFGExecuterObserver.printLnIf("value should be " + value + " but equals " + valueEvaluated);
			} else if (!functionValueCorrect) {
				CFGExecuterObserver.printLnIf("functionValue should be " + functionValue + " but equals " + functionValueEvaluated);
			}
			return false;
		}
		
		return stateCorrect;
	}
}