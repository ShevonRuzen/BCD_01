package com.bank.web.servlet;

import com.bank.core.entity.Account;
import com.bank.core.entity.User;
import com.bank.core.entity.Transfer;
import com.bank.core.entity.AuditLog;
import com.bank.ejb.service.UserSessionBean;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    @Inject
    private UserSessionBean userService;

    @PersistenceContext(unitName = "BankPU")
    private EntityManager em;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        String email = (String) session.getAttribute("email");
        User user = userService.findByEmail(email);

        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // Fetch datasets for dashboard tables
        List<User> allUsers = em.createQuery("SELECT u FROM User u", User.class).getResultList();
        List<Account> allAccounts = em.createQuery("SELECT a FROM Account a", Account.class).getResultList();
        List<Transfer> allTransfers = em.createQuery("SELECT t FROM Transfer t ORDER BY t.id DESC", Transfer.class).getResultList();
        List<AuditLog> auditLogs = em.createQuery("SELECT a FROM AuditLog a ORDER BY a.id DESC", AuditLog.class).setMaxResults(100).getResultList();

        // Compute stats dynamically from the datasets
        long totalUsers = allUsers.size();
        long totalAccounts = allAccounts.size();
        BigDecimal totalBalance = allAccounts.stream()
                                            .map(Account::getBalance)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);

        req.setAttribute("user", user);
        req.setAttribute("allUsers", allUsers);
        req.setAttribute("allAccounts", allAccounts);
        req.setAttribute("allTransfers", allTransfers);
        req.setAttribute("auditLogs", auditLogs);
        
        req.setAttribute("totalUsers", totalUsers);
        req.setAttribute("totalAccounts", totalAccounts);
        req.setAttribute("totalBalance", totalBalance);

        String page = req.getParameter("page");
        if (page == null) {
            page = "home";
        }
        req.setAttribute("activeTab", page);

        req.getRequestDispatcher("/WEB-INF/jsp/admin_dashboard.jsp").forward(req, resp);
    }
}
