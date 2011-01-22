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
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class GenerateDeploymentPackageMojoTest extends AbstractMojoTestCase {

	private GenerateDeploymentPackageMojo mojo;
	private DeployableArtifactItem configurationFiles;
	private DeployableArtifactItem sqlFiles;
	private DeployableArtifactItem warFile;


	public void setUp() throws Exception {
		super.setUp();

		configurationFiles = new DeployableArtifactItem();
		configurationFiles.setType("ConfigurationFiles");
		configurationFiles.setLocation("src/main/resources");


		sqlFiles = new DeployableArtifactItem();
		sqlFiles.setType("SqlFiles");
		sqlFiles.setLocation("src/main/sql");

		warFile = new DeployableArtifactItem();
		warFile.setType("War");
		warFile.setLocation("/tmp/apetwar.war");

		mojo = new GenerateDeploymentPackageMojo();
	}

	@Test
	public void testPackageWar() throws Exception {
		MavenProjectStub project = new MavenProjectStub();
		ArtifactStub mainArtifact = new ArtifactStub();
		mainArtifact.setType("War");
		mainArtifact.setArtifactId("com.test.tomcat");
		mainArtifact.setGroupId("test");
		mainArtifact.setVersion("1.0");
		mainArtifact.setFile(new File("main.war"));
		project.setArtifact(mainArtifact);

		setVariableValueToObject(mojo, "project", project);
		setVariableValueToObject(mojo, "outputDirectory", new File("target/"));
		setVariableValueToObject(mojo, "artifactId", "com.test.tomcat");
		setVariableValueToObject(mojo, "version", "1.0");
		setVariableValueToObject(mojo, "jarArchiver", new JarArchiver());
		setVariableValueToObject(mojo, "generateManifestOnly", true);
		setVariableValueToObject(mojo, "finalName", "test1");

		mojo.execute();
		File manifest = (File) getVariableValueFromObject(mojo, "manifestFile");
		assertDescribeTheSamePackage(mojo.getRealMainDeployableArtifact(), null, manifest);
	}


	@Test
	public void testPackageOneWithConfigurationFiles() throws Exception {
		MavenProjectStub project = new MavenProjectStub();
		ArtifactStub mainArtifact = new ArtifactStub();
		mainArtifact.setType("War");
		mainArtifact.setArtifactId("com.test.tomcat.configurationsfiles");
		mainArtifact.setGroupId("test");
		mainArtifact.setVersion("1.0");
		mainArtifact.setFile(new File("main.war"));
		project.setArtifact(mainArtifact);

		setVariableValueToObject(mojo, "project", project);
		setVariableValueToObject(mojo, "outputDirectory", new File("target/"));
		setVariableValueToObject(mojo, "artifactId", "com.test.tomcat.configurationsfiles");
		final List<DeployableArtifactItem> artifactItems = Collections.singletonList(configurationFiles);
		setVariableValueToObject(mojo, "deployableArtifacts", artifactItems);
		setVariableValueToObject(mojo, "jarArchiver", new JarArchiver());
		setVariableValueToObject(mojo, "version", "1.0");
		setVariableValueToObject(mojo, "generateManifestOnly", true);


		mojo.execute();
		File manifest = (File) getVariableValueFromObject(mojo, "manifestFile");
		assertDescribeTheSamePackage(mojo.getRealMainDeployableArtifact(), artifactItems, manifest);
	}


	@Test
	public void testPackageOneWithConfigurationFilesAndSqlFiles() throws Exception {
		MavenProjectStub project = new MavenProjectStub();
		ArtifactStub mainArtifact = new ArtifactStub();
		mainArtifact.setType("War");
		mainArtifact.setArtifactId("com.test.tomcat.configurationsfiles");
		mainArtifact.setGroupId("test");
		mainArtifact.setVersion("1.0");
		mainArtifact.setFile(new File("main.war"));
		project.setArtifact(mainArtifact);

		List<DeployableArtifactItem> daitem = new ArrayList<DeployableArtifactItem>();
		daitem.add(configurationFiles);
		daitem.add(sqlFiles);

		setVariableValueToObject(mojo, "project", project);
		setVariableValueToObject(mojo, "outputDirectory", new File("target/"));
		setVariableValueToObject(mojo, "artifactId", "com.xebialans.maven.dar");
		setVariableValueToObject(mojo, "deployableArtifacts", daitem);
		setVariableValueToObject(mojo, "jarArchiver", new JarArchiver());
		setVariableValueToObject(mojo, "version", "1.0");
		setVariableValueToObject(mojo, "generateManifestOnly", true);

		mojo.execute();
		File manifest = (File) getVariableValueFromObject(mojo, "manifestFile");
		assertDescribeTheSamePackage(mojo.getRealMainDeployableArtifact(), daitem, manifest);

	}

	@Test
	public void testPackageDarOneWithConfigurationFilesAndSqlFiles() throws Exception {
		MavenProjectStub project = new MavenProjectStub();
		ArtifactStub mainArtifact = new ArtifactStub();
		mainArtifact.setType("dar");
		mainArtifact.setGroupId("com.xebialabs.maven.unit.tests");
		mainArtifact.setVersion("1.0");
		project.setArtifact(mainArtifact);

		List<DeployableArtifactItem> daitem = new ArrayList<DeployableArtifactItem>();
		daitem.add(configurationFiles);
		daitem.add(sqlFiles);
		daitem.add(warFile);

		setVariableValueToObject(mojo, "project", project);
		setVariableValueToObject(mojo, "outputDirectory", new File("target/"));
		setVariableValueToObject(mojo, "artifactId", "monAppDar");
		setVariableValueToObject(mojo, "deployableArtifacts", daitem);
		setVariableValueToObject(mojo, "jarArchiver", new JarArchiver());
		setVariableValueToObject(mojo, "version", "1.0");
		setVariableValueToObject(mojo, "generateManifestOnly", true);

		mojo.execute();
		File manifest = (File) getVariableValueFromObject(mojo, "manifestFile");
		assertDescribeTheSamePackage(null, daitem, manifest);
	}


	private void assertDescribeTheSamePackage(DeployableArtifactItem mainArtifact, List<DeployableArtifactItem> daitem, File manifestFile) throws Exception {
		List<DeployableArtifactItem> all = Lists.newArrayList();
		if (mainArtifact != null)
			all.add(mainArtifact);
		if (daitem != null)
			all.addAll(daitem);
		assertDescribeTheSamePackage(all, manifestFile);
	}

	private void assertDescribeTheSamePackage(List<DeployableArtifactItem> daitem, File manifestFile) throws Exception {
		Manifest manifest = new Manifest(new java.io.FileInputStream(manifestFile));
		dumpManifest(manifest);
		final Map<String, Attributes> entries = manifest.getEntries();
		assertEquals(daitem.size(), entries.size());
		for (DeployableArtifactItem da : daitem) {
			String locationInDar = da.getEntryKey();
			final Attributes attributes = entries.get(locationInDar);
			assertNotNull("Entry not found " + locationInDar, attributes);
		}
	}

	private void dumpManifest(Manifest m) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			m.write(baos);
			baos.close();
		} catch (IOException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
		System.out.println(new String(baos.toByteArray()));
	}


}
