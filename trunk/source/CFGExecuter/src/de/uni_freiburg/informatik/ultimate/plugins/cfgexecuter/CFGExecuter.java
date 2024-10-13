package de.uni_freiburg.informatik.ultimate.plugins.cfgexecuter;

import java.util.Collections;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.core.model.IGenerator;
import de.uni_freiburg.informatik.ultimate.core.model.models.IElement;
import de.uni_freiburg.informatik.ultimate.core.model.models.ModelType;
import de.uni_freiburg.informatik.ultimate.core.model.observers.IObserver;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;

public class CFGExecuter implements IGenerator {

	public static final String PLUGIN_ID = Activator.PLUGIN_ID;
	private IUltimateServiceProvider mServices;
	private CFGExecuterObserver mObserver;
	private ModelType mInputDefinition;

	@Override
	public List<String> getDesiredToolIds() {
		return Collections.emptyList();
	}

	@Override
	public ModelQuery getModelQuery() {
		return ModelQuery.LAST;
	}

	@Override
	public String getPluginName() {
		return Activator.PLUGIN_NAME;
	}

	@Override
	public String getPluginID() {
		System.out.println("Havoc max int size " + CFGExecuterObserver.havocMax);
		return PLUGIN_ID;
	}

	@Override
	public void init() {
		// not needed
	}

	@Override
	public boolean isGuiRequired() {
		return false;
	}

	@Override
	public void setServices(final IUltimateServiceProvider services) {
		mServices = services;
	}
	
	@Override
	public IPreferenceInitializer getPreferences() {
		return null;
	}

	@Override
	public List<IObserver> getObservers() {
		mObserver = new CFGExecuterObserver(mServices);
		return Collections.singletonList((IObserver) mObserver);
	}

	@Override
	public void setInputDefinition(final ModelType graphType) {
		mInputDefinition = graphType;
	}
	@Override
	public ModelType getOutputDefinition() {
		return new ModelType(Activator.PLUGIN_ID, ModelType.Type.OTHER, mInputDefinition.getFileNames());
	}

	@Override
	public IElement getModel() {
		return mObserver.getRoot();
	}

	@Override
	public void finish() {
		// not needed
	}
	
	

}
