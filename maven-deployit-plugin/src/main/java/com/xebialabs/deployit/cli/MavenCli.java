package com.xebialabs.deployit.cli;

import com.xebialabs.deployit.cli.api.ObjectFactory;
import com.xebialabs.deployit.cli.api.Proxies;
import com.xebialabs.deployit.cli.api.RepositoryClient;
import com.xebialabs.deployit.cli.rest.ResponseExtractor;
import com.xebialabs.deployit.core.api.dto.RepositoryObject;
import com.xebialabs.deployit.maven.ConfigurationItem;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.maven.plugin.MojoExecutionException;

import javax.script.ScriptException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.MalformedURLException;

import static com.xebialabs.deployit.jcr.JcrConstants.ADMIN_PASSWORD;
import static com.xebialabs.deployit.jcr.JcrConstants.ADMIN_USERNAME;
import static java.lang.String.format;

/**
 * Maven Client for Deployit.
 */
public class MavenCli {

	//private final JythonInterpreter interpreter;
	private final JythonInterpreterOptions options;
	private final HttpClient client;
	private final Proxies proxies;
	private final ObjectFactory factory;
	private final RepositoryClient repositoryClient;

	public MavenCli(int port) {
		options = new JythonInterpreterOptions();
		options.setHost("localhost");
		options.setPort(port);
		options.setExposeProxies(true);
		options.setUsername(ADMIN_USERNAME);
		options.setPassword(ADMIN_PASSWORD);
		client = getAuthenticatingHttpClient();
		attemptToConnectToServer();
		proxies = new Proxies(options, client);
		factory = new ObjectFactory(proxies);
		repositoryClient = new RepositoryClient(proxies);
	}

	public void evaluate(String line) throws MojoExecutionException, ScriptException {
		//	proxies.getImportablePackage().importPackage("/tmp/truc.dar");
		System.out.println("----- " + line);
	}

	HttpClient getAuthenticatingHttpClient() {
		final HttpClient httpClient = new HttpClient();
		final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(options.getUsername(), options.getPassword());
		httpClient.getState().setCredentials(AuthScope.ANY, credentials);
		httpClient.getParams().setAuthenticationPreemptive(true);
		httpClient.getParams().setConnectionManagerTimeout(30000);
		return httpClient;
	}

	private void attemptToConnectToServer() {
		String host = options.getHost();
		int port = options.getPort();
		System.out.println("Connecting to the Deployit server at " + host + ":" + port + "...");
		String urlToConnectTo = "http://" + host + ":" + port;
		try {
			final int responseCode = client.executeMethod(new GetMethod(urlToConnectTo));
			if (responseCode != 200) {
				throw new IllegalStateException("Could contact the server at " + urlToConnectTo + " but received an HTTP error code, " + responseCode);
			} else {
				System.out.println("Succesfully connected.");
			}
		} catch (MalformedURLException mue) {
			throw new IllegalStateException("Could not contact the server at " + urlToConnectTo, mue);
		} catch (IOException e) {
			throw new IllegalStateException("Could not contact the server at " + urlToConnectTo, e);
		}
	}

	public RepositoryObject create(ConfigurationItem ci) {
		final Response response = proxies.getRepository().create(ci.getLabel(), factory.configurationItem(ci.getType(), ci.getProperties()));
		return checkForValidations(response);
	}

	private RepositoryObject checkForValidations(final Response response) {
		final ResponseExtractor responseExtractor = new ResponseExtractor(response);
		final RepositoryObject ci = responseExtractor.getEntity();
		if (!responseExtractor.isValidResponse() && !ci.getValidations().isEmpty()) {
			throw new IllegalStateException(format("Configuration item contained validation errors: {%s}", ci.getValidations()));
		}
		return ci;
	}
}
