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

/**
 * Clean (Undeploy) the target environment.
 *
 * @author Benoit Moussaud
 * @goal clean
 * @phase post-integration-test
 * @configurator override
 */
public class CleanMojo extends AbstractDeployitMojo {
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("deployit:clean");

		startServer();

		interpret("delete " + DEFAULT_DEPLOYMENT);
		//Go !
		deployit();

		interpret("shutdown");

		getLog().info("end of deployit:clean.");
	}
}
