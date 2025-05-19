package com.kapil.tenant.customFilter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CognosAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(CognosAuthenticationFilter.class);

	@Value("${cognos.api.url}") // The base URL for the Cognos API
	private String cognosApiUrl;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		logger.info("In Filter, Request URI : " + request.getRequestURI());

		String authHeader = request.getHeader("IBM-BA-Authorization");
		logger.info("CognosAuthenticationFilter : authHeader, " + authHeader);
		if (authHeader == null || !authHeader.startsWith("CAM ")) {
			logger.warn("Missing or malformed Cognos Authorization header.");
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or malformed Cognos Authorization header");
			return;
		}

		String token = authHeader.substring(4);

		// Call Cognos REST API to validate token
		try {
			if (validateCognosToken(token)) {
				var authentication = new UsernamePasswordAuthenticationToken("cognosUser", null,
						Collections.emptyList());
				SecurityContextHolder.getContext().setAuthentication(authentication);
				logger.info("Cognos user authenticated successfully.");
			} else {
				logger.warn("Cognos token validation failed.");
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Cognos token");
				return;
			}
		} catch (Exception e) {
			logger.error("Error validating Cognos token", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error communicating with Cognos API");
			return;
		}

		filterChain.doFilter(request, response);
	}

	private boolean validateCognosToken(String token)  throws IOException, InterruptedException {

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(cognosApiUrl + "/session"))
				.header("IBM-BA-Authorization", "CAM " + token).GET().build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		logger.info("Cognos validation response: " + response.statusCode());
		return response.statusCode() == 200;
	}
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
	    String path = request.getRequestURI();
	    return path.startsWith("/swagger-ui")
	        || path.startsWith("/v3/api-docs")
	        || path.startsWith("/swagger-resources")
	        || path.startsWith("/webjars")
	        || path.startsWith("/actuator");
	}
	
}