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

import com.xebialabs.deployit.DeployItConfiguration;
import com.xebialabs.deployit.DeployitOptions;
import com.xebialabs.deployit.Server;
import com.xebialabs.deployit.core.api.dto.RepositoryObject;
import com.xebialabs.deployit.jcr.JackrabbitRepositoryFactoryBean;
import com.xebialabs.deployit.maven.packager.ManifestPackager;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * Provides common code for deployit mojos
 *
 * @author Benoit Moussaud
 */
public abstract class AbstractDeployitMojo extends AbstractMojo {
	/**
	 * The maven project.
	 *
	 * @parameter expression="${project}"
	 * @required @readonly
	 */
	protected MavenProject project;


	/**
	 * @parameter default-value="${project.build.directory}"
	 * @required
	 * @readonly
	 */
	protected File outputDirectory;

	/**
	 * @parameter default-value="${project.artifactId}"
	 * @required
	 * @readonly
	 */
	protected String artifactId;


	/**
	 * Activate the test mode, the steps are not executed.
	 *
	 * @parameter default-value=false
	 */
	protected boolean testmode;


	/**
	 * Deployit Listen port
	 *
	 * @parameter default-value="4516" expression="${deployit.port}"
	 */
	private int port;

	/**
	 * Additional resources such as Database, Apache plugin configuration, JMS Queues...
	 *
	 * @parameter
	 */
	protected List<MiddlewareResource> middlewareResources;


	/**
	 * List of the Mapping
	 *
	 * @parameter
	 */
	protected List<MappingItem> mappings;

	/**
	 * List of ConfigurationItem in the target environment.
	 *
	 * @parameter
	 */
	protected List<ConfigurationItem> environment;


	/**
	 * Additional deployables artifacts
	 *
	 * @parameter
	 */
	protected List<DeployableArtifactItem> deployableArtifacts;

	/**
	 * Only the Manifest file will be generate. Do not copy files when generating Deployment package
	 *
	 * @parameter default-value=false
	 */
	protected boolean generateManifestOnly;

	/**
	 * Perform a skipped deployment before clean it.
	 *
	 * @parameter default-value=false
	 */
	protected boolean forcedClean;


	protected ManifestPackager packager;

	protected MavenCli client;

	public static final String DEFAULT_ENVIRONMENT = "Environments/DefaultEnvironment";

	public static final String DEFAULT_DEPLOYMENT = "DefaultDeployment";

	private static boolean SERVER_STARTED = false;

	protected void startServer() {
		//TODO: too naive....imagine the maven plugin runs in an hudson or bamboo...
		if (!SERVER_STARTED) {
			new File("recovery.dat").delete();
			new File("repository").delete();

			getLog().info("STARTING DEPLOYIT SERVER");
			DeployItConfiguration context = new DeployItConfiguration();

			File repositoryHomeDir = new File("repository");
			context.setJcrRepositoryPath(repositoryHomeDir.getPath());
			context.setHttpPort(getPort());
			context.setImportablePackagesPath(new File(outputDirectory, ManifestPackager.DEPLOYMENT_PACKAGE_DIR).getPath());
			context.setMinThreads(3);
			context.setMaxThreads(24);
			context.setSsl(false);


			//context.setHttpServerName("localhost");

			context.save();
			try {
				setupJcrRepository(repositoryHomeDir);
			} catch (IOException e) {
				e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			} catch (RepositoryException e) {
				e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			}

			final Server server = new Server(context, new DeployitOptions());
			server.start();

			getLog().info("STARTED DEPLOYIT SERVER");
			SERVER_STARTED = true;
		}
	}

	private static void setupJcrRepository(File repositoryHomeDir) throws IOException, RepositoryException {

		FileUtils.deleteDirectory(repositoryHomeDir);
		String homeDirAbsolutePath = repositoryHomeDir.getAbsolutePath();

		JackrabbitRepositoryFactoryBean repositoryFactoryBean = new JackrabbitRepositoryFactoryBean();
		repositoryFactoryBean.setConfiguration(new ClassPathResource("jackrabbit-repository.xml"));
		repositoryFactoryBean.setHomeDir(new FileSystemResource(homeDirAbsolutePath));

		repositoryFactoryBean.setCreateHomeDirIfNotExists(true);
		repositoryFactoryBean.afterPropertiesSet();
		repositoryFactoryBean.configureJcrRepositoryForDeployit();
		repositoryFactoryBean.destroy();
	}

	public static void stopServer() {
		Server.requestShutdown();
		SERVER_STARTED = false;
	}

	protected MavenCli getClient() throws MojoExecutionException {
		if (client == null) {
			client = new MavenCli(getPort());
			client.setLogger(getLog());
			client.setSkipStepsMode(testmode);
		}
		return client;
	}

	private int getPort() {
		return port;
	}

	protected void initialDeployment() throws MojoExecutionException {
		if (environment == null)
			throw new MojoExecutionException("Environment is empty");

		final File darFile = getPackager().getDarFile();
		if (!darFile.exists())
			throw new MojoExecutionException("Dar file does not exist " + darFile);


		startServer();

		final RepositoryObject environment = defineEnvironment();
		final RepositoryObject deploymentPackage = importDar(darFile);

		getLog().info(format("-- Deploy %s on %s", deploymentPackage.getId(), environment.getId()));
		getClient().deployAndWait(deploymentPackage.getId(), environment.getId(), mappings);
	}

	protected void undeploy() throws MojoExecutionException {
		startServer();

		if (forcedClean) {
			getClient().toggleSkipStepsMode();
			initialDeployment();
			getClient().toggleSkipStepsMode();
		}

		getClient().undeployAndWait(DEFAULT_ENVIRONMENT + "/" + artifactId);


		stopServer();
	}

	protected RepositoryObject importDar(File darFile) throws MojoExecutionException {
		getLog().info("Import dar file " + darFile);
		return getClient().importPackage(darFile);
	}

	protected RepositoryObject defineEnvironment() throws MojoExecutionException {
		getLog().info("Create the environment");
		List<String> members = new ArrayList<String>();
		for (ConfigurationItem each : environment) {
			getLog().info(" create " + each.getLabel());
			getClient().create(each);
			if (each.isAddedToEnvironment())
				members.add(each.getLabel());
		}

		ConfigurationItem ciEnvironment = new ConfigurationItem();
		ciEnvironment.setLabel(DEFAULT_ENVIRONMENT);
		ciEnvironment.setType("Environment");
		ciEnvironment.addParameter("members", members);
		return getClient().create(ciEnvironment);
	}

	ManifestPackager getPackager() {
		if (packager == null) {
			packager = new ManifestPackager(project);
			packager.setLog(getLog());
			packager.setGenerateManifestOnly(generateManifestOnly);
			packager.addDeployableArtifact(project.getArtifact());
			packager.addDeployableArtifacts(deployableArtifacts);
			packager.addMiddlewareResources(middlewareResources);
		}
		return packager;
	}
}
