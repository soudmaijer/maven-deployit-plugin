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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.xebialabs.deployit.DeployItConfiguration;
import com.xebialabs.deployit.DeployitOptions;
import com.xebialabs.deployit.Server;
import com.xebialabs.deployit.core.api.dto.RepositoryObject;
import com.xebialabs.deployit.jcr.JackrabbitRepositoryFactoryBean;
import com.xebialabs.deployit.maven.packager.ManifestPackager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.transform;
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
	 * Tell the plugin it must connect to a remote Deployit Server.
	 * The following properties become mandatory: username, password, serverAddress, port.
	 * The default value is false, indicating the Deployit Maven plugin will launch a transient server when needed.
	 *
	 * @parameter default-value=false
	 */

	protected boolean remoteServerMode;

	/**
	 * Deployit server address
	 *
	 * @parameter default-value="" expression="${deployit.server}"
	 */
	private String serverAddress;

	/**
	 * Deployit Listen port
	 *
	 * @parameter default-value="4516" expression="${deployit.port}"
	 */
	private int port;


	/**
	 * username used to connect to a remote server
	 *
	 * @parameter default-value="" expression="${deployit.username}"
	 */
	private String username;


	/**
	 * password used to connect to a remote server
	 *
	 * @parameter default-value="" expression="${deployit.password}"
	 */
	private String password;

	/**
	 * Id of the environment used for the deployment.
	 * Useful only if you are using the remote server mode to avoid to create a new environment or to fetch an existing environment.
	 *
	 * @parameter
	 */
	private String environmentId = DEFAULT_ENVIRONMENT;


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
	 * Use this attribute to add a timestamp to the version of the deployit package.
	 * by default, SNAPSHOT versions are automatically timestamped
	 *
	 * @parameter default-value = false  expression="${deployit.timestamp}"
	 */
	protected boolean timestampedVersion;

	/**
	 * Perform a skipped deployment before clean it.
	 *
	 * @parameter default-value=false  expression="${deployit.forceclean}"
	 */
	protected boolean forcedClean;

	/**
	 * Perform a skipped deployment before clean it.
	 *
	 * @parameter default-value=false  expression="${deployit.delete.previous.dar}"
	 */
	protected boolean deletePreviouslyDeployedDar;


	protected ManifestPackager packager;

	protected MavenCli client;

	public static final String DEFAULT_ENVIRONMENT = "Environments/DefaultEnvironment";

	public static final String DEFAULT_DEPLOYMENT = "DefaultDeployment";

	protected void startServer() {
		if (remoteServerMode) {
			getLog().debug("remote server mode is on, do not start the server");
			return;
		}
		if (!isServerStarted()) {
			new File("recovery.dat").delete();
			File repositoryHomeDir = new File("target/repository");
			repositoryHomeDir.delete();

			getLog().info("STARTING DEPLOYIT SERVER");
			DeployItConfiguration context = new DeployItConfiguration();

			repositoryHomeDir.mkdirs();

			context.setJcrRepositoryPath(repositoryHomeDir.getPath());
			context.setHttpPort(port);
			context.setImportablePackagesPath(new File(outputDirectory, ManifestPackager.DEPLOYMENT_PACKAGE_DIR).getPath());
			context.setMinThreads(3);
			context.setMaxThreads(24);
			context.setSsl(false);

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
		}
	}

	private boolean isServerStarted() {
		getLog().debug("Check if the server is started on port " + port);
		try {
			ServerSocket socket = new ServerSocket(port);
			socket.close();
		} catch (Exception e) {
			getLog().debug("The server is started on " + port, e);
			return true;
		}
		getLog().debug("The server is not started on " + port);
		return false;
	}

	private void setupJcrRepository(File repositoryHomeDir) throws IOException, RepositoryException {

		getLog().info("start setupJcrRepository in " + repositoryHomeDir);
		FileUtils.deleteDirectory(repositoryHomeDir);
		String homeDirAbsolutePath = repositoryHomeDir.getAbsolutePath();

		JackrabbitRepositoryFactoryBean repositoryFactoryBean = new JackrabbitRepositoryFactoryBean();
		repositoryFactoryBean.setConfiguration(new ClassPathResource("jackrabbit-repository.xml"));
		repositoryFactoryBean.setHomeDir(new FileSystemResource(homeDirAbsolutePath));

		repositoryFactoryBean.setCreateHomeDirIfNotExists(true);
		repositoryFactoryBean.afterPropertiesSet();
		repositoryFactoryBean.configureJcrRepositoryForDeployit();
		repositoryFactoryBean.destroy();
		getLog().info("end setupJcrRepository in " + repositoryHomeDir);
	}

	public void stopServer() {
		if (remoteServerMode) {
			getLog().debug("remote server mode is on, do not stop the server");
			return;
		}
		Server.requestShutdown();
		new File("recovery.dat").delete();
	}

	protected MavenCli getClient() throws MojoExecutionException {
		if (client == null) {
			client = new MavenCli(serverAddress, port, username, password);
			client.setLogger(getLog());
			client.setSkipStepsMode(testmode);
		}
		return client;
	}


	protected void initialDeployment() throws MojoExecutionException {
		if (!remoteServerMode && environment == null)
			throw new MojoExecutionException("Environment cannot be empty in the embeded mode ");

		final File darFile = getPackager().getDarFile();
		if (!darFile.exists()) {
			getLog().info("Dar file does not exist " + darFile);
			getLog().info("generate it...");
			getPackager().perform();
			getPackager().seal();
		}

		startServer();

		final RepositoryObject environment = fetchEnvironment();
		final RepositoryObject deploymentPackage = importDar(darFile);
		final String application = (String) deploymentPackage.getValues().get("application");
		final String version = (String) deploymentPackage.getValues().get("version");

		getLog().info(String.format("-- Deploy %s on %s", deploymentPackage.getId(), environment.getId()));
		final String previousPackageId = getClient().deployAndWait(deploymentPackage.getId(), environment.getId(), getMappings(application, version));

		if (deletePreviouslyDeployedDar && StringUtils.isNotBlank(previousPackageId)) {
			getLog().info("Delete previously deployed dar "+previousPackageId);
			getClient().delete(previousPackageId);
		}
	}

	private List<MappingItem> getMappings(final String application, final String version) {
		if (mappings == null)
			return Collections.emptyList();

		return transform(mappings, new Function<MappingItem, MappingItem>() {
			@Override
			public MappingItem apply(MappingItem mappingItem) {
				final String source = mappingItem.getSource();
				if (!source.startsWith("Applications")) {
					mappingItem.setSource(format("%s/%s/%s", application, version, source));
					getLog().info(" mapping translation " + source + " --> " + mappingItem.getSource());
				}
				return mappingItem;
			}
		});
	}

	protected void undeploy() throws MojoExecutionException {
		startServer();

		if (forcedClean) {
			getClient().toggleSkipStepsMode();
			initialDeployment();
			getClient().toggleSkipStepsMode();
		}

		getClient().undeployAndWait(environmentId + "/" + artifactId);


		stopServer();
	}

	protected RepositoryObject importDar(File darFile) throws MojoExecutionException {
		getLog().info("Import dar file " + darFile);
		return getClient().importPackage(darFile);
	}

	protected RepositoryObject fetchEnvironment() throws MojoExecutionException {

		try {
			getLog().info("read the environment " + environmentId);
			return getClient().get(environmentId);
		} catch (Exception e) {
			getLog().debug(e.getMessage());
			if (environment == null)
				throw new MojoExecutionException("Cannot fetch environment " + environmentId + " and not members are defined in <environnment>", e);

			getLog().info("Create the members of environment");
			List<String> members = Lists.newArrayList();
			for (ConfigurationItem each : environment) {
				getLog().info(" create " + each.getLabel());
				getClient().create(each);
				if (each.isAddedToEnvironment())
					members.add(each.getLabel());
			}

			getLog().info("Create environment " + environmentId);
			ConfigurationItem ciEnvironment = new ConfigurationItem();
			ciEnvironment.setLabel(environmentId);
			ciEnvironment.setType("Environment");
			ciEnvironment.addParameter("members", members);
			return getClient().create(ciEnvironment);
		}
	}

	ManifestPackager getPackager() {
		if (packager == null) {
			packager = new ManifestPackager(project);

			packager.setLog(getLog());
			packager.setGenerateManifestOnly(generateManifestOnly);
			packager.setTimestampedVersion(timestampedVersion);

			packager.addDeployableArtifact(project.getArtifact());
			packager.addDeployableArtifacts(deployableArtifacts);
			packager.addMiddlewareResources(middlewareResources);


		}
		return packager;
	}
}
