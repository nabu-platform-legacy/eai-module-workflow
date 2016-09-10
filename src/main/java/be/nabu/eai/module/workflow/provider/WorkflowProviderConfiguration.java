package be.nabu.eai.module.workflow.provider;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "workflowProvider")
public class WorkflowProviderConfiguration {

	private DefinedService createWorkflow, updateWorkflow, getWorkflow, 
		createTransition, updateTransition, getTransitions,
		createWorkflowProperties, updateWorkflowProperties, getWorkflowProperties,
		getRunningWorkflows;

	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.createWorkflow")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@NotNull
	public DefinedService getCreateWorkflow() {
		return createWorkflow;
	}
	public void setCreateWorkflow(DefinedService createWorkflow) {
		this.createWorkflow = createWorkflow;
	}

	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.updateWorkflow")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@NotNull
	public DefinedService getUpdateWorkflow() {
		return updateWorkflow;
	}
	public void setUpdateWorkflow(DefinedService updateWorkflow) {
		this.updateWorkflow = updateWorkflow;
	}

	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.getWorkflow")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@NotNull
	public DefinedService getGetWorkflow() {
		return getWorkflow;
	}
	public void setGetWorkflow(DefinedService getWorkflow) {
		this.getWorkflow = getWorkflow;
	}
	
	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.getRunningWorkflows")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getGetRunningWorkflows() {
		return getRunningWorkflows;
	}
	public void setGetRunningWorkflows(DefinedService getRunningWorkflows) {
		this.getRunningWorkflows = getRunningWorkflows;
	}
	
	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.createTransition")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@NotNull
	public DefinedService getCreateTransition() {
		return createTransition;
	}
	public void setCreateTransition(DefinedService createTransition) {
		this.createTransition = createTransition;
	}

	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.updateTransition")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@NotNull
	public DefinedService getUpdateTransition() {
		return updateTransition;
	}
	public void setUpdateTransition(DefinedService updateTransition) {
		this.updateTransition = updateTransition;
	}

	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.getTransition")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@NotNull
	public DefinedService getGetTransitions() {
		return getTransitions;
	}
	public void setGetTransitions(DefinedService getTransitions) {
		this.getTransitions = getTransitions;
	}

	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.createWorkflowProperties")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@NotNull
	public DefinedService getCreateWorkflowProperties() {
		return createWorkflowProperties;
	}
	public void setCreateWorkflowProperties(DefinedService createWorkflowProperty) {
		this.createWorkflowProperties = createWorkflowProperty;
	}

	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.updateWorkflowProperties")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@NotNull
	public DefinedService getUpdateWorkflowProperties() {
		return updateWorkflowProperties;
	}
	public void setUpdateWorkflowProperties(DefinedService updateWorkflowProperty) {
		this.updateWorkflowProperties = updateWorkflowProperty;
	}

	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.getWorkflowProperties")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@NotNull
	public DefinedService getGetWorkflowProperties() {
		return getWorkflowProperties;
	}
	public void setGetWorkflowProperties(DefinedService getWorkflowProperties) {
		this.getWorkflowProperties = getWorkflowProperties;
	}
	
}
