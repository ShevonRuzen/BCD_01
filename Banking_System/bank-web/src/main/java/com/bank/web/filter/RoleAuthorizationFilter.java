package com.bank.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.logging.Logger;

public class RoleAuthorizationFilter implements Filter {

    private static final Logger logger = Logger.getLogger(RoleAuthorizationFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        String role = (session != null) ? (String) session.getAttribute("role") : null;
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());

        if (path.startsWith("/admin/")) {
            if ("ADMIN".equals(role)) {
                chain.doFilter(request, response);
            } else {
                logger.warning("Unauthorized access attempt to " + path + " by user role: " + role);
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Admin role required.");
            }
        } else if (path.startsWith("/dashboard") || path.startsWith("/transfer") || path.startsWith("/user/")) {
            if ("USER".equals(role)) {
                chain.doFilter(request, response);
            } else if ("ADMIN".equals(role) && path.startsWith("/dashboard")) {
                // If ADMIN tries to access /dashboard, block them or redirect them to their dashboard
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/admin/dashboard");
            } else {
                logger.warning("Unauthorized access attempt to " + path + " by role: " + role);
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: User role required.");
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {}
}
