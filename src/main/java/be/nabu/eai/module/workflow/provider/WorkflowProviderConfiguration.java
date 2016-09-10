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
		createTransition, updateTransition, getTransition,
		createWorkflowProperty, updateWorkflowProperty, getWorkflowProperties;

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
	public DefinedService getGetTransition() {
		return getTransition;
	}
	public void setGetTransition(DefinedService getTransition) {
		this.getTransition = getTransition;
	}

	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.createWorkflowProperty")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@NotNull
	public DefinedService getCreateWorkflowProperty() {
		return createWorkflowProperty;
	}
	public void setCreateWorkflowProperty(DefinedService createWorkflowProperty) {
		this.createWorkflowProperty = createWorkflowProperty;
	}

	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.updateWorkflowProperty")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@NotNull
	public DefinedService getUpdateWorkflowProperty() {
		return updateWorkflowProperty;
	}
	public void setUpdateWorkflowProperty(DefinedService updateWorkflowProperty) {
		this.updateWorkflowProperty = updateWorkflowProperty;
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
