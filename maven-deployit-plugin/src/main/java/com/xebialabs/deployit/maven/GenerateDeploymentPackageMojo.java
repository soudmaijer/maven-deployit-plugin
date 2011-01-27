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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

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

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Build the Deployit Deployment Package (DAR)");
		getLog().info("Generate Deployment Package...");
		getPackager().perform();
		getPackager().seal();
	}


	/**
	 * For Tests
	 *
	 * @return
	 * @throws MojoExecutionException
	 */
	public DeployableArtifactItem getRealMainDeployableArtifact() throws MojoExecutionException {
		if (packager == null)
			throw new MojoExecutionException("packager must be not null here");
		return packager.getRealDeployableArtifact(project.getArtifact());
	}

	/**
	 * For Tests
	 *
	 * @return
	 * @throws MojoExecutionException
	 */
	public File getManifestFile() throws MojoExecutionException {
		if (packager == null)
			throw new MojoExecutionException("packager must be not null here");

		return packager.getManifestFile();
	}
}
