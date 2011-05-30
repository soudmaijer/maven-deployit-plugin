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

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

public class ConfigurationItem {

	protected String type;
	private String label;
	private boolean addedToEnvironment = true;

	final private Map<String, Object> properties = Maps.newHashMap();


	public String getType() {
		return type;
	}

	public String getLabel() {
		if (label == null) {
			throw new IllegalStateException("Label cannot be null");
		}
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void addParameter(String name, Object value) {
		if ("addedToEnvironment".equals(name)) {
			addedToEnvironment = Boolean.parseBoolean(value.toString());
			return;
		}

		if ("type".equals(name)) {
			//TODO: add a check 'type contains at least a dot'
			setType(value.toString());
			return;
		}

		if ("label".equals(name)) {
			label = value.toString();
			return;
		}
		properties.put(name, value);



	}

	public boolean isAddedToEnvironment() {
		return addedToEnvironment;
	}

	public void setAddedToEnvironment(boolean addedToEnvironment) {
		this.addedToEnvironment = addedToEnvironment;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	@Override
	public String toString() {
		//Joiner.MapJoiner mapJoiner = Joiner.on(",").withKeyValueSeparator("->").appendTo()
		//String properties = mapJoiner.join(ci.getProperties());
		final Set<Map.Entry<String,Object>> entries = getProperties().entrySet();
		String properties = "{";
		boolean first =  false;
		for( Map.Entry<String,Object> e:entries) {
			properties +=(first ? ",": "" )+ "\""+e.getKey()+"\":\""+e.getValue()+"\"";
			first = true ;
		}
		properties+="}";

		return format("repository.create(\"%s\",factory.configurationItem(\"%s\", %s))", getLabel(), getType(), properties);
	}
}
