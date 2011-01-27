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
import com.xebialabs.deployit.cli.MavenCli;
import com.xebialabs.deployit.core.api.dto.RepositoryObject;
import com.xebialabs.deployit.jcr.JackrabbitRepositoryFactoryBean;
import com.xebialabs.deployit.maven.packager.ManifestPackager;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import javax.jcr.RepositoryException;
import javax.script.ScriptException;
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
	 * @parameter default-value="${project.version}"
	 * @required
	 * @readonly
	 */
	protected String version;

	/**
	 * @parameter default-value="${project.packaging}"
	 * @required
	 * @readonly
	 */
	protected String packaging;

	/**
	 * Activate the test mode, the steps are not executed.
	 *
	 * @parameter default-value=false
	 */
	protected boolean testmode;


	/**
	 * The main JEE artifact to deploy
	 *
	 * @parameter default-value="${project.build.directory}/${project.build.finalName}.${project.packaging}"
	 * @required
	 */
	protected File jeeArtifact;

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

	/**
	 * The name of the DAR file to generate.
	 *
	 * @parameter alias="darName" expression="${project.build.finalName}"
	 * @required
	 */
	private String finalName;

	/**
	 * Classifier to add to the artifact generated. If given, the artifact will
	 * be an attachment instead.
	 *
	 * @parameter
	 */
	protected String classifier;

	private final StringBuffer fullScript = new StringBuffer();

	private MavenCli interpreter;

	public static final String DEFAULT_ENVIRONMENT = "Environments/DefaultEnvironment";
	public static final String DEFAULT_DEPLOYMENT = "DefaultDeployment";

	private static boolean SERVER_STARTED = false;

	protected void startServer() {
		if (!SERVER_STARTED) {
			new File("recovery.dat").delete();
			new File("repository").deleteOnExit();

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

	protected void interpret(String line) throws MojoExecutionException {
		getLog().info("Interpret [" + line + "]");
		fullScript.append(line).append('\n');
		try {
			getInterpreter().evaluate(line);
		} catch (ScriptException e) {
			throw new MojoExecutionException("interpret error", e);
		}
	}

	protected void interpret(List<String> cliCommands) throws MojoExecutionException {
		for (String cmd : cliCommands)
			interpret(cmd);

	}

	protected MavenCli getInterpreter() throws MojoExecutionException {
		if (interpreter == null) {
			interpreter = new MavenCli(getPort());
			interpreter.setLogger(getLog());
			interpreter.setSkipStepsMode(testmode);
		}
		return interpreter;
	}

	protected void deployit() throws MojoExecutionException {
		getLog().info(" ");
		getLog().info(" ");
		getLog().info("------------------------------------------------------------------");
		getLog().info("--- DEPLOYIT CHANGE PLAN  ----------------------------------------");
		getLog().info("------------------------------------------------------------------");
		interpret("changeplan steps");
		getLog().info("------------------------------------------------------------------");
		getLog().info("------------------------------------------------------------------");
		getLog().info("------------------------------------------------------------------");
		getLog().info(" ");
		getLog().info(" ");

		if (testmode) {
			interpret("deployit_nosteps");
			//interpret("export");
		} else {
			interpret("deployit");
		}
		interpret("changeplan changes");
	}

	public MavenProject getProject() {
		return project;
	}

	public void setProject(MavenProject project) {
		this.project = project;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getPackaging() {
		return packaging;
	}

	public void setPackaging(String packaging) {
		this.packaging = packaging;
	}

	public boolean isTestmode() {
		return testmode;
	}

	public void setTestmode(boolean testmode) {
		this.testmode = testmode;
	}

	public File getJeeArtifact() {
		return jeeArtifact;
	}

	public void setJeeArtifact(File jeeArtifact) {
		this.jeeArtifact = jeeArtifact;
	}

	public int getPort() {
		return (port == 0 ? 8888 : port);
	}

	public void setPort(int port) {
		this.port = port;
	}

	public List<MiddlewareResource> getMiddlewareResources() {
		return middlewareResources;
	}

	public void setMiddlewareResources(List<MiddlewareResource> middlewareResources) {
		this.middlewareResources = middlewareResources;
	}


	public List<ConfigurationItem> getEnvironment() {
		return environment;
	}

	public void setEnvironment(List<ConfigurationItem> environment) {
		this.environment = environment;
	}

	public String getScript() {
		return fullScript.toString();
	}

	protected DeployableArtifactItem getRealDeployableArtifact(final DeployableArtifactItem item)
			throws MojoExecutionException {

		if (!item.getLocation().contains(":")) {
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
			throw new MojoExecutionException(
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


	protected DeployableArtifactItem getRealDeployableArtifact(final Artifact artifact)
			throws MojoExecutionException {

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

	/**
	 * Returns the DAR file to generate, based on an optional classifier.
	 *
	 * @param basedir    the output directory
	 * @param finalName  the name of the ear file
	 * @param classifier an optional classifier
	 * @return the DAR file to generate
	 */
	protected File getDarFile(File basedir, String finalName, String classifier) {
		if (classifier == null) {
			classifier = "";
		} else if (classifier.trim().length() > 0 && !classifier.startsWith("-")) {
			classifier = "-" + classifier;
		}

		return new File(basedir, finalName + classifier + ".dar");
	}

	protected File getDarFile() {
		return getDarFile(outputDirectory, finalName, classifier);
	}

	protected void initialDeployment() throws MojoExecutionException {
		if (environment == null)
			throw new MojoExecutionException("Environment is empty");

		final File darFile = getDarFile();
		if (!darFile.exists())
			throw new MojoExecutionException("Dar file does not exist " + darFile);


		startServer();

		final RepositoryObject environment = defineEnvironment();

		final RepositoryObject deploymentPackage = importDar(darFile);


		getLog().info(format("-- Deploy %s on %s", deploymentPackage.getId(), environment.getId()));

		getInterpreter().deployAndWait(deploymentPackage.getId(), environment.getId(), mappings);
	}

	protected void undeploy() throws MojoExecutionException {
		startServer();

		if (forcedClean) {
			getInterpreter().toggleSkipStepsMode();
			initialDeployment();
			getInterpreter().toggleSkipStepsMode();
		}

		getInterpreter().undeployAndWait(DEFAULT_ENVIRONMENT + "/" + artifactId);


		stopServer();
	}

	protected RepositoryObject importDar(File darFile) throws MojoExecutionException {
		getLog().info("Import dar file " + darFile);
		return getInterpreter().importPackage(darFile);
	}

	protected RepositoryObject defineEnvironment() throws MojoExecutionException {
		getLog().info("Create the environment");
		List<String> members = new ArrayList<String>();
		for (ConfigurationItem each : environment) {
			getLog().info(" create " + each.getLabel());
			getInterpreter().create(each);
			if (each.isAddedToEnvironment())
				members.add(each.getLabel());
		}

		ConfigurationItem ciEnvironment = new ConfigurationItem();
		ciEnvironment.setLabel(DEFAULT_ENVIRONMENT);
		ciEnvironment.setType("Environment");
		ciEnvironment.addParameter("members", members);
		return getInterpreter().create(ciEnvironment);
	}
}
