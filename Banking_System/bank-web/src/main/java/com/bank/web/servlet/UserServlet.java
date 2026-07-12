package com.bank.web.servlet;

import com.bank.core.entity.Account;
import com.bank.core.entity.User;
import com.bank.ejb.service.AccountSessionBean;
import com.bank.ejb.service.MoneyRequestSessionBean;
import com.bank.ejb.service.TransferSessionBean;
import com.bank.ejb.service.UserSessionBean;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;

@WebServlet(urlPatterns = {
    "/user/profile/update", 
    "/user/accounts/deposit", 
    "/user/transfers/cancel",
    "/user/transfers/request",
    "/user/transfers/approve",
    "/user/transfers/reject"
})
public class UserServlet extends HttpServlet {

    @Inject
    private UserSessionBean userService;

    @Inject
    private AccountSessionBean accountService;

    @Inject
    private TransferSessionBean transferService;

    @Inject
    private MoneyRequestSessionBean requestService;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        String email = (String) session.getAttribute("email");
        Long userId = (Long) session.getAttribute("userId");
        String path = req.getServletPath();
        
        String redirectTab = "home";

        try {
            if ("/user/profile/update".equals(path)) {
                redirectTab = "profile";
                String name = req.getParameter("name");
                String password = req.getParameter("password");
                String confirmPassword = req.getParameter("confirmPassword");

                if (password != null && !password.trim().isEmpty() && !password.equals(confirmPassword)) {
                    throw new Exception("Passwords do not match.");
                }

                userService.updateProfile(userId, name, password);
                req.getSession().setAttribute("msg", "Profile updated successfully.");

            } else if ("/user/accounts/deposit".equals(path)) {
                redirectTab = "deposit";
                Long accountId = Long.parseLong(req.getParameter("accountId"));
                BigDecimal amount = new BigDecimal(req.getParameter("amount"));
                
                accountService.deposit(accountId, amount);
                req.getSession().setAttribute("msg", "Successfully deposited $" + amount + " to your account.");

            } else if ("/user/transfers/cancel".equals(path)) {
                redirectTab = "transfers";
                Long transferId = Long.parseLong(req.getParameter("transferId"));
                transferService.cancelScheduledTransfer(transferId, email);
                req.getSession().setAttribute("msg", "Scheduled transfer #" + transferId + " cancelled successfully.");

            } else if ("/user/transfers/request".equals(path)) {
                redirectTab = "transfers";
                String payerAccNum = req.getParameter("payerAccountNumber");
                BigDecimal amount = new BigDecimal(req.getParameter("amount"));
                String desc = req.getParameter("description");

                requestService.createRequest(email, payerAccNum, amount, desc);
                req.getSession().setAttribute("msg", "Payment request of $" + amount + " sent successfully.");

            } else if ("/user/transfers/approve".equals(path)) {
                redirectTab = "transfers";
                Long requestId = Long.parseLong(req.getParameter("requestId"));
                requestService.approveRequest(requestId, email);
                req.getSession().setAttribute("msg", "Payment request approved and money transferred.");

            } else if ("/user/transfers/reject".equals(path)) {
                redirectTab = "transfers";
                Long requestId = Long.parseLong(req.getParameter("requestId"));
                requestService.rejectRequest(requestId, email);
                req.getSession().setAttribute("msg", "Payment request declined.");
            }
        } catch (Exception e) {
            req.getSession().setAttribute("err", e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/dashboard?page=" + redirectTab);
    }
}
