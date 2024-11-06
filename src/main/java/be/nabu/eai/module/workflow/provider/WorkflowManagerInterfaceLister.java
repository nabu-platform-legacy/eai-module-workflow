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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import be.nabu.eai.developer.api.InterfaceLister;
import be.nabu.eai.developer.util.InterfaceDescriptionImpl;

public class WorkflowManagerInterfaceLister implements InterfaceLister {

private static Collection<InterfaceDescription> descriptions = null;
	
	@Override
	public Collection<InterfaceDescription> getInterfaces() {
		if (descriptions == null) {
			synchronized(WorkflowManagerInterfaceLister.class) {
				if (descriptions == null) {
					List<InterfaceDescription> descriptions = new ArrayList<InterfaceDescription>();
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Create Workflow", "be.nabu.eai.module.workflow.provider.WorkflowManager.createWorkflow"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Update Workflow", "be.nabu.eai.module.workflow.provider.WorkflowManager.updateWorkflow"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Get Workflow", "be.nabu.eai.module.workflow.provider.WorkflowManager.getWorkflow"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Get Workflows", "be.nabu.eai.module.workflow.provider.WorkflowManager.getWorkflows"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Create Transition", "be.nabu.eai.module.workflow.provider.WorkflowManager.createTransition"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Update Transition", "be.nabu.eai.module.workflow.provider.WorkflowManager.updateTransition"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Get Transitions", "be.nabu.eai.module.workflow.provider.WorkflowManager.getTransitions"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Create Workflow Properties", "be.nabu.eai.module.workflow.provider.WorkflowManager.createWorkflowProperties"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Update Workflow Properties", "be.nabu.eai.module.workflow.provider.WorkflowManager.updateWorkflowProperties"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Get Workflow Properties", "be.nabu.eai.module.workflow.provider.WorkflowManager.getWorkflowProperties"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Create Batch", "be.nabu.eai.module.workflow.provider.WorkflowManager.createBatch"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Update Batch", "be.nabu.eai.module.workflow.provider.WorkflowManager.updateBatch"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Calculate Batch State", "be.nabu.eai.module.workflow.provider.WorkflowManager.calculateBatchState"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Get Batch", "be.nabu.eai.module.workflow.provider.WorkflowManager.getBatch"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Get Batches", "be.nabu.eai.module.workflow.provider.WorkflowManager.getBatches"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Get Amount of Workflows", "be.nabu.eai.module.workflow.provider.WorkflowManager.getAmountOfWorkflows"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Merge a definition that is actively used", "be.nabu.eai.module.workflow.provider.WorkflowManager.mergeDefinition"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Get the definition of a specific version", "be.nabu.eai.module.workflow.provider.WorkflowManager.getDefinition"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Workflow listener", "be.nabu.eai.module.workflow.api.WorkflowListener.transition"));
					WorkflowManagerInterfaceLister.descriptions = descriptions;
				}
			}
		}
		return descriptions;
	}

}
