package com.xebialabs.deployit.maven;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * Describe a deployit middleware resource for a deployment package.
 */
public class MiddlewareResource implements PackagedItem {

	public static final String MR_SUFFIX = "Resource";

	private String configurationName;

	private String type;

	final private Map<String, String> properties = Maps.newHashMap();

	public void addParameter(String name, String value) {

		if ("type".equals(name)) {
			type = value.toString();
			return;
		}

		if ("configurationName".equalsIgnoreCase(name)) {
			this.configurationName = value;
		}

		properties.put(name, value);
	}


	public String getConfigurationName() {
		return configurationName;
	}

	public void setConfigurationName(String configurationName) {
		this.configurationName = configurationName;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public String getEntryKey() {
		if (StringUtils.isEmpty(configurationName))
			throw new IllegalStateException("configurationName is mandatory");

		return configurationName + MR_SUFFIX;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
