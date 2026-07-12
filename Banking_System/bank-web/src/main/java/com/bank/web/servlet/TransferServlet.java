package com.bank.web.servlet;

import com.bank.core.exception.BusinessRuleViolationException;
import com.bank.ejb.service.TransferSessionBean;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@WebServlet(urlPatterns = {"/transfer", "/transfer/schedule"})
public class TransferServlet extends HttpServlet {

    @Inject
    private TransferSessionBean transferService;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();

        try {
            String fromAccNum = req.getParameter("fromAccountNumber");
            String toAccNum = req.getParameter("toAccountNumber");
            BigDecimal amount = new BigDecimal(req.getParameter("amount"));
            String desc = req.getParameter("description");

            if ("/transfer/schedule".equals(path)) {
                LocalDateTime scheduledTime = LocalDateTime.parse(req.getParameter("scheduledTime"));
                transferService.scheduleTransfer(fromAccNum, toAccNum, amount, desc, scheduledTime);
                req.getSession().setAttribute("msg", "Transfer scheduled successfully.");
            } else {
                transferService.executeInstantTransfer(fromAccNum, toAccNum, amount, desc);
                req.getSession().setAttribute("msg", "Instant transfer completed successfully.");
            }
        } catch (BusinessRuleViolationException | SecurityException e) {
            req.getSession().setAttribute("err", e.getMessage());
        } catch (Exception e) {
            req.getSession().setAttribute("err", "Transfer failed: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/dashboard");
    }
}
