
package com.kapil.tenant.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapil.tenant.exceptionHandler.CustomException;
import com.kapil.tenant.model.Tenant;

import okhttp3.Request;
import okhttp3.Response;


@Service
public class CognosEnvRestManagerClient {

	@Value("${cognos.api.url}") // The base URL for the Cognos API
	private String cognosApiUrl;

	@Value("${cognos.api.cam-token}")
	private String camToken;

	@Value("${cognos.api.xsrf-token}")
	private String xsrfToken;

	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public CognosEnvRestManagerClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
	}

	public HttpHeaders initializeHeader(String authHeader) {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		headers.set("IBM-BA-Authorization", "CAM " + camToken);
		headers.set("Content-Type", "application/json");
		headers.set("X-XSRF-Token", xsrfToken);
		headers.setCacheControl(CacheControl.noCache());
		return headers;
	}

	// Setup the environment for a tenant in Cognos
	public boolean setupTenantEnv(String requestBodyJson, boolean createTeamFolder, String authHeader) {
		boolean result = false;
		String apiUrl = cognosApiUrl + "/tenants?create_team_folder=" + createTeamFolder;
		HttpHeaders headers = initializeHeader(authHeader);
		Map<String, Object> requestBody = null;
		try {
			requestBody = objectMapper.readValue(requestBodyJson, new TypeReference<>() {
			});
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		System.out.println("Cognos environment setup for tenant: " + requestBody.get("tenantId"));
		// Create HttpEntity object for the request (empty body if you don't need it)
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
		ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
		if (response.getStatusCode() == HttpStatus.CREATED) {
			String responseBody = response.getBody();
			System.out.println("responseBody" + responseBody);
			result = true;
		}
		return result;
	}

	// Get the environment for a tenant in Cognos
	public void getTenantEnv(String tenantId, String authHeader) {
		String apiUrl = cognosApiUrl + "/tenants/?tenant_id" + tenantId;
		HttpHeaders headers = initializeHeader(authHeader);
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);
		String responseBody = response.getBody();

		System.out.println("Cognos environment details with status : " + response.getStatusCode() + " for tenant: "
				+ responseBody);
	}

	// get all list of api keys
	public void getAPIKeys(String authHeader) {
		String apiUrl = cognosApiUrl + "/security/login_api_keys";
		HttpHeaders headers = initializeHeader(authHeader);
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);
		String responseBody = response.getBody();
		JsonNode root = null;
		try {
			root = objectMapper.readTree(responseBody);
		} catch (JsonProcessingException e) {
			throw new CustomException(CognosEnvRestManagerClient.class + " authHeader" + authHeader);
		}
		JsonNode keysArray = root.get("keys");
		// Get the first JSON object in the array
		JsonNode firstObject = keysArray.get(0);

		// Print the full object
		System.out.println(firstObject.toPrettyString());
	}

	// Update the tenant in Cognos
	public boolean updateTenant(String tenantId, Tenant reqeustTenantBody, String authHeader) {
		boolean result = false;
		String apiUrl = cognosApiUrl + "/tenants?tenant_id" + tenantId;
		// Make an API call to Cognos to reset the environment for the tenant
		// restTemplate.putForObject(resetUrl, null, String.class);
		System.out.println("Cognos environment reset for tenant: " + tenantId);
		
		HttpHeaders headers = initializeHeader(authHeader);
		Map<String, Object> requestBody = null;
		try {
			String json = objectMapper.writeValueAsString(reqeustTenantBody);
			requestBody = objectMapper.readValue(json, new TypeReference<>() {
			});
		} catch (JsonProcessingException e) {
			throw new CustomException(CognosEnvRestManagerClient.class + " updateTenant" + tenantId);
		}
		
		
		System.out.println("Cognos environment setup for tenant: " + requestBody.get("tenantId"));
		// Create HttpEntity object for the request (empty body if you don't need it)
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
		ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.PUT, entity, String.class);
		if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
			String responseBody = response.getBody();
			System.out.println("responseBody" + responseBody);
			result = true;
		}
		return result;
	}
	
	// delete the existing tenant
	public String deleteTenant(String tenantId, String authHeader) {
		String apiUrl = cognosApiUrl + "/tenants?tenant_id" + tenantId;
		HttpHeaders headers = initializeHeader(authHeader);
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.DELETE, entity, String.class);
		String responseBody = response.getBody();
		if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
			System.out.println("Successfully delete tenanat " + responseBody);
		}else {
			System.out.println("Something is error duriong deleting tenanat " + responseBody);		
		}
		return responseBody;
    }
	
}
