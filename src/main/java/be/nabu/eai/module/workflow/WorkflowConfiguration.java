package be.nabu.eai.module.workflow;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.module.workflow.provider.WorkflowProvider;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "workflow")
public class WorkflowConfiguration {
	
	private WorkflowProvider provider;
	private List<WorkflowTransition> initialTransitions;
	private List<WorkflowState> states;
	private DefinedService permissionService, roleService, tokenValidatorService;
	
	public List<WorkflowTransition> getInitialTransitions() {
		if (initialTransitions == null) {
			initialTransitions = new ArrayList<WorkflowTransition>();
		}
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
	
	public List<WorkflowState> getStates() {
		if (states == null) {
			states = new ArrayList<WorkflowState>();
		}
		return states;
	}
	public void setStates(List<WorkflowState> states) {
		this.states = states;
	}
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.PermissionHandler.hasPermission")
	public DefinedService getPermissionService() {
		return permissionService;
	}
	public void setPermissionService(DefinedService permissionService) {
		this.permissionService = permissionService;
	}

	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.RoleHandler.hasRole")
	public DefinedService getRoleService() {
		return roleService;
	}
	public void setRoleService(DefinedService roleService) {
		this.roleService = roleService;
	}

	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.libs.authentication.api.TokenValidator.isValid")
	public DefinedService getTokenValidatorService() {
		return tokenValidatorService;
	}
	public void setTokenValidatorService(DefinedService tokenValidatorService) {
		this.tokenValidatorService = tokenValidatorService;
	}
	
}
