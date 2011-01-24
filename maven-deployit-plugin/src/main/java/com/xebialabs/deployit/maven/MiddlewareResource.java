package com.xebialabs.deployit.maven;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Describe a deployit middleware resource for a deployment package.
 */
public class MiddlewareResource implements PackagedItem{

	private String name;

	private String type;

	final private Map<String, String> properties = Maps.newHashMap();

	public void addParameter(String name, String value) {
		if ("name".equalsIgnoreCase(name)) {
			this.name = value;
			return;
		}

		if ("type".equals(name)) {
			type = value.toString();
			return;
		}

		properties.put(name, value);
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public String getEntryKey() {
		return getName();
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
