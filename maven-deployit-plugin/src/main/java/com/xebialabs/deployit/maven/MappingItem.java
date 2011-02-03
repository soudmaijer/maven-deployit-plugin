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

import java.util.List;
import java.util.Map;

/**
 */
public class MappingItem extends ConfigurationItem {

	private static final List<String> SKIPPED_ITEMS = Lists.newArrayList("keyValuePairs","source","target","label");

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
		return '"' + source + " to " + target + " for " + DeployMojo.DEFAULT_DEPLOYMENT + '"';
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
}
