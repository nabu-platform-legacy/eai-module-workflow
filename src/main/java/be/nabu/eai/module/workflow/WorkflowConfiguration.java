package be.nabu.eai.module.workflow;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.module.workflow.provider.WorkflowProvider;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.artifacts.api.DataSourceProviderArtifact;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "workflow")
@XmlType(propOrder = { "connection", "provider", "version", "states", "permissionService", "roleService", "tokenValidatorService", "transitionListeners" })
public class WorkflowConfiguration {
	
	private boolean version;
	private DataSourceProviderArtifact connection;
	private WorkflowProvider provider;
	private List<WorkflowState> states;
	private DefinedService permissionService, roleService, tokenValidatorService;
	private List<DefinedService> transitionListeners;
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public WorkflowProvider getProvider() {
		if (provider == null) {
			provider = (WorkflowProvider) EAIResourceRepository.getInstance().resolve("nabu.misc.workflow.providers.basic.provider");
		}
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
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DataSourceProviderArtifact getConnection() {
		return connection;
	}
	public void setConnection(DataSourceProviderArtifact connection) {
		this.connection = connection;
	}
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.module.workflow.api.WorkflowListener.transition")
	public List<DefinedService> getTransitionListeners() {
		return transitionListeners;
	}
	public void setTransitionListeners(List<DefinedService> transitionListeners) {
		this.transitionListeners = transitionListeners;
	}
	
	public boolean isVersion() {
		return version;
	}
	public void setVersion(boolean version) {
		this.version = version;
	}
	
	
}
