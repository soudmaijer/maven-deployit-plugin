package com.xebialabs.deployit.maven;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class BaseForTestMojo extends AbstractMojoTestCase {

	protected GenerateDeploymentPackageMojo darMojo;
	protected DeployableArtifactItem configurationFiles;
	protected DeployableArtifactItem sqlFiles;
	protected DeployableArtifactItem warFile;
	protected MiddlewareResource mrDataSource;
	protected MiddlewareResource mrModjk;
	protected DeployMojo deployMojo;
	protected ConfigurationItem host;
	protected ConfigurationItem tomcatServer;

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
		mrDataSource.addParameter("driver", "com.mysql.jdbc.Driver");
		mrDataSource.addParameter("url", "jdbc:mysql://localhost/petclinic");
		mrDataSource.addParameter("username", "petclinic");
		mrDataSource.addParameter("password", "secr$t");
		mrDataSource.addParameter("settings-EntryKey-1", "autoCommit");
		mrDataSource.addParameter("settings-EntryValue-1", "true");

		mrModjk = new MiddlewareResource();
		mrModjk.setName("AnModJkConfiguration");
		mrModjk.setType("ModJkApacheModuleConfiguration");
		mrModjk.addParameter("urlMounts-EntryPrefix-1", "/foo");
		mrModjk.addParameter("jkstatus", "true");


		host = new ConfigurationItem();
		host.setType("Host");
		host.addParameter("label", "Infrastructure/tomcat6.vm");
		host.addParameter("address", "ubuntu-weblogic-1.local");
		host.addParameter("username", "weblogic");
		host.addParameter("password", "weblogic");
		host.addParameter("operatingSystemFamily", "UNIX");
		host.addParameter("accessMethod", "SSH_SCP");

		tomcatServer = new ConfigurationItem();
		tomcatServer.setType("TomcatUnmanagedServer");
		tomcatServer.addParameter("host", "Infrastructure/tomcat6.vm");
		tomcatServer.addParameter("label", "Infrastructure/tomcat6.vm/tomcat6-1");
		tomcatServer.addParameter("tomcatHome", "/opt/apache-tomcat-6.0.26");
		tomcatServer.addParameter("startCommand", "/opt/apache-tomcat-6.0.26/bin/catalina.sh start");
		tomcatServer.addParameter("stopCommand", "/opt/apache-tomcat-6.0.26/bin/catalina.sh stop");
		tomcatServer.addParameter("appBase", "/opt/apache-tomcat-6.0.26/mywebapps");
		tomcatServer.addParameter("baseUrl", "http://ubuntu-weblogic-1.local:8080");
		tomcatServer.addParameter("ajpPort", "8009");


		darMojo = new GenerateDeploymentPackageMojo();
		deployMojo = new DeployMojo();
	}
}
