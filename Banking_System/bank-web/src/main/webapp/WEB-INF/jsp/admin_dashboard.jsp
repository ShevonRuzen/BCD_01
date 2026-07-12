<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.core.entity.User" %>
<%@ page import="com.bank.core.entity.Account" %>
<%@ page import="com.bank.core.entity.Transfer" %>
<%@ page import="com.bank.core.entity.AuditLog" %>
<%@ page import="java.util.List" %>
<%@ page import="java.math.BigDecimal" %>
<%
    User loggedInUser = (User) request.getAttribute("user");
    List<User> allUsers = (List<User>) request.getAttribute("allUsers");
    List<Account> allAccounts = (List<Account>) request.getAttribute("allAccounts");
    List<Transfer> allTransfers = (List<Transfer>) request.getAttribute("allTransfers");
    List<AuditLog> auditLogs = (List<AuditLog>) request.getAttribute("auditLogs");

    long totalUsers = (Long) request.getAttribute("totalUsers");
    long totalAccounts = (Long) request.getAttribute("totalAccounts");
    BigDecimal totalBalance = (BigDecimal) request.getAttribute("totalBalance");

    String activeTab = (String) request.getAttribute("activeTab");
    if (activeTab == null) activeTab = "home";

    String msg = (String) session.getAttribute("msg");
    String err = (String) session.getAttribute("err");
    session.removeAttribute("msg");
    session.removeAttribute("err");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Build Bank | Admin Console</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
    <style>
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 1.5rem;
            margin-bottom: 2rem;
        }
        .stat-card {
            background: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 1.5rem;
            box-shadow: 0 1px 3px rgba(0,0,0,0.05);
        }
        .stat-value {
            font-size: 1.8rem;
            font-weight: 600;
            color: var(--primary);
            margin-top: 0.5rem;
        }
        .dashboard-split {
            display: grid;
            grid-template-columns: 2fr 1fr;
            gap: 2rem;
        }
        @media(max-width: 900px) {
            .dashboard-split {
                grid-template-columns: 1fr;
            }
        }
        .table-card {
            background: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 1.5rem;
            margin-bottom: 1.5rem;
            box-shadow: 0 1px 3px rgba(0,0,0,0.05);
        }
        .tab-content {
            display: none;
        }
        .tab-content.active {
            display: block;
        }
        .search-box {
            width: 100%;
            padding: 0.65rem 0.85rem;
            background: #FFFFFF;
            border: 1px solid #CBD5E1;
            border-radius: 6px;
            color: var(--text-dark);
            margin-bottom: 1rem;
            font-size: 0.9rem;
        }
        .search-box:focus {
            outline: none;
            border-color: var(--accent);
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
        }
    </style>
</head>
<body>
    <div class="dashboard-container">
        <!-- Collapsible Navigation Sidebar -->
        <div class="sidebar">
            <div class="sidebar-header">
                <span>Build Bank Admin</span>
            </div>
            <ul class="sidebar-menu">
                <li class="sidebar-item <%= "home".equals(activeTab) ? "active" : "" %>">
                    <a href="?page=home">Overview</a>
                </li>
                <li class="sidebar-item <%= "users".equals(activeTab) ? "active" : "" %>">
                    <a href="?page=users">Customers</a>
                </li>
                <li class="sidebar-item <%= "accounts".equals(activeTab) ? "active" : "" %>">
                    <a href="?page=accounts">Accounts</a>
                </li>
                <li class="sidebar-item <%= "logs".equals(activeTab) ? "active" : "" %>">
                    <a href="?page=logs">Audits & Logs</a>
                </li>
            </ul>
            <div class="sidebar-footer">
                <div style="font-size: 0.8rem; opacity: 0.7; margin-bottom: 0.75rem;">
                    Role: <strong>ADMIN</strong>
                </div>
                <form action="<%= request.getContextPath() %>/logout" method="POST" style="margin:0;">
                    <button type="submit" class="btn" style="background: rgba(255,255,255,0.1); border:1px solid rgba(255,255,255,0.2); color:#fff; width:100%; font-size:0.85rem;">Logout</button>
                </form>
            </div>
        </div>

        <!-- Main Workspace Workspace -->
        <div class="main-workspace">
            <div class="page-header">
                <div>
                    <h2>
                        <% if ("home".equals(activeTab)) { %>Admin Console Overview<% } %>
                        <% if ("users".equals(activeTab)) { %>Customer Directory<% } %>
                        <% if ("accounts".equals(activeTab)) { %>Bank Vault Accounts<% } %>
                        <% if ("logs".equals(activeTab)) { %>System Logs Ledger<% } %>
                    </h2>
                    <div style="color: var(--text-muted); font-size:0.85rem; margin-top:0.25rem;">
                        Logged in as: <strong><%= loggedInUser != null ? loggedInUser.getEmail() : "" %></strong>
                    </div>
                </div>
            </div>

            <% if (msg != null) { %>
                <div class="alert-success"><%= msg %></div>
            <% } %>
            <% if (err != null) { %>
                <div class="error"><%= err %></div>
            <% } %>

            <!-- VIEW: OVERVIEW (HOME) -->
            <div class="tab-content <%= "home".equals(activeTab) ? "active" : "" %>">
                <div class="stats-grid">
                    <div class="stat-card">
                        <div style="color: var(--text-muted); font-size: 0.9rem;">Total Bank Customers</div>
                        <div class="stat-value"><%= totalUsers %></div>
                    </div>
                    <div class="stat-card">
                        <div style="color: var(--text-muted); font-size: 0.9rem;">Active Vault Accounts</div>
                        <div class="stat-value"><%= totalAccounts %></div>
                    </div>
                    <div class="stat-card">
                        <div style="color: var(--text-muted); font-size: 0.9rem;">Cumulative Deposits</div>
                        <div class="stat-value" style="color: var(--success);">$<%= totalBalance %></div>
                    </div>
                </div>

                <div class="card">
                    <h3>Administrative Controls</h3>
                    <p style="color: var(--text-muted); line-height: 1.6;">Welcome to the official Build Bank Admin portal. Use the sidebar navigation on the left to add customers, manage active status policies, open standard accounts, and audit real-time CDI transactions.</p>
                </div>
            </div>

            <!-- VIEW: CUSTOMERS -->
            <div class="tab-content <%= "users".equals(activeTab) ? "active" : "" %>">
                <div class="dashboard-split">
                    <div class="table-card">
                        <h3>Customer Directory</h3>
                        <input type="text" class="search-box" id="userSearch" placeholder="Search by name, email..." onkeyup="filterUsers()">
                        <table id="usersTable">
                            <thead>
                                <tr>
                                    <th>Full Name</th>
                                    <th>Email ID</th>
                                    <th>Authority Role</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% if (allUsers != null) { %>
                                    <% for (User u : allUsers) { %>
                                        <tr>
                                            <td><strong><%= u.getName() %></strong></td>
                                            <td><%= u.getEmail() %></td>
                                            <td><span style="font-weight:600;"><%= u.getUserType() %></span></td>
                                        </tr>
                                    <% } %>
                                <% } %>
                            </tbody>
                        </table>
                    </div>

                    <div>
                        <div class="card">
                            <h3>Register New User</h3>
                            <form action="<%= request.getContextPath() %>/admin/users/create" method="POST">
                                <div class="form-group">
                                    <label>Customer Name</label>
                                    <input type="text" name="name" required placeholder="John Doe">
                                </div>
                                <div class="form-group">
                                    <label>Email Address</label>
                                    <input type="email" name="email" required placeholder="john@domain.com">
                                </div>
                                <div class="form-group">
                                    <label>Access Password</label>
                                    <input type="password" name="password" required placeholder="••••••••">
                                </div>
                                <div class="form-group">
                                    <label>Security Role</label>
                                    <select name="role" required>
                                        <option value="USER">USER (Standard Client)</option>
                                        <option value="ADMIN">ADMIN (System Manager)</option>
                                    </select>
                                </div>
                                <button type="submit" class="btn">Register User</button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>

            <!-- VIEW: ACCOUNTS -->
            <div class="tab-content <%= "accounts".equals(activeTab) ? "active" : "" %>">
                <div class="dashboard-split">
                    <div class="table-card">
                        <h3>Bank Vault Accounts</h3>
                        <input type="text" class="search-box" id="accountSearch" placeholder="Search by Account Number, Owner..." onkeyup="filterAccounts()">
                        <table id="accountsTable">
                            <thead>
                                <tr>
                                    <th>Acc Number</th>
                                    <th>Owner</th>
                                    <th>Balance</th>
                                    <th>Status</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% if (allAccounts != null) { %>
                                    <% for (Account acc : allAccounts) { %>
                                        <tr>
                                            <td><strong><%= acc.getAccountNumber() %></strong></td>
                                            <td><%= acc.getOwner().getEmail() %></td>
                                            <td style="color: var(--success); font-weight:600;">$<%= acc.getBalance() %></td>
                                            <td>
                                                <span class="status-badge status-<%= acc.getStatus() %>"><%= acc.getStatus() %></span>
                                            </td>
                                            <td>
                                                <form action="<%= request.getContextPath() %>/admin/accounts/toggle" method="POST" style="margin: 0; display: inline;">
                                                    <input type="hidden" name="accountId" value="<%= acc.getId() %>">
                                                    <% if (acc.getStatus() == Account.Status.ACTIVE) { %>
                                                        <input type="hidden" name="status" value="INACTIVE">
                                                        <button type="submit" class="btn" style="background: var(--danger); border-color: var(--danger); padding: 0.25rem 0.5rem; font-size: 0.75rem; width: auto;">Deactivate</button>
                                                    <% } else { %>
                                                        <input type="hidden" name="status" value="ACTIVE">
                                                        <button type="submit" class="btn" style="background: var(--success); border-color: var(--success); padding: 0.25rem 0.5rem; font-size: 0.75rem; width: auto;">Activate</button>
                                                    <% } %>
                                                </form>
                                            </td>
                                        </tr>
                                    <% } %>
                                <% } %>
                            </tbody>
                        </table>
                    </div>

                    <div>
                        <div class="card">
                            <h3>Open Standard Account</h3>
                            <form action="<%= request.getContextPath() %>/admin/accounts/create" method="POST">
                                <div class="form-group">
                                    <label>User Email</label>
                                    <input type="email" name="targetEmail" required placeholder="user@domain.com">
                                </div>
                                <div class="form-group">
                                    <label>Initial Balance ($)</label>
                                    <input type="number" step="0.01" name="initialBalance" required value="100.00">
                                </div>
                                <button type="submit" class="btn">Open Account</button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>

            <!-- VIEW: AUDITS & LOGS -->
            <div class="tab-content <%= "logs".equals(activeTab) ? "active" : "" %>">
                <div class="table-card">
                    <h3>Global Transaction Ledger</h3>
                    <div style="max-height: 250px; overflow-y: auto;">
                        <table>
                            <thead>
                                <tr>
                                    <th>Txn ID</th>
                                    <th>From Account</th>
                                    <th>To Account</th>
                                    <th>Amount</th>
                                    <th>Description</th>
                                    <th>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% if (allTransfers != null) { %>
                                    <% for (Transfer t : allTransfers) { %>
                                        <tr>
                                            <td><%= t.getTransferId() %></td>
                                            <td>Acc <%= t.getFromAccount().getAccountNumber() %> (<%= t.getFromAccount().getOwner().getName() %>)</td>
                                            <td>Acc <%= t.getToAccount().getAccountNumber() %> (<%= t.getToAccount().getOwner().getName() %>)</td>
                                            <td><strong>$<%= t.getAmount() %></strong></td>
                                            <td><%= t.getDescription() %></td>
                                            <td>
                                                <span class="status-badge status-<%= t.getStatus() %>"><%= t.getStatus() %></span>
                                            </td>
                                        </tr>
                                    <% } %>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                </div>

                <div class="table-card">
                    <h3>Persistent Security Audit Log (CDI/EJB interceptors)</h3>
                    <div style="max-height: 350px; overflow-y: auto; background: #F8FAFC; border:1px solid var(--border); padding: 1rem; border-radius: 6px; font-family: monospace; font-size: 0.8rem; line-height: 1.4; color: var(--text-dark);">
                        <% if (auditLogs != null) { %>
                            <% for (AuditLog log : auditLogs) { %>
                                <div class="audit-row-<%= log.isSuccess() %>" style="margin-bottom: 0.5rem; border-bottom: 1px solid var(--border); padding-bottom: 0.5rem;">
                                    [<%= log.getCreatedAt().toString().replace("T", " ").substring(0, 19) %>]
                                    <strong><%= log.getCaller() %></strong> invoked
                                    <strong><%= log.getAction() %></strong> in <%= log.getDurationMs() %>ms.
                                    <br>
                                    <span style="color: var(--text-muted);"><%= log.getDetails() %></span>
                                </div>
                            <% } %>
                        <% } %>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script>
        function filterUsers() {
            var input = document.getElementById("userSearch").value.toUpperCase();
            var table = document.getElementById("usersTable");
            var tr = table.getElementsByTagName("tr");
            for (var i = 1; i < tr.length; i++) {
                var name = tr[i].getElementsByTagName("td")[0].textContent.toUpperCase();
                var email = tr[i].getElementsByTagName("td")[1].textContent.toUpperCase();
                if (name.indexOf(input) > -1 || email.indexOf(input) > -1) {
                    tr[i].style.display = "";
                } else {
                    tr[i].style.display = "none";
                }
            }
        }

        function filterAccounts() {
            var input = document.getElementById("accountSearch").value.toUpperCase();
            var table = document.getElementById("accountsTable");
            var tr = table.getElementsByTagName("tr");
            for (var i = 1; i < tr.length; i++) {
                var accNum = tr[i].getElementsByTagName("td")[0].textContent.toUpperCase();
                var owner = tr[i].getElementsByTagName("td")[1].textContent.toUpperCase();
                if (accNum.indexOf(input) > -1 || owner.indexOf(input) > -1) {
                    tr[i].style.display = "";
                } else {
                    tr[i].style.display = "none";
                }
            }
        }
    </script>
</body>
</html>
