package be.nabu.eai.module.workflow;

import java.util.UUID;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class UuidXmlAdapter extends XmlAdapter<String, UUID> {

	@Override
	public UUID unmarshal(String v) throws Exception {
		return v == null ? null : (v.indexOf('-') > 0 ? UUID.fromString(v) : WorkflowManager.fromString(v));
	}

	@Override
	public String marshal(UUID v) throws Exception {
		return v == null ? null : v.toString().replace("-", "");
	}

}
