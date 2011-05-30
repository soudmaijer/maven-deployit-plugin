package com.xebialabs.deployit.maven;

import com.google.common.collect.Lists;
import com.xebialabs.deployit.core.api.dto.RepositoryObject;
import org.junit.Test;

import java.util.Collection;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 */
public class MappingItemTest {


	@Test
	public void testSimpleConvert() throws Exception {

		MappingItem item = new MappingItem();
		item.setSource("Applications/deployit-petclinic/1.0/config");
		item.setTarget("Infrastructure/tomcat6.vm/tomcat6-1");
		item.setType("com.xebialabs.deployit.plugin.tomcat.ci.TomcatWarMapping");

		final RepositoryObject repositoryObject = item.toRepositoryObject();

		assertEquals("Applications/deployit-petclinic/1.0/config", repositoryObject.getValues().get("source").toString());
		assertEquals("Infrastructure/tomcat6.vm/tomcat6-1", repositoryObject.getValues().get("target").toString());
		assertEquals("Infrastructure/tomcat6.vm/tomcat6-1/deployit-petclinic", repositoryObject.getId());


	}

	@Test
	public void testConvertWithCollection() throws Exception {

		MappingItem item = new MappingItem();
		item.setSource("Applications/deployit-petclinic/1.0-20110530-105612/LoadBalancingConfigurationResource");
		item.setTarget("Infrastructure/apache2.vm/Apache2WebServer");
		item.setType("com.xebialabs.deployit.plugin.apache.modjk.ci.ModJkApacheModuleConfigurationMapping");

		item.addParameter("targets",Lists.newArrayList("Infrastructure/tomcat6.vm/tomcat6-1","Infrastructure/tomcat6.vm/tomcat6-2"));
		final RepositoryObject repositoryObject = item.toRepositoryObject();

		assertEquals("Applications/deployit-petclinic/1.0-20110530-105612/LoadBalancingConfigurationResource", repositoryObject.getValues().get("source").toString());
		assertEquals("Infrastructure/apache2.vm/Apache2WebServer", repositoryObject.getValues().get("target").toString());
		assertEquals("Infrastructure/apache2.vm/Apache2WebServer/deployit-petclinic", repositoryObject.getId());
		final Object targets = repositoryObject.getValues().get("targets");
		assertTrue(targets instanceof Collection);


	}
}
