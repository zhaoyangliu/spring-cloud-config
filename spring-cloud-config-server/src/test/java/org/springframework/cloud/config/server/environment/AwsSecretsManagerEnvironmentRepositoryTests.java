package org.springframework.cloud.config.server.environment;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;

import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.*;
import org.junit.Test;
import org.springframework.cloud.config.server.config.ConfigServerProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;

import static org.mockito.Mockito.mock;

public class AwsSecretsManagerEnvironmentRepositoryTests {

	final ConfigServerProperties server = new ConfigServerProperties();

	@Test
	public void listAllSecrets() throws IOException {
		final String secretName = "sandbox/dev/hostedcommander";
		final String region = "us-west-2";
		// Create a Secrets Manager client
		final AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard()
				.withRegion(region).build();

		System.out.println("Getting secrets");

		final String secret;
		final String decodedBinarySecret;

		final GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
				.withSecretId(secretName);
		GetSecretValueResult getSecretValueResult = null;
		try {
			getSecretValueResult = client.getSecretValue(getSecretValueRequest);
		}
		catch (final DecryptionFailureException e) {
			// Secrets Manager can't decrypt the protected secret text using the provided
			// KMS key.
			// Deal with the exception here, and/or rethrow at your discretion.
			throw e;
		}
		catch (final InternalServiceErrorException e) {
			// An error occurred on the server side.
			// Deal with the exception here, and/or rethrow at your discretion.
			throw e;
		}
		catch (final InvalidParameterException e) {
			// You provided an invalid value for a parameter.
			// Deal with the exception here, and/or rethrow at your discretion.
			throw e;
		}
		catch (final InvalidRequestException e) {
			// You provided a parameter value that is not valid for the current state of
			// the resource.
			// Deal with the exception here, and/or rethrow at your discretion.
			throw e;
		}
		catch (final ResourceNotFoundException e) {
			// We can't find the resource that you asked for.
			// Deal with the exception here, and/or rethrow at your discretion.
			throw e;
		}
		// Decrypts secret using the associated KMS CMK.
		// Depending on whether the secret is a string or binary, one of these fields will
		// be populated.
		if (getSecretValueResult.getSecretString() != null) {
			secret = getSecretValueResult.getSecretString();
			System.out.println("secret: " + secret);
		}
		else {
			decodedBinarySecret = new String(Base64.getDecoder()
					.decode(getSecretValueResult.getSecretBinary()).array());
			System.out.println("decodedBinarySecret: " + decodedBinarySecret);
		}
	}

}
