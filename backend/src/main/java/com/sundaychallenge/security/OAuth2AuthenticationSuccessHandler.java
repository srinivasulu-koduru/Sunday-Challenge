package com.sundaychallenge.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

/**
 * Custom Authentication Success Handler that persists the SecurityContext
 * into the HTTP Session and redirects users based on their authority:
 * - ROLE_ADMIN -> admin-dashboard.html
 * - ROLE_STUDENT -> student-dashboard.html
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final String frontendUrl;
    private final SecurityContextRepository securityContextRepository;

    public OAuth2AuthenticationSuccessHandler(@Value("${app.frontend.url:http://localhost:5500}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
        this.securityContextRepository = new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        // Explicitly store SecurityContext in SecurityContextRepository (HttpSession)
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authentication);
        securityContextRepository.saveContext(context, request, response);

        HttpSession session = request.getSession(false);
        String sessionId = (session != null) ? session.getId() : "NO_SESSION";

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        boolean isAdmin = authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        String targetUrl;
        if (isAdmin) {
            targetUrl = frontendUrl + "/pages/admin-dashboard.html";
        } else {
            targetUrl = frontendUrl + "/pages/student-dashboard.html";
        }

        log.info("[DEBUG] OAuth2 Login Success! Principal: {}, Authorities: {}, IsAdmin: {}, Session ID: {}",
                authentication.getName(), authorities, isAdmin, sessionId);
        log.info("[DEBUG] Redirecting authenticated browser to: {}", targetUrl);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
