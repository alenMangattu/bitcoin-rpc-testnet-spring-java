package com.coinbase.cb.configuration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        
        // Skip JWT validation for public endpoints
        if (path.contains("/api/v1/auth/login") || 
            path.contains("/api/v1/auth/register") || 
            path.contains("/helloworld")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        
        logger.debug("Processing request for path: {}", request.getRequestURI());
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("No valid authorization header found");
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String userEmail = jwtService.getEmailFromToken(jwt);
        
        logger.debug("Extracted email from token: {}", userEmail);
        
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                boolean isValid = jwtService.validateJwtToken(jwt);
                logger.debug("JWT token validation result: {}", isValid);
                
                if (isValid) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                    logger.debug("Successfully loaded user details for email: {}", userEmail);
                    
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("Successfully set authentication for user: {}", userEmail);
                }
            } catch (UsernameNotFoundException e) {
                logger.error("User not found with email: {} - Exception: {}", userEmail, e.getMessage());
            } catch (Exception e) {
                logger.error("Error processing authentication: {}", e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
} 