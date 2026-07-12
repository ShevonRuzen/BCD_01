package com.bank.web.servlet;

import com.bank.core.entity.Account;
import com.bank.core.entity.User;
import com.bank.ejb.service.AccountSessionBean;
import com.bank.ejb.service.UserSessionBean;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

@WebServlet(urlPatterns = {
    "/admin/users/create", 
    "/admin/accounts/toggle", 
    "/admin/accounts/create"
})
public class AdminServlet extends HttpServlet {

    @Inject
    private UserSessionBean userService;

    @Inject
    private AccountSessionBean accountService;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        String redirectTab = "home";

        try {
            if ("/admin/users/create".equals(path)) {
                redirectTab = "users";
                String name = req.getParameter("name");
                String email = req.getParameter("email");
                String password = req.getParameter("password");
                String roleStr = req.getParameter("role");

                User.UserType type = User.UserType.valueOf(roleStr);
                User registeredUser = userService.registerUser(name, email, password, type);
                
                // If standard USER is registered, automatically initialize their 8-digit bank account with $100 starter balance
                if (type == User.UserType.USER) {
                    accountService.createAccount(registeredUser, new BigDecimal("100.00"));
                }
                
                req.getSession().setAttribute("msg", "Successfully registered new " + roleStr + " user: " + email);

            } else if ("/admin/accounts/toggle".equals(path)) {
                redirectTab = "accounts";
                Long accId = Long.parseLong(req.getParameter("accountId"));
                String statusStr = req.getParameter("status");

                Account.Status status = Account.Status.valueOf(statusStr);
                accountService.toggleAccountStatus(accId, status);
                req.getSession().setAttribute("msg", "Account #" + accId + " status updated to " + statusStr);

            } else if ("/admin/accounts/create".equals(path)) {
                redirectTab = "accounts";
                String email = req.getParameter("targetEmail");
                BigDecimal initialBalance = new BigDecimal(req.getParameter("initialBalance"));

                User owner = userService.findByEmail(email);
                if (owner == null) {
                    throw new Exception("User not found with email: " + email);
                }

                accountService.createAccount(owner, initialBalance);
                req.getSession().setAttribute("msg", "Successfully created new account for user: " + email);
            }
        } catch (Exception e) {
            req.getSession().setAttribute("err", "Admin operation failed: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/admin/dashboard?page=" + redirectTab);
    }
}
