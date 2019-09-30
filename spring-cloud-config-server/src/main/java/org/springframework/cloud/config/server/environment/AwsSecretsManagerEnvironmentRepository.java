/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.config.server.environment;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;

import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.*;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.config.ConfigServerProperties;
import org.springframework.core.Ordered;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AwsSecretsManagerEnvironmentRepository
		implements EnvironmentRepository, Ordered {

	private final AWSSecretsManager secretsManager;

	private final ConfigServerProperties serverProperties;

	protected int order = Ordered.LOWEST_PRECEDENCE;

	public AwsSecretsManagerEnvironmentRepository(AWSSecretsManager secretsManager,
			ConfigServerProperties server) {
		this.secretsManager = secretsManager;
		this.serverProperties = server;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	// public List<SecretListEntry> getTestHostedcommanderSecrets() {
	// System.out.println("get one secret");
	// List<SecretListEntry> secretListEntryList = new ArrayList<>();
	// ListSecretsResult listSecretsResult = null;
	//
	// final ListSecretsRequest listSecretsRequest = new ListSecretsRequest()
	// .withMaxResults(10);
	// if (listSecretsResult != null) {
	// listSecretsRequest.setNextToken(listSecretsResult.getNextToken());
	// }
	//
	// try {
	// listSecretsResult = this.secretsManager.listSecrets(listSecretsRequest);
	// secretListEntryList.addAll(listSecretsResult.getSecretList());
	//
	// System.out.println("Next token: " + listSecretsResult.getNextToken());
	// }
	// catch (final Exception e) {
	// // We can't find the resource that you asked for.
	// // Deal with the exception here, and/or rethrow at your discretion.
	// System.out.println("Exception listing secrets");
	// throw e;
	// }
	// return secretListEntryList;
	// }

	public List<SecretListEntry> getAllSecrets() {
		List<SecretListEntry> secretListEntryList = new ArrayList<>();
		ListSecretsResult listSecretsResult = null;

		do {
			final ListSecretsRequest listSecretsRequest = new ListSecretsRequest()
					.withMaxResults(10);
			if (listSecretsResult != null) {
				listSecretsRequest.setNextToken(listSecretsResult.getNextToken());
			}

			try {
				listSecretsResult = this.secretsManager.listSecrets(listSecretsRequest);
				secretListEntryList.addAll(listSecretsResult.getSecretList());

				System.out.println("Next token: " + listSecretsResult.getNextToken());
			}
			catch (final Exception e) {
				// We can't find the resource that you asked for.
				// Deal with the exception here, and/or rethrow at your discretion.
				System.out.println("Exception listing secrets");
				throw e;
			}
		}
		while (listSecretsResult.getNextToken() != null
				&& !listSecretsResult.getNextToken().isEmpty());

		return secretListEntryList;
	}

	@Override
	public Environment findOne(String specifiedApplication, String specifiedProfile,
			String specifiedLabel) {
		final String application = StringUtils.isEmpty(specifiedApplication)
				? serverProperties.getDefaultApplicationName() : specifiedApplication;
		final String profile = StringUtils.isEmpty(specifiedProfile)
				? serverProperties.getDefaultProfile() : specifiedProfile;
		final String label = StringUtils.isEmpty(specifiedLabel)
				? serverProperties.getDefaultLabel() : specifiedLabel;

		StringBuilder objectKeyPrefix = new StringBuilder();
		if (!StringUtils.isEmpty(label)) {
			objectKeyPrefix.append(label).append("/");
		}

		objectKeyPrefix.append(application).append("-").append(profile);

		final Environment environment = new Environment(application, profile);
		environment.setLabel(label);

		final ListSecretsRequest listSecretsRequest = new ListSecretsRequest()
				.withMaxResults(100);

		ListSecretsResult listSecretsResult = null;
		try {
			listSecretsResult = this.secretsManager.listSecrets(listSecretsRequest);
		}
		catch (final Exception e) {
			// We can't find the resource that you asked for.
			// Deal with the exception here, and/or rethrow at your discretion.
			System.out.println("Exception listing secrets");
			throw e;
		}

		final String region = "us-west-2";
		// Create a Secrets Manager client
		final AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard()
				.withRegion(region).build();

		System.out.println("Getting secrets");

		final String decodedBinarySecret;

		final String secretName = "sandbox/dev/hostedcommander";

		final GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
				.withSecretId(secretName);
		GetSecretValueResult getSecretValueResult = null;
		try {
			getSecretValueResult = client.getSecretValue(getSecretValueRequest);
		}
		catch (final ResourceNotFoundException e) {
			// We can't find the resource that you asked for.
			// Deal with the exception here, and/or rethrow at your discretion.
			System.out.println(
					"secret value not set, no staging label, will skip, exception: "
							+ e.getMessage());
		}
		catch (final Exception e) {
			System.out.println("secret getting exception: " + e.getMessage());
		}

		String secret = getSecretValueResult.getSecretString();

		System.out.println("secret result: " + secret);

		final YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();

		yaml.setResources(
				new InputStreamResource(new ByteArrayInputStream(secret.getBytes())));

		Properties properties = yaml.getObject();

		if (!properties.isEmpty()) {
			environment.add(new PropertySource(secretName, properties));
		}

		// List<SecretListEntry> secretListEntryList = getAllSecrets();
		//
		// for (SecretListEntry secretListEntry : secretListEntryList) {
		// final String secret;
		// final String secretPath = secretListEntry.getName();
		// System.out.println("current secret path: " + secretPath);
		//
		// final GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
		// .withSecretId(secretPath);
		//
		// GetSecretValueResult getSecretValueResult = null;
		// try {
		// getSecretValueResult = client.getSecretValue(getSecretValueRequest);
		// }
		// catch (final ResourceNotFoundException e) {
		// // We can't find the resource that you asked for.
		// // Deal with the exception here, and/or rethrow at your discretion.
		// System.out.println(
		// "secret value not set, no staging label, will skip, exception: "
		// + e.getMessage());
		// continue;
		// }
		// catch (final Exception e) {
		// System.out.println("secret getting exception: " + e.getMessage());
		// continue;
		// }
		// secret = getSecretValueResult.getSecretString();
		//
		// // System.out.println("secret result: " + secret);
		//
		// final YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
		//
		// yaml.setResources(
		// new InputStreamResource(new ByteArrayInputStream(secret.getBytes())));
		//
		// Properties properties = yaml.getObject();
		//
		// if (!properties.isEmpty()) {
		// environment
		// .add(new PropertySource(secretListEntry.getName(), properties));
		// }
		// }

		return environment;
	}

}
