package com.xebialabs.deployit.cli;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.xebialabs.deployit.cli.api.DeployitClient;
import com.xebialabs.deployit.cli.api.ObjectFactory;
import com.xebialabs.deployit.cli.api.Proxies;
import com.xebialabs.deployit.cli.api.RepositoryClient;
import com.xebialabs.deployit.cli.rest.ResponseExtractor;
import com.xebialabs.deployit.core.api.dto.RepositoryObject;
import com.xebialabs.deployit.core.api.dto.StepInfo;
import com.xebialabs.deployit.core.api.dto.TaskInfo;
import com.xebialabs.deployit.maven.ConfigurationItem;
import com.xebialabs.deployit.maven.MappingItem;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import javax.script.ScriptException;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import static com.xebialabs.deployit.jcr.JcrConstants.ADMIN_PASSWORD;
import static com.xebialabs.deployit.jcr.JcrConstants.ADMIN_USERNAME;
import static java.lang.String.format;

/**
 * Maven Client for Deployit.
 */
public class MavenCli {

	private final JythonInterpreterOptions options;
	private final HttpClient client;
	private final Proxies proxies;
	private final ObjectFactory factory;
	private final RepositoryClient repositoryClient;
	private final DeployitClient deployitClient;

	private Log logger;

	private boolean testMode = false;

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
		deployitClient = new DeployitClient(proxies);


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

	public RepositoryObject importPackage(File darFile) {
		return deployitClient.importPackage(darFile.getPath());
	}

	public void deployAndWait(String source, String target, List<MappingItem> mappings) {
		final RepositoryObject[] generatedMappings = generateMappings(source, target, mappings);
		final String taskId = deployitClient.prepareDeployment(source, target, generatedMappings);
		if (testMode) {
			logger.info("Test mode, skip all the steps");
			deployitClient.skipSteps(taskId, range(1, deployitClient.retrieveTaskInfo(taskId).getNrOfSteps() + 1));
		}
		deployitClient.startTaskAndWait(taskId);
		checkTaskState(taskId);
	}

	public void undeployAndWait(String source) {
		deployAndWait(source, null, null);
	}

	private RepositoryObject[] generateMappings(String source, String target, List<MappingItem> mappings) {

		if (target == null)
			return null;

		if (mappings == null)
			mappings = Lists.newArrayList();

		final RepositoryObject dp = repositoryClient.read(source);
		final Map<String, Object> values = dp.getValues();
		final Object dpp = values.get("deployableArtifacts");
		List<String> deployableArtifacts = Lists.newArrayList();
		deployableArtifacts.addAll((Collection<? extends String>) dpp);

		logger.info(format("generate mappings %s - %s", source, target));
		final RepositoryObject[] generatedMappings = deployitClient.generateMappings(deployableArtifacts, target);
		for (RepositoryObject repositoryObjectMapping : generatedMappings) {
			logger.info("generated mapping " + repositoryObjectMapping);
			final Map<String, Object> mappingValues = repositoryObjectMapping.getValues();

			final MappingItem configuredMapping = searchCandidateMapping(mappings, mappingValues);
			if (configuredMapping == null)
				continue;

			//Manage Lamda properties
			for (Map.Entry<String, Object> entry : configuredMapping.getProperties().entrySet()) {
				logger.debug(format("%s %s with %s", (mappings.contains(entry.getKey()) ? "overwrite" : "set"), entry.getKey(), entry.getValue().toString()));
				mappingValues.put(entry.getKey(), entry.getValue());
			}
			//Manage K,V pairs
			final List<Map<String, String>> configuredMappingKeyValuePairs = configuredMapping.getKeyValuePairs();
			if (configuredMappingKeyValuePairs != null) {
				//TODO: check the key from generated mappings before override it.
				final Object keyValuePairsFromMapping = mappingValues.get("keyValuePairs");
				logger.debug("replace kvPair " + keyValuePairsFromMapping + " by " + configuredMappingKeyValuePairs);
				mappingValues.put("keyValuePairs", configuredMappingKeyValuePairs);
			}
			logger.info("modified mapping  " + repositoryObjectMapping);
		}
		return generatedMappings;
	}

	private MappingItem searchCandidateMapping(List<MappingItem> mappings, Map<String, Object> mappingValues) {
		final String sourceMapping = (String) mappingValues.get("source");
		final String targetMapping = (String) mappingValues.get("target");
		final Collection<MappingItem> foundMappings = Collections2.filter(mappings, new Predicate<MappingItem>() {
			public boolean apply(MappingItem mappingItem) {
				if (mappingItem == null)
					return false;
				return mappingItem.getSource().equals(sourceMapping) && mappingItem.getTarget().equals(targetMapping);
			}
		});

		if (foundMappings.isEmpty()) {
			logger.debug(format("Found 0 mappings for source (%s) and target (%s) in mappings (%s) ", sourceMapping, targetMapping, mappings));
			return null;
		}

		if (foundMappings.size() > 1)
			throw new IllegalStateException(format("Found %n mappings which are candidate for source (%s) and target (%s) : %s", foundMappings.size(), sourceMapping, targetMapping, foundMappings.toString()));

		return foundMappings.iterator().next();
	}


	public void setLogger(Log logger) {
		this.logger = logger;
	}

	private void checkTaskState(String taskId) {
		final TaskInfo taskInfo = deployitClient.retrieveTaskInfo(taskId);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
		logger.info(format("%s Label      %s", taskId, taskInfo.getLabel()));
		logger.info(format("%s State      %s %d/%d", taskId, taskInfo.getState(), taskInfo.getCurrentStepNr(), taskInfo.getNrOfSteps()));
		final GregorianCalendar startDate = (GregorianCalendar) taskInfo.getStartDate();
		final GregorianCalendar completionDate = (GregorianCalendar) taskInfo.getCompletionDate();
		logger.info(format("%s Start      %s", taskId, sdf.format(startDate.getTime())));
		logger.info(format("%s Completion %s", taskId, sdf.format(completionDate.getTime())));

		StringBuilder sb = new StringBuilder();
		for (int i = 1; i <= taskInfo.getNrOfSteps(); i++) {
			final Response stepInfoResponse = proxies.getTaskRegistry().getStepInfo(taskId, i, null);
			final StepInfo stepInfo = new ResponseExtractor(stepInfoResponse).getEntity();
			final String description = stepInfo.getDescription();
			final String log = stepInfo.getLog();
			String stepInfoMessage;
			if (StringUtils.isEmpty(log) || description.equals(log)) {
				stepInfoMessage = format("%s step #%d %s\t%s", taskId, i, stepInfo.getState(), description);
			} else {
				stepInfoMessage = format("%s step #%d %s\t%s\n%s", taskId, i, stepInfo.getState(), description, log);
			}

			logger.info(stepInfoMessage);
			if ("FAILED".endsWith(stepInfo.getState()))
				sb.append(stepInfoMessage);
		}

		if ("STOPPED".equals(taskInfo.getState()))
			throw new IllegalStateException(format("Errors when executing task %s: %s", taskId, sb));
	}


	public void setSkipStepsMode(boolean testMode) {
		this.testMode = testMode;
	}

	public void toggleSkipStepsMode()  {
		this.testMode = (testMode ? false : true);
	}

	private Integer[] range(int begin, int end) {
		Integer[] result = new Integer[end - begin];
		for (int i = begin; i < end; i++) {
			result[i - begin] = i;
		}
		return result;
	}
}
