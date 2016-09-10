package be.nabu.eai.module.workflow;

import java.util.List;

import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.vm.step.Map;

public class WorkflowTransition {
	// a generated if for this state
	private String id;
	// the name of the state
	private String name, description;
	// the service to execute
	private DefinedService service;
	// the state we move to after the transition is done
	private WorkflowState targetState;
	// the map step required to map the input
	private Map inputMapping;
	// the fields in the return value of the service to store as workflow state
	private List<String> fieldsToStore;
}
