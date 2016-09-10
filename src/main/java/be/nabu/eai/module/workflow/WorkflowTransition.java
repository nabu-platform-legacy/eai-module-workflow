package be.nabu.eai.module.workflow;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.util.KeyValueMapAdapter;
import be.nabu.libs.services.api.DefinedService;

// can not directly refer to target state as this may result in circular references!!
// must refer to the id of the target state, separate resolving
public class WorkflowTransition {
	// a generated if for this state
	private String id;
	// the name of the state
	private String name, description;
	// the service to execute
	private DefinedService service;
	// the state we move to after the transition is done
	private String targetStateId;
	// the fields in the return value of the service to store as workflow state
	private Map<String, String> fieldsToStore;
	// the roles that are allowed to run this transition
	private List<String> roles;
	
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
	
	public String getTargetStateId() {
		return targetStateId;
	}
	public void setTargetStateId(String targetStateId) {
		this.targetStateId = targetStateId;
	}
	
	@XmlJavaTypeAdapter(KeyValueMapAdapter.class)
	public Map<String, String> getFieldsToStore() {
		return fieldsToStore;
	}
	public void setFieldsToStore(Map<String, String> fieldsToStore) {
		this.fieldsToStore = fieldsToStore;
	}
	
	public List<String> getRoles() {
		return roles;
	}
	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
	
}
