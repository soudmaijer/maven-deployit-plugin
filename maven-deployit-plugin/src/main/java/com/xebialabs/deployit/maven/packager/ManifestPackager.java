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

package com.xebialabs.deployit.maven.packager;

import com.xebialabs.deployit.maven.DeployableArtifactItem;
import com.xebialabs.deployit.maven.MiddlewareResource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.JarArchiver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ManifestPackager {

	public static final String DEPLOYMENT_PACKAGE_DIR = "deployment-package";

	private final File targetDirectory;

	private final Manifest manifest = new Manifest();

	private boolean generateManifestOnly = false;
	private Log log;
	private final File outputDirectory;
	private MavenProject project;

	private boolean timestampedVersion;

	static final String APPLICATION = "CI-Application";
	static final String VERSION = "CI-Version";

	public ManifestPackager(MavenProject project) {
		this.project = project;
		this.outputDirectory = new File(project.getBuild().getDirectory());
		this.targetDirectory = new File(outputDirectory, DEPLOYMENT_PACKAGE_DIR + File.separator + project.getArtifactId() + File.separator + project.getVersion());
		this.targetDirectory.mkdirs();
	}


	public File getTargetDirectory() {
		return targetDirectory;
	}


	public void perform() {
		final Attributes mainAttributes = manifest.getMainAttributes();
		mainAttributes.putValue("Manifest-Version", "1.0");
		mainAttributes.putValue("Deployit-Package-Format-Version", "1.2");
		mainAttributes.putValue(APPLICATION, project.getArtifactId());
		final String pomVersion = project.getVersion();

		String darVersion = pomVersion;
		if (timestampedVersion) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
			darVersion = pomVersion + "-" + dateFormat.format(System.currentTimeMillis());
		}

		mainAttributes.putValue(VERSION, darVersion);

		final File meta_inf = new File(targetDirectory, "META-INF");
		meta_inf.mkdirs();
		File manifestFile = new File(meta_inf, "MANIFEST.MF");
		getLog().info("Generate manifest file " + manifestFile.getAbsolutePath());
		FileOutputStream fos = null;
		try {
			dumpManifest();
			fos = new FileOutputStream(manifestFile);
			manifest.write(fos);
		} catch (IOException e) {
			new RuntimeException("generation of the manifest file failed", e);
		} finally {
			IOUtils.closeQuietly(fos);
		}
	}

	private void dumpManifest() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			manifest.write(baos);
		} finally {
			IOUtils.closeQuietly(baos);
		}
		getLog().debug(new String(baos.toByteArray()));
	}

	protected void addDeployableArtifact(DeployableArtifactItem item) {
		getLog().info(" add deployable artifact : " + item);
		if ("Dar".equalsIgnoreCase(item.getType()))
			return;

		if ("Pom".equalsIgnoreCase(item.getType()))
			return;

		final Map<String, Attributes> entries = manifest.getEntries();
		final Attributes attributes = new Attributes();
		final File location = new File(item.getFileSystemLocation());

		attributes.putValue("CI-Type", item.getType());
		if (item.hasName())
			attributes.putValue("CI-Name", item.getName());

		entries.put(item.getEntryKey(), attributes);

		final File targetDir = new File(targetDirectory, item.getDarLocation());
		if (generateManifestOnly) {
			getLog().info("Skip copying artifact " + item.getName() + " to " + targetDir);
			return;
		}
		targetDir.mkdirs();

		File locationTargetDirs;
		//do not create missing directories is there are no parents or if the file is absolute
		if (location.isAbsolute() || location.getParent() == null) {
			locationTargetDirs = targetDir;
		} else {
			locationTargetDirs = new File(targetDir, location.getParent());
			locationTargetDirs.mkdirs();
		}


		try {
			if (location.isDirectory()) {
				FileUtils.copyDirectoryToDirectory(location, locationTargetDirs);
			} else {
				FileUtils.copyFileToDirectory(location, locationTargetDirs);
			}
		} catch (IOException e) {
			throw new RuntimeException("Fail to copy of " + location + " to " + targetDir, e);

		}

	}

	protected void addMiddlewareResource(MiddlewareResource mr) {
		getLog().info(" add mddleware resource : " + mr.getConfigurationName());
		final Map<String, Attributes> entries = manifest.getEntries();
		final Attributes attributes = new Attributes();
		attributes.putValue("CI-Type", mr.getType());
		for (Map.Entry<String, String> entry : mr.getProperties().entrySet()) {
			attributes.putValue("CI-" + entry.getKey(), entry.getValue());
		}
		entries.put(mr.getEntryKey(), attributes);
	}

	public boolean isGenerateManifestOnly() {
		return generateManifestOnly;
	}

	public void setGenerateManifestOnly(boolean generateManifestOnly) {
		this.generateManifestOnly = generateManifestOnly;
	}

	public File getManifestFile() {
		return new File(targetDirectory, "META-INF/MANIFEST.MF");
	}

	public void setLog(Log log) {
		this.log = log;
	}

	private Log getLog() {
		return log;
	}


	protected DeployableArtifactItem getRealDeployableArtifact(final DeployableArtifactItem item) {
		if (!isMavenDependency(item)) {
			getLog().info(" add a deployable artifact " + item);
			String relativeLocation = item.getLocation();
			File fileSysLoca = new File(project.getBasedir(), relativeLocation);
			getLog().debug("  filesystem location is " + fileSysLoca.getPath());
			item.setFileSystemLocation(fileSysLoca.getPath());
			return item;
		}

		getLog().info(" add a maven deployable artifact " + item);
		getLog().debug("-translateIntoPath- " + item.getLocation());
		String key = item.getLocation();
		Artifact artifact = (Artifact) project.getArtifactMap().get(key);
		if (artifact == null)
			getLog().debug("Not found, search in the dependency artifacts...");
		for (Object o : project.getDependencyArtifacts()) {
			Artifact da = (Artifact) o;
			final String artifactKey = da.getGroupId() + ":" + da.getArtifactId();
			if (artifactKey.equals(key)) {
				artifact = da;
			}
		}
		if (artifact == null) {
			throw new IllegalStateException(
					"The artifact "
							+ key
							+ " referenced in plugin as is not found the project dependencies");
		}

		DeployableArtifactItem mavenDeployableArtifact = new DeployableArtifactItem();
		final String artifactFile = artifact.getFile().toString();
		mavenDeployableArtifact.setLocation(artifactFile);
		mavenDeployableArtifact.setFileSystemLocation(artifactFile);
		if (item.hasName())
			mavenDeployableArtifact.setName(item.getName());
		else
			mavenDeployableArtifact.setName(artifact.getArtifactId());
		mavenDeployableArtifact.setType(item.getType());
		mavenDeployableArtifact.setDarLocation(item.getDarLocation());
		mavenDeployableArtifact.setFolder(item.isFolder());
		return mavenDeployableArtifact;

	}

	/**
	 * TODO: define markups to handle location or dependency
	 * item.getLocation().indexOf(':') > 1 fix absolute location on Windows Plateforrm
	 * assuming no groupId contains only one character !
	 */
	private boolean isMavenDependency(DeployableArtifactItem item) {
		return item.getLocation().contains(":") && item.getLocation().indexOf(':') > 1;
	}

	public DeployableArtifactItem getRealDeployableArtifact(final Artifact artifact) {
		DeployableArtifactItem mavenDeployableArtifact = new DeployableArtifactItem();
		mavenDeployableArtifact.setName(artifact.getArtifactId());
		mavenDeployableArtifact.setType(capitalize(artifact.getType()));

		final File file = artifact.getFile();
		if (file != null) {
			mavenDeployableArtifact.setFileSystemLocation(file.toString());
			mavenDeployableArtifact.setLocation(file.toString());
		}
		return mavenDeployableArtifact;
	}


	private String capitalize(String inputWord) {
		String firstLetter = inputWord.substring(0, 1);  // Get first letter
		String remainder = inputWord.substring(1);    // Get remainder of word.
		String capitalized = firstLetter.toUpperCase() + remainder.toLowerCase();
		return capitalized;

	}


	public void addDeployableArtifact(Artifact artifact) {
		addDeployableArtifact(getRealDeployableArtifact(artifact));
	}

	public void addDeployableArtifacts(List<DeployableArtifactItem> deployableArtifacts) {
		if (deployableArtifacts == null) {
			return;
		}

		getLog().info("Add the artifacts");
		for (DeployableArtifactItem item : deployableArtifacts) {
			addDeployableArtifact(getRealDeployableArtifact(item));
		}
	}

	public void addMiddlewareResources(List<MiddlewareResource> middlewareResources) {
		if (middlewareResources == null) {
			return;
		}

		getLog().info("Add the middleware resources");
		for (MiddlewareResource mr : middlewareResources) {
			addMiddlewareResource(mr);
		}
	}

	public void seal() throws MojoExecutionException {
		if (generateManifestOnly) {
			getLog().info("Do not seal the dar file, return now");
			return;
		}

		try {
			File darFile = getDarFile();
			getLog().info("Seal the archive in " + darFile);

			final MavenArchiver mvnArchiver = new MavenArchiver();
			mvnArchiver.setArchiver(new JarArchiver());
			mvnArchiver.setOutputFile(darFile);

			mvnArchiver.getArchiver().addDirectory(getTargetDirectory());

			final File manifestFile = getManifestFile();
			getLog().debug("set Manifest file of the archive " + manifestFile);
			mvnArchiver.getArchiver().setManifest(manifestFile);

			mvnArchiver.createArchive(project, new MavenArchiveConfiguration());

			project.getArtifact().setFile(darFile);

		} catch (Exception e) {
			throw new MojoExecutionException("Error assembling DAR", e);
		}
	}

	public File getDarFile() {
		return new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".dar");
	}


	public void setTimestampedVersion(boolean timestampedVersion) {
		this.timestampedVersion = timestampedVersion;
	}


}
