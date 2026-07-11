package com.techmart.api;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Server-side security filter that prevents non-customer users (including admins)
 * from accessing any URL under /user/.
 */
@WebFilter("/user/*")
public class CustomerAccessFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        HttpSession session = httpReq.getSession(false);
        String role = session != null ? (String) session.getAttribute("loggedInUserRole") : null;

        if (role == null) {
            // Not logged in — redirect to login page
            String contextPath = httpReq.getContextPath();
            httpResp.sendRedirect(contextPath + "/login.html");
            return;
        }
        if (!"CUSTOMER".equals(role)) {
            // Logged in but not a customer — redirect to login page.
            // This prevents the tab from suddenly loading the admin dashboard.
            String contextPath = httpReq.getContextPath();
            httpResp.sendRedirect(contextPath + "/login.html");
            return;
        }
        // Is customer — allow through
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}
