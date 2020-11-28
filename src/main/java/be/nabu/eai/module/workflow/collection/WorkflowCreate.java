package be.nabu.eai.module.workflow.collection;

import be.nabu.libs.types.api.annotation.ComplexTypeDescriptor;
import be.nabu.libs.types.api.annotation.Field;

@ComplexTypeDescriptor(propOrder = {"name" })
public class WorkflowCreate {
	
	private String name;
	
	@Field(minOccurs = 1)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
}
