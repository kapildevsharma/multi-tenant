package com.kapil.tenant.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapil.tenant.dto.TenantRequest;
import com.kapil.tenant.exceptionHandler.CustomException;
import com.kapil.tenant.model.Tenant;
import com.kapil.tenant.repository.TenantRepository;

@Service
public class TenantService {

	@Autowired
	private TenantDatabaseService tenantDatabaseService;
	
	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private CognosEnvRestManagerClient cognosEnvRestManagerClient;
	
	@Autowired
	private CognosApiOkHttpClientService apiOkHttpClientService;
	
	@Autowired
	private ObjectMapper mapper;
	
	public Tenant createTenant(TenantRequest tenantRequest, String authHeader) {
		
		String tenantName = tenantRequest.getTenantName();
		tenantRequest.setNameSpace("tenant_" + tenantName);
		Tenant tenant = null;
		try {
			tenant = tenantDatabaseService.createTenant(tenantRequest);
			
			Map<String, Object> request = Map.of("defaultName", tenantRequest.getTenantName(), "tenantId", tenant.getTenantId(), "disabled",true);
			String payload= null;
			try {
				payload = mapper.writeValueAsString(request);
			}  catch (JsonProcessingException e) {
				throw new CustomException(TenantService.class + " Exception in reqeust mapper " + request);
			}
			
		// comment below code to run database operation	
		//	boolean createTeamFolder = true;
		//	boolean tenantCreated = cognosEnvRestManagerClient.setupTenantEnv(payload, createTeamFolder, authHeader);
		//	System.out.println("cognosClient, Tenant Created : "+ tenantCreated);
		//	String result = apiOkHttpClientService.createTenant(payload, createTeamFolder, authHeader);
		//	System.out.println("cognosApiService Tenant Created : "+ result);

		} catch (Exception e) {
			throw new CustomException(TenantService.class + " Exception " + e.getMessage());
		}
		return tenant;

	}

	public boolean assignProductToTenant(String tenantId, String productName, String authHeader) throws JsonMappingException, JsonProcessingException {
		Map<String, Object> response = apiOkHttpClientService.assignProductToTenant(tenantId, productName, authHeader);
		boolean isProductAdded = (boolean) response.get("result");

		if (isProductAdded) {
			try {
				tenantDatabaseService.assignProductToTenantDB(tenantId, productName, response);
			} catch (SQLException | JsonProcessingException e) {
				throw new CustomException(TenantService.class + " During assigning Product To Tenant in Database,  Exception " + e.getMessage());
			}
		}
		return isProductAdded;
	}
	
	public boolean deactiveProductTenant(String tenantId, String productName, String authHeader) throws JsonMappingException, JsonProcessingException {
		String tenantMetaData = tenantDatabaseService.getTenantMetaData(tenantId);
		Map<String, Object> response = apiOkHttpClientService.deactiveProductTenant(tenantId, productName, authHeader, tenantMetaData);
		boolean isProductAdded = (boolean) response.get("result");

		if (isProductAdded) {
			try {
				tenantDatabaseService.deactivateProductTenantDB(tenantId, productName, response);
			} catch (SQLException | JsonProcessingException e) {
				throw new CustomException(TenantService.class + " During deactivating Tenant Product in Database,  Exception " + e.getMessage());
			}
		}
		return isProductAdded;
	}
	
	public Tenant updateTenant(String tenantId, String authHeader, Tenant reqeustBody) throws JsonProcessingException {
		Tenant udpateTenant = tenantDatabaseService.updateTenant(tenantId, reqeustBody);
		boolean tenantUpdated = cognosEnvRestManagerClient.updateTenant(tenantId, reqeustBody, authHeader);
		System.out.println("cognosClient, Tenant Updated : "+ tenantUpdated);
		String result = apiOkHttpClientService.updateTenant(tenantId, reqeustBody, authHeader);
		System.out.println("cognosApiService Tenant Updated : "+ result);

		return udpateTenant;
	}

	public String deleteTenant(String tenantId, String authHeader) throws JsonProcessingException {
		
		tenantDatabaseService.deleteTenant(tenantId);
		
		String result = cognosEnvRestManagerClient.deleteTenant(tenantId, authHeader);
		System.out.println("cognosClient, Tenant deleted : "+ result);
		result = apiOkHttpClientService.deleteTenant(tenantId, authHeader);
		System.out.println("cognosApiService Tenant deleted : "+ result);

		return result;
	}
	
	public Tenant getTenant(String tenantId, String authHeader) {
	//	cognosEnvRestManagerClient.getTenantEnv(tenantId,authHeader);
	//	apiOkHttpClientService.getTenant(tenantId, authHeader);
		
		return tenantRepository.findByTenantId(tenantId)
				.orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
	}
	
	// LIST ALL TENANTS
    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }
    
 // LIST ALL API Keys
    public String getAPIKeys(String authHeader) {
    	cognosEnvRestManagerClient.getAPIKeys(authHeader);
    	return apiOkHttpClientService.getAPIKeys(authHeader);
    }
}
