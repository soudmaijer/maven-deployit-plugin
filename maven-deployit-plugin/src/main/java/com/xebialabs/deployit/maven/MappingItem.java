/*
 * This file is part of Maven Deployit plugin.
 *
 * Maven Deployit plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maven Deployit plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Maven Deployit plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.xebialabs.deployit.maven;

import com.google.common.collect.Lists;
import com.xebialabs.deployit.core.api.dto.RepositoryObject;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

/**
 */
public class MappingItem extends ConfigurationItem {

	private static final List<String> SKIPPED_ITEMS = Lists.newArrayList("keyValuePairs", "source", "target", "label");

	private String source;
	private String target;
	private List<Map<String, String>> keyValuePairs;

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getLabel() {
		return '"' + source + " to " + target + '"';
	}

	//@Override

	public void addParameter(String name, Object value) {
		if (SKIPPED_ITEMS.contains(name))
			return;

		super.addParameter(name, value);
	}

	public void setKeyValuePairs(List<Map<String, String>> keyValuePairs) {
		this.keyValuePairs = keyValuePairs;
	}

	public List<Map<String, String>> getKeyValuePairs() {
		return keyValuePairs;
	}

	public boolean equals(String sourceMapping, String targetMapping) {
		return getSource().equals(sourceMapping) && getTarget().equals(targetMapping);
	}

	@Override
	public String toString() {
		return String.format("MappingItem(%s,%s,%s,%s)", source, target, type, getProperties().toString());
	}


	public RepositoryObject toRepositoryObject() {
		RepositoryObject repositoryObject = new com.xebialabs.deployit.core.api.dto.ConfigurationItem(type);
		repositoryObject.setId(getId());

		repositoryObject.setProperty("source",source);
		repositoryObject.setProperty("target", target);
		for (Map.Entry<String, Object> entry : getProperties().entrySet()) {
			final String key = entry.getKey();
			final Object value = entry.getValue();
			repositoryObject.setProperty(key, value);
		}

		if (keyValuePairs != null && !keyValuePairs.isEmpty()) {
			repositoryObject.setProperty("keyValuePairs", keyValuePairs);
		}
		return repositoryObject;
	}

	//Source    Applications/wls/1.2.2/wls-1.2.2.ear
	//Target    Infrastructure/Domain WLS Petshop/Petshop Server 1
	//Id        Infrastructure/Domain WLS Petshop/Petshop Server 1/wls
	private String getId() {
		final String[] split = StringUtils.split(source, "/");
		final String applicationName = split[1];
		return getTarget() + "/" + applicationName;
	}
}
