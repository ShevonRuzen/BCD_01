package com.bank.web.servlet;

import com.bank.core.entity.User;
import com.bank.core.exception.BusinessRuleViolationException;
import com.bank.ejb.service.UserSessionBean;
import com.bank.ejb.service.AccountSessionBean;
import com.bank.core.entity.Account;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    @Inject
    private UserSessionBean userService;

    @Inject
    private AccountSessionBean accountService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        if (password == null || password.length() < 8) {
            req.setAttribute("error", "Password must be at least 8 characters long.");
            req.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(req, resp);
            return;
        }

        if (!password.equals(confirmPassword)) {
            req.setAttribute("error", "Passwords do not match.");
            req.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(req, resp);
            return;
        }

        try {
            // Register as standard USER only
            User user = userService.registerUser(name, email, password, User.UserType.USER);

            // Auto-create a default account for them with $100.00 initial balance
            accountService.createAccount(user, new BigDecimal("100.00"));

            // Log them in immediately
            HttpSession session = req.getSession(true);
            session.setAttribute("userId", user.getId());
            session.setAttribute("email", user.getEmail());
            session.setAttribute("role", user.getUserType().name());

            resp.sendRedirect(req.getContextPath() + "/dashboard");
        } catch (BusinessRuleViolationException e) {
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", "An unexpected system error occurred: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(req, resp);
        }
    }
}
