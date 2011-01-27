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


import org.junit.Test;


public class DeployitMojoTest extends BaseForTestMojo {


	@Test
	public void testOneServerEnvMojo() throws Exception {
		/*
		{
			MavenProjectStub project = new MavenProjectStub();
			ArtifactStub mainArtifact = new ArtifactStub();
			mainArtifact.setType("War");
			mainArtifact.setGroupId("com.test.tomcat");
			mainArtifact.setArtifactId("testApp");
			mainArtifact.setVersion("1.0");
			mainArtifact.setFile(new File("src/test/resources/m2repo/com/xebialabs/deployit/petclinic/petclinic-war/PetClinic/1.0/PetClinic-1.0.war"));
			project.setArtifact(mainArtifact);

			setVariableValueToObject(darMojo, "project", project);
			setVariableValueToObject(darMojo, "outputDirectory", new File("target/"));
			setVariableValueToObject(darMojo, "artifactId", "testApp");
			setVariableValueToObject(darMojo, "version", "1.0");
			setVariableValueToObject(darMojo, "jarArchiver", new JarArchiver());
			setVariableValueToObject(darMojo, "finalName", "test1");
			darMojo.execute();
		}

		{
			MavenProjectStub project = new MavenProjectStub();
			ArtifactStub mainArtifact = new ArtifactStub();
			mainArtifact.setType("War");
			mainArtifact.setGroupId("com.test.tomcat");
			mainArtifact.setArtifactId("testApp");
			mainArtifact.setVersion("1.0");
			mainArtifact.setFile(new File("src/test/resources/m2repo/com/xebialabs/deployit/petclinic/petclinic-war/PetClinic/1.0/PetClinic-1.0.war"));
			project.setArtifact(mainArtifact);

			List<ConfigurationItem> env = Lists.newArrayList(host, tomcatServer);

			setVariableValueToObject(deployMojo, "testmode", true);
			setVariableValueToObject(deployMojo, "environment", env);
			setVariableValueToObject(deployMojo, "project", project);
			setVariableValueToObject(deployMojo, "artifactId", "testApp");
			setVariableValueToObject(deployMojo, "version", "1.0");
			deployMojo.execute();
		}
		*/
		System.out.println("Nop");

	}


}
