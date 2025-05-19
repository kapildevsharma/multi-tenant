package com.kapil.tenant.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapil.tenant.config.FlywayService;
import com.kapil.tenant.dto.TenantRequest;
import com.kapil.tenant.model.Status;
import com.kapil.tenant.model.Tenant;
import com.kapil.tenant.repository.TenantRepository;
import com.kapil.tenant.utility.TenantUtils;

@Component
public class TenantDatabaseService {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private FlywayService flywayService;

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private ObjectMapper mapper;

	public Tenant createTenant(TenantRequest tenantRequest) {
		String tenantName = tenantRequest.getTenantName();
		String schema = "tenant_" + tenantName;
		jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
		flywayService.runMigrations(schema);

		Tenant tenant = new Tenant(tenantName, schema, Status.CREATE, tenantRequest.getNameSpace(),
				tenantRequest.getUserId());
		tenantRepository.save(tenant);

		System.out.println("Tenant saved in database: ");

		return tenant;
	}

	public boolean assignProductToTenantDB(String tenantId, String productName, Map<String, Object> response)
			throws SQLException, JsonProcessingException {
		Tenant tenant = tenantRepository.findByTenantId(tenantId)
				.orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

		String tenantName = tenant.getTenantId();

		Connection dbConnection = DriverManager.getConnection(flywayService.getTenantJdbcURL(tenant.getSchemaName()),
				flywayService.getDbUsername(), flywayService.getDbPassword());

		String insertSQL = "INSERT INTO tenant_product (tenant_id, product_name, product_status) VALUES (?, ?, ?)";
		try (PreparedStatement stmt = dbConnection.prepareStatement(insertSQL)) {
			stmt.setString(1, tenantId);
			stmt.setString(2, productName);
			stmt.setString(3, Status.ACTIVATE.name());
			stmt.executeUpdate();
		}
		String selectSQL = "Select * from tenant_metadata where tenant_id =?";
		try (PreparedStatement stmt = dbConnection.prepareStatement(selectSQL)) {
			stmt.setString(1, tenantId);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				System.out.println("Update tenant_metadata for  " + tenantId);
				String updateSQL = "UPDATE tenant_metadata SET metadata = ?, updated_at = ? WHERE tenant_id = ?";
				try (PreparedStatement updateStmt = dbConnection.prepareStatement(updateSQL)) {
					updateStmt.setString(1, mapper.writeValueAsString(response));
					updateStmt.setString(2, TenantUtils.getLocalDateTime());
					updateStmt.setString(3, tenantId);
					updateStmt.executeUpdate();
				}

			} else {
				insertSQL = "INSERT INTO tenant_metadata (tenant_id, tenant_name, tenant_status, tenant_namespace, metadata) VALUES (?, ?, ?, ?, ?)";
				try (PreparedStatement insertStmt = dbConnection.prepareStatement(insertSQL)) {
					insertStmt.setString(1, tenantId);
					insertStmt.setString(2, tenantName);
					insertStmt.setString(3, tenant.getStatus().name());
					insertStmt.setString(4, tenant.getNamespace());
					insertStmt.setString(5, mapper.writeValueAsString(response));
					insertStmt.executeUpdate();
				}
			}
		}

		return true;
	}

	public boolean deactivateProductTenantDB(String tenantId, String productName, Map<String, Object> response)
			throws SQLException, JsonProcessingException {
		Tenant tenant = tenantRepository.findByTenantId(tenantId)
				.orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

		Connection dbConnection = DriverManager.getConnection(flywayService.getTenantJdbcURL(tenant.getSchemaName()),
				flywayService.getDbUsername(), flywayService.getDbPassword());

		String selectProdcutSQL = "Select * from tenant_product where tenant_id =?";
		try (PreparedStatement stmt = dbConnection.prepareStatement(selectProdcutSQL)) {
			stmt.setString(1, tenantId);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String status = rs.getString("product_status");
				if (status.equals(Status.DEACTIVATE.name())) {
					System.out.println("Product" + productName + " already deactivated for tenant " + tenantId);
					return false;
				} else {
					System.out.println("Update tenant_product for  " + tenantId);
					String updateProductSQL = "UPDATE tenant_product SET product_status = ?, modified = ? WHERE tenant_id = ?";
					try (PreparedStatement updateStmt = dbConnection.prepareStatement(updateProductSQL)) {
						updateStmt.setString(1, Status.DEACTIVATE.name());
						updateStmt.setString(2, TenantUtils.getLocalDateTime());
						updateStmt.setString(3, tenantId);
						updateStmt.executeUpdate();
					}
				}
			}
		}

		String selectSQL = "Select * from tenant_metadata where tenant_id =?";
		try (PreparedStatement stmt = dbConnection.prepareStatement(selectSQL)) {
			stmt.setString(1, tenantId);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				System.out.println("Update tenant_metadata for  " + tenantId);
				String updateSQL = "UPDATE tenant_metadata SET metadata = ?, updated_at = ? WHERE tenant_id = ?";
				try (PreparedStatement updateStmt = dbConnection.prepareStatement(updateSQL)) {
					updateStmt.setString(1, mapper.writeValueAsString(response));
					updateStmt.setString(2, TenantUtils.getLocalDateTime());
					updateStmt.setString(3, tenantId);
					updateStmt.executeUpdate();
				}

			}
		}

		return true;
	}

	public String getTenantMetaData(String tenantId) {
		Tenant tenant = tenantRepository.findByTenantId(tenantId)
				.orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

		String metadata = "";
		Connection dbConnection;
		try {
			dbConnection = DriverManager.getConnection(flywayService.getTenantJdbcURL(tenant.getSchemaName()),
					flywayService.getDbUsername(), flywayService.getDbPassword());
			String selectSQL = "Select * from tenant_metadata where tenant_id =?";
			try (PreparedStatement stmt = dbConnection.prepareStatement(selectSQL)) {
				stmt.setString(1, tenantId);
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					metadata = rs.getString("metadata");
					System.out.println("Get tenant_metadata for  " + tenantId);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return metadata;
	}

	public Tenant updateTenant(String tenantId, Tenant reqeustBody) throws JsonProcessingException {
		Tenant tenant = tenantRepository.findByTenantId(tenantId)
				.orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

		jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + tenant.getSchemaName());
		jdbcTemplate.execute("CREATE SCHEMA " + reqeustBody.getSchemaName());
		flywayService.runMigrations(reqeustBody.getSchemaName());
		tenant.setStatus(Status.UPDATE);
		Tenant udpateTenant = tenantRepository.save(tenant);
		return udpateTenant;
	}

	public void deleteTenant(String tenantId) throws JsonProcessingException {
		Tenant tenant = tenantRepository.findByTenantId(tenantId)
				.orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
		String schema = "tenant_" + tenantId;
		jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schema);
		flywayService.runMigrations(schema);
		tenant.setStatus(Status.DELETE);
		tenantRepository.delete(tenant);
	}

	public Tenant getTenant(String tenantId, String authHeader) {
		return tenantRepository.findByTenantId(tenantId)
				.orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
	}

	public List<Tenant> getAllTenants() {
		return (List<Tenant>) tenantRepository.findAll();
	}

}
