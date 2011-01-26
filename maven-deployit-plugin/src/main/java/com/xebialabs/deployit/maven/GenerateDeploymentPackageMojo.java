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

import com.xebialabs.deployit.maven.packager.ManifestPackager;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.jar.JarArchiver;

import java.io.File;

/**
 * Build up the Deployit Deployment Package
 *
 * @author Benoit Moussaud
 * @phase package
 * @goal generate-deployment-package
 * @requiresDependencyResolution compile
 * @configurator override
 */
public class GenerateDeploymentPackageMojo extends AbstractDeployitMojo {

	/**
	 * The Jar archiver.
	 *
	 * @component role="org.codehaus.plexus.archiver.Archiver" role-hint="jar"
	 */
	private JarArchiver jarArchiver;


	/**
	 * @component
	 */
	private MavenProjectHelper projectHelper;

	private File manifestFile;

	public void execute() throws MojoExecutionException, MojoFailureException {

		getLog().info("Build the Deployit Deployment Package (DAR)");

		ManifestPackager packager = new ManifestPackager(artifactId, version, outputDirectory);
		packager.setLogger(getLog());

		packager.setGenerateManifestOnly(generateManifestOnly);
		getLog().info("Generate Deployment Package...");


		packager.addDeployableArtifact(getRealDeployableArtifact(project.getArtifact()));
		//Handle additionnal maven artifacts
		if (deployableArtifacts != null) {
			getLog().info("Add the artifacts");
			for (DeployableArtifactItem item : deployableArtifacts) {
				packager.addDeployableArtifact(getRealDeployableArtifact(item));
			}
		}

		//Handle the middleware resources
		if (middlewareResources != null) {
			getLog().info("Add the middleware resources");
			for (MiddlewareResource mr : middlewareResources) {
				packager.addMiddlewareResource(mr);
			}
		}

		packager.perform();

		manifestFile = packager.getManifestFile();
		getLog().info("Manifest file generated in " + manifestFile);

		if (generateManifestOnly) {
			getLog().info("Do not seal the dar file, return now");
			return;
		}

		try {

			File darFile = getDarFile();
			getLog().info("Seal the archive in " + darFile);

			final MavenArchiver mvnArchiver = new MavenArchiver();
			getLog().debug("Jar archiver implementation[" + jarArchiver.getClass().getName() + "]");
			mvnArchiver.setArchiver(jarArchiver);
			mvnArchiver.setOutputFile(darFile);

			mvnArchiver.getArchiver().addDirectory(packager.getTargetDirectory());

			final File manifestFile = packager.getManifestFile();
			getLog().debug("set Manifest file of the archive " + manifestFile);
			mvnArchiver.getArchiver().setManifest(manifestFile);

			mvnArchiver.createArchive(getProject(), new  MavenArchiveConfiguration());

			if (classifier != null) {
				projectHelper.attachArtifact(getProject(), "dar", classifier, darFile);
			} else {
				getProject().getArtifact().setFile(darFile);
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Error assembling DAR", e);
		}
	}


	public DeployableArtifactItem getRealMainDeployableArtifact() throws MojoExecutionException {
		return getRealDeployableArtifact(project.getArtifact());
	}
}
