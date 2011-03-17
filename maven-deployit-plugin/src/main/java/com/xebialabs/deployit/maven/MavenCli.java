package com.xebialabs.deployit.maven;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.xebialabs.deployit.cli.CliOptions;
import com.xebialabs.deployit.cli.api.DeployitClient;
import com.xebialabs.deployit.cli.api.ObjectFactory;
import com.xebialabs.deployit.cli.api.Proxies;
import com.xebialabs.deployit.cli.api.RepositoryClient;
import com.xebialabs.deployit.cli.rest.ResponseExtractor;
import com.xebialabs.deployit.core.api.dto.*;
import com.xebialabs.deployit.core.api.resteasy.DeployitClientException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.logging.Log;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.google.common.collect.Collections2.filter;
import static com.xebialabs.deployit.jcr.JcrConstants.ADMIN_PASSWORD;
import static com.xebialabs.deployit.jcr.JcrConstants.ADMIN_USERNAME;
import static java.lang.String.format;

/**
 * Maven Client for Deployit.
 */
public class MavenCli {

	private final CliOptions options;
	private final HttpClient client;
	private final Proxies proxies;
	private final ObjectFactory factory;
	private final RepositoryClient repositoryClient;
	private final DeployitClient deployitClient;

	private Log logger;

	private boolean testMode = false;

	public MavenCli(String serverAddress, int port, String username, String password) {
		options = new CliOptions();
		if (StringUtils.isNotBlank(serverAddress))
			options.setHost(serverAddress);

		options.setPort(port);
		options.setExposeProxies(true);
		options.setUsername(StringUtils.isBlank(username) ? ADMIN_USERNAME : username);
		options.setPassword(StringUtils.isBlank(password) ? ADMIN_PASSWORD : password);
		client = getAuthenticatingHttpClient();
		attemptToConnectToServer();
		proxies = new Proxies(options, client);
		factory = new ObjectFactory(proxies);
		repositoryClient = new RepositoryClient(proxies);
		deployitClient = new DeployitClient(proxies);
	}


	private HttpClient getAuthenticatingHttpClient() {
		final HttpClient httpClient = new HttpClient();
		final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(options.getUsername(), options.getPassword());
		httpClient.getState().setCredentials(AuthScope.ANY, credentials);
		httpClient.getParams().setAuthenticationPreemptive(true);
		httpClient.getParams().setConnectionManagerTimeout(30000);
		return httpClient;
	}

	private void attemptToConnectToServer() {
		String urlToConnectTo = options.getUrl();
		System.out.println("Connecting to the Deployit server at " + urlToConnectTo + "...");
		try {
			final int responseCode = client.executeMethod(new GetMethod(urlToConnectTo + "/deployit/server/info"));
			if (responseCode == 200) {
				System.out.println("Succesfully connected.");
			} else if (responseCode == 401 || responseCode == 403) {
				throw new IllegalStateException("You were not authenticated correctly, did you use the correct credentials?");
			} else {
				throw new IllegalStateException("Could contact the server at " + urlToConnectTo + " but received an HTTP error code, " + responseCode);
			}
		} catch (MalformedURLException mue) {
			throw new IllegalStateException("Could not contact the server at " + urlToConnectTo, mue);
		} catch (IOException e) {
			throw new IllegalStateException("Could not contact the server at " + urlToConnectTo, e);
		}
	}

	public RepositoryObject create(ConfigurationItem ci) {
		final String id = ci.getLabel();
		try {
			return get(id);
		} catch (Exception e) {
			logger.debug(format("%s does not exist, create it", id));
			final Response response = getProxies().getRepository().create(id, getFactory().configurationItem(ci.getType(), ci.getProperties()));
			return checkForValidations(response);
		}

	}

	public void delete(String id) {
		try {
			getRepositoryClient().delete(id);
		} catch (Exception e) {
			logger.debug(format("delete fails %s", id));
		}
	}


	public RepositoryObject get(String ciId) {
		final Response response = getProxies().getRepository().read(ciId);
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
		return getDeployitClient().importPackage(darFile.getPath());
	}

	/**
	 *
	 * @param source
	 * @param target
	 * @param mappings
	 * @return the id of the previous deployed package (or null) if not found.
	 */
	public String deployAndWait(String source, String target, List<MappingItem> mappings) {

		final RepositoryObject[] generatedMappings = generateMappings(source, target, mappings);
		target = computeRealTarget(source, target);
		logger.info("  real target is " + target);
		String previousDeployedPackage = getPreviousDeployedPackage(target);
		String taskId = null;
		try {
			taskId = prepareDeployment(source, target, generatedMappings);
			if (taskId == null) {
				throw new RuntimeException("Prepare deployemend failed");
			}
			if (testMode) {
				logger.info("Test mode, skip all the steps");
				getDeployitClient().skipSteps(taskId, range(1, getDeployitClient().retrieveTaskInfo(taskId).getNrOfSteps() + 1));
			}

			logger.info("Start deployment task " + taskId);
			getDeployitClient().startTaskAndWait(taskId);
			checkTaskState(taskId);
		} catch (DeployitClientException e) {
			logger.error(" DeployitClientException: " + e.getMessage());
			if (!e.getMessage().contains("The mappings did not lead to any steps")) {
				throw e;
			}
		} finally {
			if (taskId != null) {
				logger.info(" Cancel task " + taskId);
				getDeployitClient().cancelTask(taskId);
			}
		}
		return previousDeployedPackage;
	}

	private String prepareDeployment(String source, String target, RepositoryObject[] mappings) {
		RepositoryObjects mappingsDto = new RepositoryObjects();
        if (mappings != null && mappings.length > 0) {
            mappingsDto.setObjects(Arrays.asList(mappings));
        }

        // validate the mappings
	    final ResponseExtractor responseExtractor = new ResponseExtractor(proxies.getDeployment().validate(source, target, mappingsDto));
	    if (responseExtractor.isValidResponse()) {
			// prepare the deployment
			Steps steps = new ResponseExtractor(proxies.getDeployment().prepare(source, target, mappingsDto)).getEntity();
			return steps.getTaskId();
        } else {
		    final RepositoryObjects validated = responseExtractor.getEntity();
		    for (RepositoryObject v : validated.getObjects()) {
			    if (!v.getValidations().isEmpty()) {
				    logger.error(format("mapping with id %s has the following validation errors %s", v.getId(), v.getValidations()));
			    }
			    throw new RuntimeException("Mapping validation errors");
		    }
		    return null;
        }
	}

	private String getPreviousDeployedPackage(String target) {
		try {
			final RepositoryObject dp = getRepositoryClient().read(target);
			final Map<String, Object> values = dp.getValues();
			Object source = values.get("source");
			return (source == null ? null : source.toString());
		} catch (Exception e) {
			logger.debug("previous deployed package "+target+" not found",e);
			return  null;
		}
	}

	private String computeRealTarget(String source, String target) {
		//target = "Environments/DefaultEnvironment/deployit-petclinic";
		String deployedApplicationId = target + "/" + StringUtils.split(source, "/")[1];
		logger.info("  deployedApplicationId " + deployedApplicationId);
		for (String d : getRepositoryClient().search("Deployment")) {
			if (d.equals(deployedApplicationId))
				return deployedApplicationId;
		}

		return target;
	}

	public void undeployAndWait(String source) {
		deployAndWait(source, null, null);
	}

	private RepositoryObject[] generateMappings(String source, String target, List<MappingItem> mappings) {
		if (target == null)
			return null;

		if (mappings == null)
			mappings = Lists.newArrayList();

		final RepositoryObject dp = getRepositoryClient().read(source);
		final Map<String, Object> values = dp.getValues();
		final Object dpp = values.get("deployableArtifacts");
		List<String> deployableArtifacts = Lists.newArrayList();
		deployableArtifacts.addAll((Collection<? extends String>) dpp);

		logger.info(format("generate mappings %s - %s", source, target));
		final RepositoryObject[] generatedMappings = getDeployitClient().generateMappings(deployableArtifacts, target);
		for (RepositoryObject repositoryObjectMapping : generatedMappings) {
			logger.info("  process generated mapping " + repositoryObjectMapping.getId());
			logger.debug("   mapping id ->" + repositoryObjectMapping.getId());
			final Map<String, Object> mappingValues = repositoryObjectMapping.getValues();

			final MappingItem configuredMapping = searchCandidateMapping(mappings, mappingValues);
			if (configuredMapping == null)
				continue;

			logger.debug("   found a configured mapping " + configuredMapping);
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
			logger.info("   modified mapping  " + repositoryObjectMapping);
		}
		return generatedMappings;
	}

	private MappingItem searchCandidateMapping(List<MappingItem> mappings, Map<String, Object> mappingValues) {
		//TODO: include the type of the mapping too
		final String sourceMapping = (String) mappingValues.get("source");
		final String targetMapping = (String) mappingValues.get("target");
		final Collection<MappingItem> foundMappings = filter(mappings, new Predicate<MappingItem>() {
			public boolean apply(MappingItem mappingItem) {
				if (mappingItem == null)
					return false;

				return mappingItem.equals(sourceMapping, targetMapping);
			}
		});

		if (foundMappings.isEmpty()) {
			logger.debug(format("   found 0 mapping for source (%s) and target (%s) in mappings (%s) ", sourceMapping, targetMapping, mappings));
			return null;
		}

		if (foundMappings.size() > 1)
			throw new IllegalStateException(format("found %n mappings which are candidate for source (%s) and target (%s) : %s", foundMappings.size(), sourceMapping, targetMapping, foundMappings.toString()));


		final MappingItem foundMapping = foundMappings.iterator().next();
		logger.debug(format("   found 1 mapping for source (%s) and target (%s) : ", sourceMapping, targetMapping, foundMapping));
		return foundMapping;
	}


	public void setLogger(Log logger) {
		this.logger = logger;
	}

	private void checkTaskState(String taskId) {
		final TaskInfo taskInfo = getDeployitClient().retrieveTaskInfo(taskId);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
		logger.info(format("%s Label      %s", taskId, taskInfo.getLabel()));
		logger.info(format("%s State      %s %d/%d", taskId, taskInfo.getState(), taskInfo.getCurrentStepNr(), taskInfo.getNrOfSteps()));
		final GregorianCalendar startDate = (GregorianCalendar) taskInfo.getStartDate();
		final GregorianCalendar completionDate = (GregorianCalendar) taskInfo.getCompletionDate();
		logger.info(format("%s Start      %s", taskId, sdf.format(startDate.getTime())));
		logger.info(format("%s Completion %s", taskId, sdf.format(completionDate.getTime())));

		StringBuilder sb = new StringBuilder();
		for (int i = 1; i <= taskInfo.getNrOfSteps(); i++) {
			final Response stepInfoResponse = getProxies().getTaskRegistry().getStepInfo(taskId, i, null);
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

	public void toggleSkipStepsMode() {
		this.testMode = (testMode ? false : true);
	}

	private Integer[] range(int begin, int end) {
		Integer[] result = new Integer[end - begin];
		for (int i = begin; i < end; i++) {
			result[i - begin] = i;
		}
		return result;
	}

	Proxies getProxies() {
		return proxies;
	}

	ObjectFactory getFactory() {
		return factory;
	}

	RepositoryClient getRepositoryClient() {
		return repositoryClient;
	}

	DeployitClient getDeployitClient() {
		return deployitClient;
	}
}
