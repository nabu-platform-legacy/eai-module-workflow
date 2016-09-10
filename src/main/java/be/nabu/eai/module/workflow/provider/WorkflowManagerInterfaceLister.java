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
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Create Transition", "be.nabu.eai.module.workflow.provider.WorkflowManager.createTransition"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Update Transition", "be.nabu.eai.module.workflow.provider.WorkflowManager.updateTransition"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Get Transitions", "be.nabu.eai.module.workflow.provider.WorkflowManager.getTransitions"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Create Workflow Property", "be.nabu.eai.module.workflow.provider.WorkflowManager.createWorkflowProperty"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Update Workflow Property", "be.nabu.eai.module.workflow.provider.WorkflowManager.updateWorkflowProperty"));
					descriptions.add(new InterfaceDescriptionImpl("Workflow", "Get Workflow Properties", "be.nabu.eai.module.workflow.provider.WorkflowManager.getWorkflowProperties"));
					WorkflowManagerInterfaceLister.descriptions = descriptions;
				}
			}
		}
		return descriptions;
	}

}
