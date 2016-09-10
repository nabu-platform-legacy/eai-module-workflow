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
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public DefinedService getService() {
		return service;
	}
	public void setService(DefinedService service) {
		this.service = service;
	}
	public WorkflowState getTargetState() {
		return targetState;
	}
	public void setTargetState(WorkflowState targetState) {
		this.targetState = targetState;
	}
	public Map getInputMapping() {
		return inputMapping;
	}
	public void setInputMapping(Map inputMapping) {
		this.inputMapping = inputMapping;
	}
	public List<String> getFieldsToStore() {
		return fieldsToStore;
	}
	public void setFieldsToStore(List<String> fieldsToStore) {
		this.fieldsToStore = fieldsToStore;
	}
	
}
