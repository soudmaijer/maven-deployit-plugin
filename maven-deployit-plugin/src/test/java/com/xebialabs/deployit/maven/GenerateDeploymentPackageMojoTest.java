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
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class GenerateDeploymentPackageMojoTest extends BaseForTestMojo {

	@Test
	public void testPackageWar() throws Exception {
		List<DeployableArtifactItem> deployableArtifactItems = Lists.newArrayList();
		List<MiddlewareResource> mrs = Lists.newArrayList();
		final String artifactID = "testPackageWar";
		performTestAndAssert(artifactID, "war", deployableArtifactItems, mrs);
	}

	@Test
	public void testPackageWarSTimeStampedVersion() throws Exception {
		List<DeployableArtifactItem> deployableArtifactItems = Lists.newArrayList();
		List<MiddlewareResource> mrs = Lists.newArrayList();
		final String artifactID = "testPackageWar";
		setVariableValueToObject(darMojo, "timestampedVersion", true);
		performTestAndAssert(artifactID, "war", deployableArtifactItems, mrs,"123.4");
	}


	@Test
	public void testPackageOneWithConfigurationFiles() throws Exception {
		List<DeployableArtifactItem> deployableArtifactItems = Collections.singletonList(configurationFiles);
		List<MiddlewareResource> mrs = Lists.newArrayList();
		final String artifactID = "testPackageOneWithConfigurationFiles";
		performTestAndAssert(artifactID, "war", deployableArtifactItems, mrs);
	}

	@Test
	public void testPackageOneWithConfigurationFilesOnWindows() throws Exception {
		configurationFiles.setLocation("C:/too");
		List<DeployableArtifactItem> deployableArtifactItems = Collections.singletonList(configurationFiles);
		List<MiddlewareResource> mrs = Lists.newArrayList();
		final String artifactID = "testPackageOneWithConfigurationFilesOnWindows";
		performTestAndAssert(artifactID, "war", deployableArtifactItems, mrs);
	}


	@Test
	public void testPackageOneWithConfigurationFilesAndSqlFiles() throws Exception {
		List<DeployableArtifactItem> deployableArtifactItems = Lists.newArrayList(configurationFiles, sqlFiles);
		List<MiddlewareResource> mrs = Lists.newArrayList();
		final String artifactID = "testPackageOneWithConfigurationFilesAndSqlFiles";
		performTestAndAssert(artifactID, "War", deployableArtifactItems, mrs);

	}

	@Test
	public void testPackageDarOneWithConfigurationFilesAndSqlFiles() throws Exception {
		List<DeployableArtifactItem> deployableArtifactItems = Lists.newArrayList(configurationFiles, sqlFiles, warFile);
		List<MiddlewareResource> mrs = Lists.newArrayList();
		final String artifactID = "testPackageDarOneWithConfigurationFilesAndSqlFiles";
		performTestAndAssert(artifactID, "dar", deployableArtifactItems, mrs);
	}

	@Test
	public void testPackageDarOneWithConfigurationFilesAndSqlFilesAndADataSource() throws Exception {
		List<DeployableArtifactItem> deployableArtifactItems = Lists.newArrayList(configurationFiles, sqlFiles, warFile);
		List<MiddlewareResource> mrs = Lists.newArrayList(mrDataSource);
		final String artifactID = "testPackageDarOneWithConfigurationFilesAndSqlFilesAndADataSource";
		performTestAndAssert(artifactID, "dar", deployableArtifactItems, mrs);
	}

	@Test
	public void testPackageDarOneWithConfigurationFilesAndSqlFilesAndTwoMiddlewareResources() throws Exception {
		List<DeployableArtifactItem> deployableArtifactItems = Lists.newArrayList(configurationFiles, sqlFiles, warFile);
		List<MiddlewareResource> mrs = Lists.newArrayList(mrDataSource, mrModjk);
		final String artifactID = "testPackageDarOneWithConfigurationFilesAndSqlFilesAndTwoMiddlewareResources";
		performTestAndAssert(artifactID, "dar", deployableArtifactItems, mrs);
	}

	@Test
	public void testPackageDarOneWithConfigurationFilesAndSqlFilesAndTwoMiddlewareResourcesTimestamped() throws Exception {
		List<DeployableArtifactItem> deployableArtifactItems = Lists.newArrayList(configurationFiles, sqlFiles, warFile);
		List<MiddlewareResource> mrs = Lists.newArrayList(mrDataSource, mrModjk);
		final String artifactID = "testPackageDarOneWithConfigurationFilesAndSqlFilesAndTwoMiddlewareResources";
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		setVariableValueToObject(darMojo, "timestampedVersion", true);

		final String darVersion = "12.2";
		performTestAndAssert(artifactID, "dar", deployableArtifactItems, mrs, darVersion);
		final String now = dateFormat.format(System.currentTimeMillis());

		final String version = getCIVersionFromManifest();
		final String darVersionNow = darVersion + "-" + now;
		assertTrue(version + " doest not contains " + darVersion + "-" + now, version.startsWith(darVersionNow));

	}

	private String getCIVersionFromManifest() throws IOException, MojoExecutionException {
		Manifest manifest = new Manifest(new java.io.FileInputStream(darMojo.getManifestFile()));
		final Attributes mainAttributes = manifest.getMainAttributes();
		return mainAttributes.getValue("CI-Version");
	}

	@Test
	public void testPackageDarOneWithConfigurationFilesAndSqlFilesAndTwoMiddlewareResourcesImplicitTimestampedTrueWithSnapShot() throws Exception {
		List<DeployableArtifactItem> deployableArtifactItems = Lists.newArrayList(configurationFiles, sqlFiles, warFile);
		List<MiddlewareResource> mrs = Lists.newArrayList(mrDataSource, mrModjk);
		final String artifactID = "testPackageDarOneWithConfigurationFilesAndSqlFilesAndTwoMiddlewareResources";
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

		final String darVersion = "2.4.5-SNAPSHOT";
		performTestAndAssert(artifactID, "dar", deployableArtifactItems, mrs, darVersion);
		final String now = dateFormat.format(System.currentTimeMillis());

		final String version = getCIVersionFromManifest();
		final String darVersionNow = "2.4.5" + "-" + now;
		assertTrue(version + " doest not contains " + darVersion + "-" + now, version.startsWith(darVersionNow));

	}


	private void assertDescribeTheSamePackage(DeployableArtifactItem mainArtifact, List<DeployableArtifactItem> daitem, List<MiddlewareResource> mr, File manifestFile) throws Exception {
		List<PackagedItem> all = Lists.newArrayList();
		if (mainArtifact != null && !mainArtifact.getType().equalsIgnoreCase("dar"))
			all.add(mainArtifact);
		if (daitem != null)
			all.addAll(daitem);
		if (mr != null)
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

	private void performTestAndAssert(String artifactID, String type, List<DeployableArtifactItem> deployableArtifactItems, List<MiddlewareResource> mrs) throws Exception {
		Random r = new Random();
		final String version = "" + r.nextInt(10000);
		performTestAndAssert(artifactID, type, deployableArtifactItems, mrs, version);
		final String manifestVersion = getCIVersionFromManifest();
		assertEquals(version, manifestVersion);
	}

	private void performTestAndAssert(String artifactID, String type, List<DeployableArtifactItem> deployableArtifactItems, List<MiddlewareResource> mrs, String version) throws Exception {
		MavenProjectStub project = new MavenProjectStub() {
			private Build build;

			@Override
			public void setBuild(Build build) {
				this.build = build;
			}

			@Override
			public Build getBuild() {
				return build;
			}
		};

		ArtifactStub mainArtifact = new ArtifactStub();
		mainArtifact.setType(type);
		mainArtifact.setArtifactId("com.test.tomcat");
		mainArtifact.setGroupId("test");
		mainArtifact.setVersion("1.0");
		mainArtifact.setFile(new File("main.war"));
		project.setArtifact(mainArtifact);
		project.setBuild(new Build());
		project.setGroupId("com.xebialabs.unit.tests");
		project.setArtifactId(artifactID);
		project.setVersion(version);

		project.getBuild().setDirectory(new File("target/").getPath());
		project.getBuild().setOutputDirectory(new File("target/classes").getPath());
		project.getBuild().setFinalName(project.getArtifactId());

		setVariableValueToObject(darMojo, "project", project);
		setVariableValueToObject(darMojo, "generateManifestOnly", true);
		setVariableValueToObject(darMojo, "deployableArtifacts", deployableArtifactItems);
		setVariableValueToObject(darMojo, "middlewareResources", mrs);


		darMojo.execute();
		assertDescribeTheSamePackage(darMojo.getRealMainDeployableArtifact(), deployableArtifactItems, mrs, darMojo.getManifestFile());
	}


}
