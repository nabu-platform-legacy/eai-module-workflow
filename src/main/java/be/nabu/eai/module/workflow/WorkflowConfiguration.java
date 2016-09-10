package be.nabu.eai.module.workflow;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.module.workflow.provider.WorkflowProvider;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;

public class WorkflowConfiguration {
	
	private WorkflowProvider provider;
	private List<WorkflowTransition> initialTransitions;

	public List<WorkflowTransition> getInitialTransitions() {
		return initialTransitions;
	}
	public void setInitialTransitions(List<WorkflowTransition> initialTransitions) {
		this.initialTransitions = initialTransitions;
	}
	
	@NotNull
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public WorkflowProvider getProvider() {
		return provider;
	}
	public void setProvider(WorkflowProvider provider) {
		this.provider = provider;
	}
	
}
