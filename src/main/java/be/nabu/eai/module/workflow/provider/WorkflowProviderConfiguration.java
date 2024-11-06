/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
		getWorkflows, createBatch, updateBatch, calculateBatchState, getBatch, getBatches,
		getAmountOfWorkflows, mergeDefinition, getDefinition;

	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.createWorkflow")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@NotNull
	public DefinedService getCreateWorkflow() {
		return createWorkflow;
	}
	public void setCreateWorkflow(DefinedService createWorkflow) {
		this.createWorkflow = createWorkflow;
	}

	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.mergeDefinition")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getMergeDefinition() {
		return mergeDefinition;
	}
	public void setMergeDefinition(DefinedService mergeDefinition) {
		this.mergeDefinition = mergeDefinition;
	}
	
	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.getDefinition")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getGetDefinition() {
		return getDefinition;
	}
	public void setGetDefinition(DefinedService getDefinition) {
		this.getDefinition = getDefinition;
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
	
	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.getWorkflows")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getGetWorkflows() {
		return getWorkflows;
	}
	public void setGetWorkflows(DefinedService getWorkflows) {
		this.getWorkflows = getWorkflows;
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

	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.getTransitions")
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
	
	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.createBatch")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getCreateBatch() {
		return createBatch;
	}
	public void setCreateBatch(DefinedService createBatch) {
		this.createBatch = createBatch;
	}
	
	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.updateBatch")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getUpdateBatch() {
		return updateBatch;
	}
	public void setUpdateBatch(DefinedService updateBatch) {
		this.updateBatch = updateBatch;
	}
	
	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.calculateBatchState")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getCalculateBatchState() {
		return calculateBatchState;
	}
	public void setCalculateBatchState(DefinedService calculateBatchState) {
		this.calculateBatchState = calculateBatchState;
	}
	
	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.getBatch")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getGetBatch() {
		return getBatch;
	}
	public void setGetBatch(DefinedService getBatch) {
		this.getBatch = getBatch;
	}
	
	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.getBatches")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getGetBatches() {
		return getBatches;
	}
	public void setGetBatches(DefinedService getBatches) {
		this.getBatches = getBatches;
	}

	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.provider.WorkflowManager.getAmountOfWorkflows")
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedService getGetAmountOfWorkflows() {
		return getAmountOfWorkflows;
	}
	public void setGetAmountOfWorkflows(DefinedService getAmountOfWorkflows) {
		this.getAmountOfWorkflows = getAmountOfWorkflows;
	}
	
}
