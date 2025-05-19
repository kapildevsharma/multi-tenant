package com.kapil.tenant.dto;

public class TenantResponse {
    private String message;
    private String tenantId;
    private String status;

    public TenantResponse(String message, String tenantId, String status) {
    	this.message = message;
        this.tenantId = tenantId;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getStatus() {
        return status;
    }
}