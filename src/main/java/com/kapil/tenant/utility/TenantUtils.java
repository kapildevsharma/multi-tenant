package com.kapil.tenant.utility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TenantUtils {

    // to extract namespace value from a string like: "namespace=LDAP_ABC"
    public static String extractNamespace(String text) {
        String[] parts = text.split("=");
        if (parts.length == 2) {
            return parts[1].trim();
        }
        return text.trim(); // fallback
    }
    
    // to extract TenantID from "TenantID=T001" style string
    public String extractValue(String input, String key) {
        String[] lines = input.split("[\\r\\n]+"); // handle multiline
        for (String line : lines) {
            if (line.toLowerCase().contains(key.toLowerCase() + "=")) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    return parts[1].trim();
                }
            }
        }
        return null;
    }
    
    public static boolean containsTenantId(String text, String tenantId) {
        if (text == null) return false;
        return text.toLowerCase().contains("tenantid=" + tenantId.toLowerCase());
    }
    
    public static Map<String, Object> convertWithJackson(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
    }
    
    public static String getLocalDateTime() {
        // Get current LocalDateTime
        LocalDateTime currentTime = LocalDateTime.now();

        // Format the current time to a string in the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String timestamp = currentTime.format(formatter);

        // Print the timestamp
        System.out.println("Current Timestamp: " + timestamp);
        return timestamp;
    }
}
