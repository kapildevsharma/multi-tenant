package com.kapil.tenant.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapil.tenant.exceptionHandler.CustomException;
import com.kapil.tenant.model.Tenant;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class CognosApiOkHttpClientService {

    @Value("${cognos.api.base-url}")
    private String baseUrl;

    @Value("${cognos.api.cam-token}")
    private String authToken;

    @Value("${cognos.api.xsrf-token}")
    private String xsrfToken;
    
    private final OkHttpClient okHttpClient;
   	private final ObjectMapper objectMapper;
   	
    
    public CognosApiOkHttpClientService(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
    }

    public Request getRequestWithHeader(String url, String authHeader, RequestBody requestBody,  String method) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("IBM-BA-Authorization", "CAM " + authToken)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-XSRF-Token", xsrfToken) // modify every session
                .addHeader("Cache-Control", "no-cache");

      
        // Set the HTTP method
        switch (method.toUpperCase()) {
            case "GET" -> builder.get();
            case "POST" -> builder.post(requestBody);
            case "PUT" -> builder.put(requestBody);
            case "DELETE" -> builder.delete(null);
            case "PATCH" -> builder.patch(requestBody);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        Request request = builder.build();

    	return request;
    }
    
    public String createTenant(String requestBodyJson, boolean createTeamFolder,  String authHeader) {
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(requestBodyJson, mediaType);

        String url = baseUrl + "/api/v1/tenants?create_team_folder=" + createTeamFolder;
        Request request = getRequestWithHeader(url, authHeader, body, "POST");
        System.out.println("Cognos API Servcie to create the tenant: " + request.url().toString());
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response: " + response);
            }
            return response.body().string();
        }catch(IOException e ) {
        	return e.getMessage();
        }
    }
    
    public Map<String, Object> assignProductToTenant(String tenantId, String productName, String authHeader) throws JsonMappingException, JsonProcessingException {
		Map<String, Object> response = new HashMap<>();

		ObjectMapper mapper = new ObjectMapper();
		String tenantDetailsJson = String.format("""
		        {
		          "tenants": [
		            {
		            "tenantID": "%s",
					 "product": "%s",
		              "defaultName": "%s",
		              "disabled": false,
		              "hidden": false,
		              "modificationTime": "%s",
		              "searchPath": "/content/tenant[abc123]",
		              "type": "tenant",
		              "version": 1
		            }
		          ]
		        }
		        """, tenantId, productName,tenantId, System.currentTimeMillis());
		// tenantDetailsJson = getTenant(tenantId, authHeader)

        JsonNode rootNode = mapper.readTree(tenantDetailsJson);
        JsonNode tenantNode = rootNode.get("tenants").get(0);
        // Convert selected fields to Map
        Iterator<Map.Entry<String, JsonNode>> fields = tenantNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            response.put(entry.getKey(), entry.getValue().asText());
        }
		response.put("result", true);
		return response;
    }
    
    public Map<String, Object> deactiveProductTenant(String tenantId, String productName, String authHeader,String tenantMetaData) throws JsonMappingException, JsonProcessingException {
		Map<String, Object> response = new HashMap<>();


	        ObjectMapper mapper = new ObjectMapper();
	        JsonNode rootNode = mapper.readTree(tenantMetaData);

	        Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
	        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if(entry.getKey().equals("product")) {
                String product = entry.getValue().asText().replace(productName, "").trim();
                response.put(entry.getKey(), product);
            }else {
            	 response.put(entry.getKey(), entry.getValue().asText());
            }
        }
		response.put("result", true);
        System.out.println("Modified Response: " + response);
		return response;
    }
    
    public String getTenant(String tenantId, String authHeader) {
    	String url = baseUrl + "/api/v1/tenants?tenant_idr=" + tenantId;
    	Request request = getRequestWithHeader(url, authHeader, null, "GET");
         
        String tenantDetails = "";
        System.out.println("Cognos API Servcie to create the tenant: " + request.url().toString());
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response: " + response);
            }
            tenantDetails = response.body().string();
        }catch(IOException e ) { 
        	tenantDetails = e.getMessage();
        }
        
        System.out.println("Cognos API Servcie to get the tenant detail: " + tenantDetails);
        return tenantDetails;
    }
    
 	public String getAPIKeys(String authHeader) {
 		String url = baseUrl + "/api/v1/security/login_api_keys";
    	Request request = getRequestWithHeader(url, authHeader, null, "GET");

    	String responseBody = "";
    	
    	 try (Response response = okHttpClient.newCall(request).execute()) {
             if (!response.isSuccessful()) {
                 throw new IOException("Unexpected response: " + response);
             }
             responseBody = response.body().string();
             JsonNode root = objectMapper.readTree(responseBody);
             JsonNode keysArray = root.get("keys");
      		// Get the first JSON object in the array
      		JsonNode firstObject = keysArray.get(0);
      		// Print the full object
      		System.out.println(firstObject.toPrettyString());
         }catch(IOException e ) { 
        	 responseBody = e.getMessage();
         }
 		return responseBody;
 	}
 	
 	public String updateTenant(String tenantId, Tenant reqeustTenantBody, String authHeader) throws JsonProcessingException {
      
		String requestBodyJson = objectMapper.writeValueAsString(reqeustTenantBody);
		MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(requestBodyJson, mediaType);

        String url = baseUrl + "/api/v1/tenants?tenant_id" + tenantId;
        Request request = getRequestWithHeader(url, authHeader, body, "PUT");
        System.out.println("Cognos API Servcie to udpate the tenant: " + request.url().toString());
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new CustomException("Unexpected response: " + response);
            }
            return response.body().string();
        }catch(IOException e ) {
        	throw new CustomException(CognosApiOkHttpClientService.class+ " update Tenant" + e.getMessage());

        }
    }
 	
 	public String deleteTenant(String tenantId, String authHeader) {
        String url = baseUrl + "/api/v1/tenants?tenant_id" + tenantId;
        Request request = getRequestWithHeader(url, authHeader, null, "DELETE");
        System.out.println("Cognos API Servcie to delete the tenant: " + request.url().toString());
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new CustomException("Unexpected response: " + response);
            }
            return response.body().string();
        }catch(IOException e ) {
        	throw new CustomException(CognosApiOkHttpClientService.class+ " Delete Tenant" + e.getMessage());
        }
    }
 	
}
