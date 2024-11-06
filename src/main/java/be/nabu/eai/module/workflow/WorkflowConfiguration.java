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
@XmlType(propOrder = { "connection", "provider", "version", "states", "permissionService", "roleService", "tokenValidatorService", "transitionListeners", "basePath" })
public class WorkflowConfiguration {
	
	private boolean version;
	private DataSourceProviderArtifact connection;
	private WorkflowProvider provider;
	private List<WorkflowState> states;
	private DefinedService permissionService, roleService, tokenValidatorService;
	private List<DefinedService> transitionListeners;
	private String basePath;
	
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
	
	public String getBasePath() {
		return basePath;
	}
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}
	
}
