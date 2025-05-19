# Mutli Tenant Manager
Swagger URL : http://localhost:8080/swagger-ui/index.html

1. Create Tenant
POST /tenants

Request:
{
  "tenantId": "tenant001",
  "schemaName": "tenant_tenant001"
}
Response:
{
  "message": "Tenant created successfully",
  "tenantId": "tenant001",
  "status": "ACTIVE"
}
📌 2. Reset Tenant
PUT /tenants/tenant001/reset

Request:
{
  "force": true
}
Response:
{
  "message": "Tenant tenant001 has been reset"
}
📌 3. Refresh Tenant
PUT /tenants/tenant001/refresh

Response:
{
  "message": "Tenant tenant001 data has been refreshed"
}
📌 4. Get Tenant
GET /tenants/tenant001

Response:
{
  "tenantId": "tenant001",
  "schemaName": "tenant_tenant001",
  "status": "ACTIVE",
  "createdAt": "2025-04-30T10:23:00Z"
}
📌 5. List All Tenants
GET /tenants

Response:

[
  {
    "tenantId": "tenant001",
    "schemaName": "tenant_tenant001",
    "status": "ACTIVE"
  },
  {
    "tenantId": "tenant002",
    "schemaName": "tenant_tenant002",
    "status": "INACTIVE"
  }
]

