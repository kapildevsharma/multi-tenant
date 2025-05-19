
package com.kapil.tenant.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kapil.tenant.dto.TenantRequest;
import com.kapil.tenant.dto.TenantResponse;
import com.kapil.tenant.kafka.KafkaActionService;
import com.kapil.tenant.model.Tenant;
import com.kapil.tenant.service.TenantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Tenant Management", description = "Operations related to tenant management")
@RestController
@RequestMapping("/tenants")
public class TenantController {

    @Autowired
    private TenantService tenantService;
    
    @Autowired
    private KafkaActionService kafkaActionService;

    @Operation(summary = "Create a new tenant")
    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@RequestBody TenantRequest request,  @RequestHeader("IBM-BA-Authorization") String authHeader) {
    	 Tenant createdTenant = tenantService.createTenant(request, authHeader);
    	 TenantResponse response = new TenantResponse(
    	            "Tenant created successfully",
    	            createdTenant.getTenantId(),
    	            createdTenant.getStatus().name()
    	        );
    	kafkaActionService.triggerTenantAction("tenant-create", request.getTenantId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Assign product to an tenant")
	@PostMapping("/{tenantId}/add-product")
	public ResponseEntity<String> assignProductToTenant(@RequestParam String productName, @PathVariable String tenantId,
			@RequestHeader("IBM-BA-Authorization") String authHeader) throws JsonProcessingException {
		String response = "";
		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("tenantId", tenantId);
		payload.put("productName", productName);
		boolean isProductAssign = tenantService.assignProductToTenant(tenantId, productName, authHeader);
		if (isProductAssign) {
			response = "Product " + productName + " assigned successfully to tenant(" + tenantId + ")";
			payload.put("action", "PRODUCT_ADDED");
		} else {
			response = "Product " + productName + " does not assign to tenant(" + tenantId + ")";
			payload.put("action", "PRODUCT_NOT_ADDED");
		}
		payload.put("timestamp", System.currentTimeMillis());

		kafkaActionService.sendNotificationTenant("tenant-add-product", payload);
		return ResponseEntity.ok(response);
	}

    @Operation(summary = "Deactivate product to an tenant")
   	@PostMapping("/{tenantId}/deactive-product")
   	public ResponseEntity<String> deactiveProductTenant(@RequestParam String productName, @PathVariable String tenantId,
   			@RequestHeader("IBM-BA-Authorization") String authHeader) throws JsonProcessingException {
   		String response = "";
   		Map<String, Object> payload = new HashMap<String, Object>();
   		payload.put("tenantId", tenantId);
   		payload.put("productName", productName);
   		boolean isProductAssign = tenantService.deactiveProductTenant(tenantId, productName, authHeader);
   		if (isProductAssign) {
   			response = "Product " + productName + " is deactivated successfully for tenant(" + tenantId + ")";
   			payload.put("product_status", "PRODUCT_DEACTIVATE");
   		} else {
   			response = "Product " + productName + " does not deactivated for tenant(" + tenantId + ")";
   			payload.put("product_status", "PRODUCT_NOT_DEACTIVATE");
   		}
   		payload.put("timestamp", System.currentTimeMillis());

   		kafkaActionService.sendNotificationTenant("tenant-product-deactivate", payload);
   		return ResponseEntity.ok(response);
   	}

    
    @Operation(summary = "Update an existing tenant by its tenant id")
    @PutMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> resetTenant(@RequestBody Tenant reqeustBody, @PathVariable String tenantId, @RequestHeader("IBM-BA-Authorization") String authHeader) throws JsonProcessingException {
       
        Tenant updatedTenant = tenantService.updateTenant(tenantId, authHeader, reqeustBody);
        TenantResponse response = new TenantResponse(
                "Tenant " + tenantId + " has been updated", updatedTenant.getTenantId(),
                updatedTenant.getStatus().name()
            );
        kafkaActionService.triggerTenantAction("tenant-update", tenantId);
        return ResponseEntity.ok(response);
        
    }

    @Operation(summary = "Get tenant information")
    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable String tenantId, @RequestHeader("IBM-BA-Authorization") String authHeader) {
        Tenant tenant = tenantService.getTenant(tenantId, authHeader);
        kafkaActionService.triggerTenantAction("get Tenant details", tenantId);
        
        TenantResponse response = new TenantResponse("Tenant " + tenantId + " is found successfully",
				tenant.getTenantId(), tenant.getStatus().name());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get all existing tenant inforamtion")
    @GetMapping
    public ResponseEntity<List<Tenant>> listTenants(@RequestHeader("IBM-BA-Authorization") String authHeader) {
    	// trigger to Kafka
        kafkaActionService.triggerTenantAction("get All Tenant details", "all");
        return ResponseEntity.ok(tenantService.getAllTenants());
    }
    
}
