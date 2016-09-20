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
					WorkflowManagerInterfaceLister.descriptions = descriptions;
				}
			}
		}
		return descriptions;
	}

}
