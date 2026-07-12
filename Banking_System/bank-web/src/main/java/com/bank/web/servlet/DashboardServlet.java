package com.bank.web.servlet;

import com.bank.core.entity.Account;
import com.bank.core.entity.MoneyRequest;
import com.bank.core.entity.Transfer;
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
import java.util.ArrayList;
import java.util.List;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    @Inject private UserSessionBean        userService;
    @Inject private AccountSessionBean     accountService;
    @Inject private TransferSessionBean    transferService;
    @Inject private MoneyRequestSessionBean requestService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        String email = (String) session.getAttribute("email");
        User user = userService.findByEmail(email);

        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        List<Account> accounts = accountService.findAccountsByUser(user);
        req.setAttribute("user", user);
        req.setAttribute("accounts", accounts);

        String accountNumber = accounts.isEmpty() ? null : accounts.get(0).getAccountNumber();

        List<Transfer> transfers         = new ArrayList<>();
        List<Transfer> depositHistory     = new ArrayList<>();
        List<Transfer> scheduledTransfers = new ArrayList<>();
        List<MoneyRequest> allRequests    = new ArrayList<>();

        if (accountNumber != null) {
            transfers         = transferService.getRecentTransfers(accountNumber);
            depositHistory    = transferService.getDepositHistory(accountNumber);
            scheduledTransfers = transferService.getScheduledTransfers(accountNumber);
        }
        allRequests = requestService.getAllRequests(email);

        req.setAttribute("transfers",         transfers);
        req.setAttribute("depositHistory",     depositHistory);
        req.setAttribute("scheduledTransfers", scheduledTransfers);
        req.setAttribute("allRequests",        allRequests);

        // P2P request lists for Transfers tab
        List<MoneyRequest> pendingIncoming = requestService.getIncomingPendingRequests(email);
        List<MoneyRequest> outgoingRequests = requestService.getOutgoingRequests(email);
        req.setAttribute("pendingIncoming",  pendingIncoming);
        req.setAttribute("outgoingRequests", outgoingRequests);

        // Active tab/page
        String page = req.getParameter("page");
        if (page == null) page = "home";
        req.setAttribute("activeTab", page);

        req.getRequestDispatcher("/WEB-INF/jsp/dashboard.jsp").forward(req, resp);
    }
}
