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
import org.apache.commons.io.IOUtils;
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

	private MiddlewareResource mrDataSource;
	private MiddlewareResource mrModjk;


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

		mrDataSource = new MiddlewareResource();
		mrDataSource.setName("petclinicDS");
		mrDataSource.setType("DummyDataSource");
		mrDataSource.addParameter("driver","com.mysql.jdbc.Driver");
		mrDataSource.addParameter("url","jdbc:mysql://localhost/petclinic");
		mrDataSource.addParameter("username","petclinic");
		mrDataSource.addParameter("password","secr$t");
		mrDataSource.addParameter("settings-EntryKey-1","autoCommit");
		mrDataSource.addParameter("settings-EntryValue-1","true");

		mrModjk = new MiddlewareResource();
		mrModjk.setName("AnModJkConfiguration");
		mrModjk.setType("ModJkApacheModuleConfiguration");
		mrModjk.addParameter("urlMounts-EntryPrefix-1","/foo");
		mrModjk.addParameter("jkstatus","true");

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
		assertDescribeTheSamePackage(mojo.getRealMainDeployableArtifact(), null, null, manifest);
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

		final List<DeployableArtifactItem> artifactItems = Collections.singletonList(configurationFiles);

		setVariableValueToObject(mojo, "project", project);
		setVariableValueToObject(mojo, "outputDirectory", new File("target/"));
		setVariableValueToObject(mojo, "artifactId", "com.test.tomcat.configurationsfiles");
		setVariableValueToObject(mojo, "deployableArtifacts", artifactItems);
		setVariableValueToObject(mojo, "jarArchiver", new JarArchiver());
		setVariableValueToObject(mojo, "version", "1.0");
		setVariableValueToObject(mojo, "generateManifestOnly", true);


		mojo.execute();
		File manifest = (File) getVariableValueFromObject(mojo, "manifestFile");
		assertDescribeTheSamePackage(mojo.getRealMainDeployableArtifact(), artifactItems,null,  manifest);
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

		List<DeployableArtifactItem> deployableArtifactItems = Lists.newArrayList(configurationFiles, sqlFiles);

		setVariableValueToObject(mojo, "project", project);
		setVariableValueToObject(mojo, "outputDirectory", new File("target/"));
		setVariableValueToObject(mojo, "artifactId", "com.xebialans.maven.dar");
		setVariableValueToObject(mojo, "deployableArtifacts", deployableArtifactItems);
		setVariableValueToObject(mojo, "jarArchiver", new JarArchiver());
		setVariableValueToObject(mojo, "version", "1.0");
		setVariableValueToObject(mojo, "generateManifestOnly", true);

		mojo.execute();
		File manifest = (File) getVariableValueFromObject(mojo, "manifestFile");
		assertDescribeTheSamePackage(mojo.getRealMainDeployableArtifact(), deployableArtifactItems, null, manifest);

	}

	@Test
	public void testPackageDarOneWithConfigurationFilesAndSqlFiles() throws Exception {
		MavenProjectStub project = new MavenProjectStub();
		ArtifactStub mainArtifact = new ArtifactStub();
		mainArtifact.setType("dar");
		mainArtifact.setGroupId("com.xebialabs.maven.unit.tests");
		mainArtifact.setVersion("1.0");
		project.setArtifact(mainArtifact);

		List<DeployableArtifactItem> daitem = Lists.newArrayList(configurationFiles,sqlFiles, warFile);

		setVariableValueToObject(mojo, "project", project);
		setVariableValueToObject(mojo, "outputDirectory", new File("target/"));
		setVariableValueToObject(mojo, "artifactId", "monAppDar");
		setVariableValueToObject(mojo, "deployableArtifacts", daitem);
		setVariableValueToObject(mojo, "jarArchiver", new JarArchiver());
		setVariableValueToObject(mojo, "version", "1.0");
		setVariableValueToObject(mojo, "generateManifestOnly", true);

		mojo.execute();
		File manifest = (File) getVariableValueFromObject(mojo, "manifestFile");
		assertDescribeTheSamePackage(null, daitem, null, manifest);
	}

	@Test
	public void testPackageDarOneWithConfigurationFilesAndSqlFilesAndADataSource() throws Exception {
		MavenProjectStub project = new MavenProjectStub();
		ArtifactStub mainArtifact = new ArtifactStub();
		mainArtifact.setType("dar");
		mainArtifact.setGroupId("com.xebialabs.maven.unit.tests");
		mainArtifact.setVersion("1.0");
		project.setArtifact(mainArtifact);

		List<DeployableArtifactItem> deployableArtifactItems = new ArrayList<DeployableArtifactItem>();
		deployableArtifactItems.add(configurationFiles);
		deployableArtifactItems.add(sqlFiles);
		deployableArtifactItems.add(warFile);

		List<MiddlewareResource> mrs = Lists.newArrayList(mrDataSource);

		setVariableValueToObject(mojo, "project", project);
		setVariableValueToObject(mojo, "outputDirectory", new File("target/"));
		setVariableValueToObject(mojo, "artifactId", "monAppDar");
		setVariableValueToObject(mojo, "deployableArtifacts", deployableArtifactItems);
		setVariableValueToObject(mojo, "middlewareResources", mrs);
		setVariableValueToObject(mojo, "jarArchiver", new JarArchiver());
		setVariableValueToObject(mojo, "version", "1.0");
		setVariableValueToObject(mojo, "generateManifestOnly", true);

		mojo.execute();
		File manifest = (File) getVariableValueFromObject(mojo, "manifestFile");
		assertDescribeTheSamePackage(null, deployableArtifactItems, mrs, manifest);
	}

	@Test
	public void testPackageDarOneWithConfigurationFilesAndSqlFilesAndTwoMiddlewareResources() throws Exception {
		MavenProjectStub project = new MavenProjectStub();
		ArtifactStub mainArtifact = new ArtifactStub();
		mainArtifact.setType("dar");
		mainArtifact.setGroupId("com.xebialabs.maven.unit.tests");
		mainArtifact.setVersion("1.0");
		project.setArtifact(mainArtifact);

		List<DeployableArtifactItem> deployableArtifactItems = Lists.newArrayList(configurationFiles,sqlFiles, warFile);
		List<MiddlewareResource> mrs = Lists.newArrayList(mrDataSource,mrModjk);

		setVariableValueToObject(mojo, "project", project);
		setVariableValueToObject(mojo, "outputDirectory", new File("target/"));
		setVariableValueToObject(mojo, "artifactId", "monAppDar");
		setVariableValueToObject(mojo, "deployableArtifacts", deployableArtifactItems);
		setVariableValueToObject(mojo, "middlewareResources", mrs);
		setVariableValueToObject(mojo, "jarArchiver", new JarArchiver());
		setVariableValueToObject(mojo, "version", "1.0");
		setVariableValueToObject(mojo, "generateManifestOnly", true);

		mojo.execute();
		File manifest = (File) getVariableValueFromObject(mojo, "manifestFile");
		assertDescribeTheSamePackage(null, deployableArtifactItems, mrs, manifest);
	}


	private void assertDescribeTheSamePackage(DeployableArtifactItem mainArtifact, List<DeployableArtifactItem> daitem, List<MiddlewareResource> mr,File manifestFile) throws Exception {
		List<PackagedItem> all = Lists.newArrayList();
		if (mainArtifact != null)
			all.add(mainArtifact);
		if (daitem != null)
			all.addAll(daitem);
		if (mr !=null)
			all.addAll(mr);

		assertDescribeTheSamePackage(all, manifestFile);
	}

	private void assertDescribeTheSamePackage(List<PackagedItem> daitem, File manifestFile) throws Exception {
		Manifest manifest = new Manifest(new java.io.FileInputStream(manifestFile));
		dumpManifest(manifest);
		final Map<String, Attributes> entries = manifest.getEntries();
		assertEquals(daitem.size(), entries.size());
		for (PackagedItem packagedItem : daitem) {
			String locationInDar = packagedItem.getEntryKey();
			final Attributes attributes = entries.get(locationInDar);
			assertNotNull("Entry not found " + locationInDar, attributes);
			assertEquals(packagedItem.getType(), attributes.getValue("CI-Type"));
		}
	}

	private void dumpManifest(Manifest m) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			m.write(baos);
		} finally {
			IOUtils.closeQuietly(baos);
		}

		baos.close();
		System.out.println(new String(baos.toByteArray()));
	}


}
