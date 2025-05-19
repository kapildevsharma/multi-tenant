
package com.kapil.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TenantManagerApplication {
	private static final Logger logger = LoggerFactory.getLogger(TenantManagerApplication.class);
	
    public static void main(String[] args) {
    	logger.info("Application started");
        SpringApplication.run(TenantManagerApplication.class, args);
    }
}
