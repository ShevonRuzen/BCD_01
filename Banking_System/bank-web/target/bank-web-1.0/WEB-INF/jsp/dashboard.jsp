<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.core.entity.User" %>
<%@ page import="com.bank.core.entity.Account" %>
<%@ page import="com.bank.core.entity.Transfer" %>
<%@ page import="com.bank.core.entity.MoneyRequest" %>
<%@ page import="java.util.List" %>
<%
    User loggedInUser         = (User)           request.getAttribute("user");
    List<Account> accounts    = (List<Account>)  request.getAttribute("accounts");
    List<Transfer> transfers  = (List<Transfer>) request.getAttribute("transfers");
    List<Transfer> depositHistory = (List<Transfer>) request.getAttribute("depositHistory");
    List<Transfer> scheduledTransfers = (List<Transfer>) request.getAttribute("scheduledTransfers");
    List<MoneyRequest> pendingIncoming  = (List<MoneyRequest>) request.getAttribute("pendingIncoming");
    List<MoneyRequest> outgoingRequests = (List<MoneyRequest>) request.getAttribute("outgoingRequests");
    List<MoneyRequest> allRequests      = (List<MoneyRequest>) request.getAttribute("allRequests");

    Account primaryAcc = (accounts != null && !accounts.isEmpty()) ? accounts.get(0) : null;
    String accNum = (primaryAcc != null) ? primaryAcc.getAccountNumber() : "—";

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
    <title>Build Bank | My Account</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
    <style>
        .two-col { display:grid; grid-template-columns:2fr 1fr; gap:2rem; }
        @media(max-width:900px){ .two-col{ grid-template-columns:1fr; } }
        .account-hero {
            background: var(--primary);
            color: #fff;
            border-radius: 10px;
            padding: 1.75rem 2rem;
            margin-bottom: 1.5rem;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .account-hero .balance { font-size:2rem; font-weight:700; }
        .account-hero .label   { font-size:0.8rem; opacity:0.7; margin-bottom:0.2rem; }
        .feed-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0.8rem 0;
            border-bottom: 1px solid var(--border);
        }
        .feed-item:last-child { border-bottom: none; }
        .feed-icon {
            width: 34px; height: 34px;
            border-radius: 50%;
            display: flex; align-items: center; justify-content: center;
            font-size: 0.75rem; font-weight: 700;
            margin-right: 0.85rem;
            flex-shrink: 0;
        }
        .icon-in   { background:#D1FAE5; color:#065F46; }
        .icon-out  { background:#FEE2E2; color:#991B1B; }
        .icon-dep  { background:#DBEAFE; color:#1E40AF; }
        .icon-req  { background:#FEF3C7; color:#92400E; }
        .icon-fee  { background:#F3F4F6; color:#374151; }
        .tab-content        { display:none; }
        .tab-content.active { display:block; }
        .form-section {
            background:#F8FAFC;
            border:1px solid var(--border);
            border-radius:8px;
            padding:1.25rem;
            margin-bottom:1.5rem;
        }
        .amount-positive { color: var(--success); font-weight:600; }
        .amount-negative { color: var(--danger);  font-weight:600; }
        .amount-neutral  { color: var(--text-muted); font-weight:600; }
    </style>
</head>
<body>

    <!-- Toast notifications -->
    <div id="toast" style="display:none;position:fixed;top:20px;right:20px;z-index:9999;
        background:var(--primary);color:#fff;padding:1rem 1.5rem;border-radius:8px;
        box-shadow:0 10px 20px rgba(0,0,0,0.15);">
        <span id="toastMsg"></span>
    </div>

    <div class="dashboard-container">

        <!-- ===== SIDEBAR ===== -->
        <div class="sidebar">
            <div class="sidebar-header">Build Bank</div>

            <div style="padding:0 1rem 1rem; border-bottom:1px solid rgba(255,255,255,0.1);">
                <div style="font-size:0.72rem;opacity:0.6;margin-bottom:0.2rem;">Client</div>
                <div style="font-weight:600;font-size:0.9rem;"><%= loggedInUser != null ? loggedInUser.getName() : "" %></div>
                <div style="font-size:0.75rem;opacity:0.7;margin-top:0.15rem;"><%= loggedInUser != null ? loggedInUser.getEmail() : "" %></div>
                <div style="margin-top:0.4rem;background:rgba(255,255,255,0.12);border-radius:6px;
                            padding:0.3rem 0.6rem;font-size:0.78rem;font-weight:600;letter-spacing:0.5px;">
                    Acc&nbsp;#&nbsp;<%= accNum %>
                </div>
            </div>

            <ul class="sidebar-menu">
                <li class="sidebar-item <%= "home".equals(activeTab) ? "active" : "" %>">
                    <a href="?page=home">Overview</a>
                </li>
                <li class="sidebar-item <%= "transfers".equals(activeTab) ? "active" : "" %>">
                    <a href="?page=transfers">Transfers & Requests</a>
                </li>
                <li class="sidebar-item <%= "deposit".equals(activeTab) ? "active" : "" %>">
                    <a href="?page=deposit">Deposit Cash</a>
                </li>
                <li class="sidebar-item <%= "profile".equals(activeTab) ? "active" : "" %>">
                    <a href="?page=profile">Security Settings</a>
                </li>
            </ul>

            <div class="sidebar-footer">
                <form action="<%= request.getContextPath() %>/logout" method="POST" style="margin:0;">
                    <button type="submit" class="btn"
                        style="background:rgba(255,255,255,0.1);border:1px solid rgba(255,255,255,0.2);
                               color:#fff;width:100%;font-size:0.85rem;">Logout</button>
                </form>
            </div>
        </div>

        <!-- ===== MAIN CONTENT ===== -->
        <div class="main-workspace">

            <% if (msg != null) { %><div class="alert-success"><%= msg %></div><% } %>
            <% if (err != null) { %><div class="error"><%= err %></div><% } %>

            <!-- ============================================================ -->
            <!-- HOME TAB                                                      -->
            <!-- ============================================================ -->
            <div class="tab-content <%= "home".equals(activeTab) ? "active" : "" %>">

                <!-- Balance hero card -->
                <% if (primaryAcc != null) { %>
                <div class="account-hero">
                    <div>
                        <div class="label">Account Number</div>
                        <div style="font-size:1.4rem;font-weight:700;letter-spacing:1px;"><%= accNum %></div>
                        <div class="label" style="margin-top:0.6rem;">Account Holder</div>
                        <div style="font-size:0.95rem;"><%= loggedInUser != null ? loggedInUser.getName() : "" %></div>
                    </div>
                    <div style="text-align:right;">
                        <div class="label">Available Balance</div>
                        <div class="balance">$<%= primaryAcc.getBalance() %></div>
                        <div class="label" style="margin-top:0.3rem;">Status: <%= primaryAcc.getStatus() %></div>
                    </div>
                </div>
                <% } %>

                <!-- Activity feed -->
                <div class="card">
                    <h3 style="margin-top:0;">Account Activity</h3>

                    <%
                        boolean hasFeed = false;
                        // Transfers feed
                        if (transfers != null && !transfers.isEmpty()) { hasFeed = true; }
                        if (allRequests != null && !allRequests.isEmpty()) { hasFeed = true; }
                    %>

                    <% if (!hasFeed) { %>
                        <p style="color:var(--text-muted);">No activity yet. Make your first deposit or transfer!</p>
                    <% } else { %>

                        <!-- Transfers -->
                        <% if (transfers != null) { for (Transfer txn : transfers) {
                            String myAcc = (primaryAcc != null) ? primaryAcc.getAccountNumber() : "";
                            boolean isDeposit  = txn.getDescription() != null && txn.getDescription().equals("Self-Deposit");
                            boolean isFee      = txn.getDescription() != null && txn.getDescription().startsWith("Transaction Fee");
                            boolean isSent     = !isDeposit && txn.getFromAccount().getAccountNumber().equals(myAcc);
                            boolean isReceived = !isDeposit && !isSent && txn.getToAccount().getAccountNumber().equals(myAcc);
                            String iconClass   = isDeposit ? "icon-dep" : isFee ? "icon-fee" : isSent ? "icon-out" : "icon-in";
                            String iconLabel   = isDeposit ? "DEP" : isFee ? "FEE" : isSent ? "OUT" : "IN";
                        %>
                        <div class="feed-item">
                            <div style="display:flex;align-items:center;">
                                <div class="feed-icon <%= iconClass %>"><%= iconLabel %></div>
                                <div>
                                    <div style="font-size:0.88rem;font-weight:500;">
                                        <% if (isDeposit) { %>Self-Deposit
                                        <% } else if (isFee) { %>Transaction Fee (2.45%)
                                        <% } else if (isSent) { %>Transfer to <%= txn.getToAccount().getOwner().getName() %> (Acc <%= txn.getToAccount().getAccountNumber() %>)
                                        <% } else { %>Received from <%= txn.getFromAccount().getOwner().getName() %> (Acc <%= txn.getFromAccount().getAccountNumber() %>)
                                        <% } %>
                                    </div>
                                    <div style="font-size:0.75rem;color:var(--text-muted);margin-top:0.1rem;">
                                        <%= txn.getTransferId() %> &bull; <%= txn.getStatus() %>
                                        <% if (txn.getDescription() != null && !txn.getDescription().equals("Self-Deposit") && !txn.getDescription().startsWith("Transaction Fee")) { %>
                                            &bull; <%= txn.getDescription() %>
                                        <% } %>
                                    </div>
                                </div>
                            </div>
                            <div style="text-align:right;white-space:nowrap;">
                                <span class="<%= isDeposit ? "amount-positive" : isFee ? "amount-negative" : isSent ? "amount-negative" : "amount-positive" %>">
                                    <%= (isDeposit || isReceived) ? "+" : "-" %>$<%= txn.getAmount() %>
                                </span>
                                <% if (txn.getCompletedTime() != null) { %>
                                    <div style="font-size:0.72rem;color:var(--text-muted);margin-top:0.1rem;">
                                        <%= txn.getCompletedTime().toString().replace("T"," ").substring(0,16) %>
                                    </div>
                                <% } %>
                            </div>
                        </div>
                        <% } } %>

                        <!-- Money requests feed -->
                        <% if (allRequests != null) { for (MoneyRequest r : allRequests) {
                            String myEmail = loggedInUser != null ? loggedInUser.getEmail() : "";
                            boolean iRequested = r.getRequester().getOwner().getEmail().equals(myEmail);
                            String statusClass = r.getStatus() == MoneyRequest.Status.APPROVED ? "amount-positive"
                                              : r.getStatus() == MoneyRequest.Status.REJECTED  ? "amount-negative"
                                              : "amount-neutral";
                        %>
                        <div class="feed-item">
                            <div style="display:flex;align-items:center;">
                                <div class="feed-icon icon-req">REQ</div>
                                <div>
                                    <div style="font-size:0.88rem;font-weight:500;">
                                        <% if (iRequested) { %>
                                            Request to <%= r.getPayer().getOwner().getName() %> (Acc <%= r.getPayer().getAccountNumber() %>)
                                        <% } else { %>
                                            Request from <%= r.getRequester().getOwner().getName() %> (Acc <%= r.getRequester().getAccountNumber() %>)
                                        <% } %>
                                    </div>
                                    <div style="font-size:0.75rem;color:var(--text-muted);margin-top:0.1rem;">
                                        <%= r.getRequestId() %> &bull; <%= r.getStatus() %>
                                        <% if (r.getDescription() != null && !r.getDescription().isEmpty()) { %>
                                            &bull; <%= r.getDescription() %>
                                        <% } %>
                                    </div>
                                </div>
                            </div>
                            <div style="text-align:right;">
                                <span class="<%= statusClass %>">$<%= r.getAmount() %></span>
                            </div>
                        </div>
                        <% } } %>

                    <% } %>
                </div>
            </div>

            <!-- ============================================================ -->
            <!-- TRANSFERS TAB                                                 -->
            <!-- ============================================================ -->
            <div class="tab-content <%= "transfers".equals(activeTab) ? "active" : "" %>">
                <div class="two-col">
                    <div>

                        <!-- Scheduled transfers (visible to sender AND recipient) -->
                        <% boolean hasScheduled = scheduledTransfers != null && !scheduledTransfers.isEmpty(); %>
                        <% if (hasScheduled) { %>
                        <div class="card mb-2" style="border-left:4px solid #6366F1;">
                            <h3 style="color:#6366F1;margin-top:0;">Scheduled Payments</h3>
                            <% String myEmail2 = loggedInUser != null ? loggedInUser.getEmail() : ""; %>
                            <% for (Transfer sc : scheduledTransfers) {
                                boolean isSender2 = sc.getFromAccount().getOwner().getEmail().equals(myEmail2);
                            %>
                            <div class="feed-item">
                                <div>
                                    <strong><%= sc.getTransferId() %></strong>
                                    <span style="font-size:0.8rem;color:var(--text-muted);"> &bull; <%= sc.getDescription() %></span>
                                    <div style="font-size:0.78rem;color:var(--text-muted);margin-top:0.15rem;">
                                        <% if (isSender2) { %>
                                            Sending to <strong><%= sc.getToAccount().getOwner().getName() %></strong>
                                            (Acc <%= sc.getToAccount().getAccountNumber() %>)
                                        <% } else { %>
                                            Incoming from <strong><%= sc.getFromAccount().getOwner().getName() %></strong>
                                            (Acc <%= sc.getFromAccount().getAccountNumber() %>)
                                        <% } %>
                                    </div>
                                    <% if (sc.getScheduledTime() != null) { %>
                                    <div style="font-size:0.75rem;color:#6366F1;margin-top:0.15rem;">
                                        Executes: <%= sc.getScheduledTime().toString().replace("T"," ").substring(0,16) %>
                                        (+ 2.45% fee on sender)
                                    </div>
                                    <% } %>
                                </div>
                                <div style="display:flex;flex-direction:column;gap:0.35rem;align-items:flex-end;">
                                    <strong style="color:var(--primary);">$<%= sc.getAmount() %></strong>
                                    <span class="status-badge status-SCHEDULED">SCHEDULED</span>
                                    <form action="<%= request.getContextPath() %>/user/transfers/cancel" method="POST" style="margin:0;">
                                        <input type="hidden" name="transferId" value="<%= sc.getId() %>">
                                        <button type="submit" class="btn"
                                            style="background:var(--danger);border-color:var(--danger);
                                                   font-size:0.72rem;padding:0.22rem 0.5rem;width:auto;">
                                            Cancel
                                        </button>
                                    </form>
                                </div>
                            </div>
                            <% } %>
                        </div>
                        <% } %>

                        <!-- Pending incoming requests -->
                        <% if (pendingIncoming != null && !pendingIncoming.isEmpty()) { %>
                        <div class="card mb-2" style="border-left:4px solid #D97706;">
                            <h3 style="color:#D97706;margin-top:0;">Incoming Requests (Awaiting Approval)</h3>
                            <% for (MoneyRequest r : pendingIncoming) { %>
                            <div class="feed-item">
                                <div>
                                    <strong><%= r.getRequestId() %></strong>
                                    — from <strong><%= r.getRequester().getOwner().getName() %></strong>
                                    (Acc <%= r.getRequester().getAccountNumber() %>)
                                    <div style="font-size:0.78rem;color:var(--text-muted);"><%= r.getDescription() %></div>
                                    <div style="font-size:0.78rem;color:#92400E;margin-top:0.2rem;">
                                        Note: approving charges 2.45% fee on your account
                                    </div>
                                </div>
                                <div style="display:flex;flex-direction:column;gap:0.4rem;align-items:flex-end;">
                                    <span style="font-weight:700;color:var(--primary);">$<%= r.getAmount() %></span>
                                    <div style="display:flex;gap:0.4rem;">
                                        <form action="<%= request.getContextPath() %>/user/transfers/approve" method="POST" style="margin:0;">
                                            <input type="hidden" name="requestId" value="<%= r.getId() %>">
                                            <button type="submit" class="btn"
                                                style="background:var(--success);border-color:var(--success);
                                                       font-size:0.75rem;padding:0.25rem 0.6rem;width:auto;">Approve</button>
                                        </form>
                                        <form action="<%= request.getContextPath() %>/user/transfers/reject" method="POST" style="margin:0;">
                                            <input type="hidden" name="requestId" value="<%= r.getId() %>">
                                            <button type="submit" class="btn"
                                                style="background:var(--danger);border-color:var(--danger);
                                                       font-size:0.75rem;padding:0.25rem 0.6rem;width:auto;">Decline</button>
                                        </form>
                                    </div>
                                </div>
                            </div>
                            <% } %>
                        </div>
                        <% } %>

                        <!-- Sent request status -->
                        <div class="card mb-2">
                            <h3 style="margin-top:0;">Sent Requests</h3>
                            <% if (outgoingRequests == null || outgoingRequests.isEmpty()) { %>
                                <p style="color:var(--text-muted);font-size:0.9rem;">No outgoing requests sent.</p>
                            <% } else { %>
                                <% for (MoneyRequest r : outgoingRequests) { %>
                                <div class="feed-item">
                                    <div>
                                        <strong><%= r.getRequestId() %></strong>
                                        — to <%= r.getPayer().getOwner().getName() %> (Acc <%= r.getPayer().getAccountNumber() %>)
                                        <div style="font-size:0.78rem;color:var(--text-muted);"><%= r.getDescription() %></div>
                                    </div>
                                    <div style="text-align:right;">
                                        <div style="font-weight:600;color:var(--primary);">$<%= r.getAmount() %></div>
                                        <span class="status-badge status-<%= r.getStatus() %>"
                                              style="margin-top:0.25rem;display:inline-block;"><%= r.getStatus() %></span>
                                    </div>
                                </div>
                                <% } %>
                            <% } %>
                        </div>

                        <!-- Transaction history ledger -->
                        <div class="card">
                            <h3 style="margin-top:0;">Transaction History</h3>
                            <% if (transfers == null || transfers.isEmpty()) { %>
                                <p style="color:var(--text-muted);font-size:0.9rem;">No transactions yet.</p>
                            <% } else { %>
                                <% for (Transfer txn : transfers) { %>
                                <div class="feed-item">
                                    <div>
                                        <strong><%= txn.getTransferId() %></strong>
                                        <span style="font-size:0.8rem;color:var(--text-muted);"> &bull; <%= txn.getDescription() %></span>
                                        <div style="font-size:0.75rem;color:var(--text-muted);margin-top:0.15rem;">
                                            Acc <%= txn.getFromAccount().getAccountNumber() %> → Acc <%= txn.getToAccount().getAccountNumber() %>
                                        </div>
                                    </div>
                                    <div style="text-align:right;display:flex;flex-direction:column;align-items:flex-end;gap:0.25rem;">
                                        <strong style="color:var(--primary);">$<%= txn.getAmount() %></strong>
                                        <span class="status-badge status-<%= txn.getStatus() %>"><%= txn.getStatus() %></span>
                                        <% if (txn.getStatus() == Transfer.Status.SCHEDULED) { %>
                                        <form action="<%= request.getContextPath() %>/user/transfers/cancel" method="POST" style="margin:0;">
                                            <input type="hidden" name="transferId" value="<%= txn.getId() %>">
                                            <button type="submit" class="btn"
                                                style="background:var(--danger);border-color:var(--danger);
                                                       font-size:0.7rem;padding:0.2rem 0.45rem;width:auto;">Cancel</button>
                                        </form>
                                        <% } %>
                                    </div>
                                </div>
                                <% } %>
                            <% } %>
                        </div>
                    </div>

                    <!-- Right: action forms -->
                    <div>
                        <div class="card">
                            <h3 style="margin-top:0;">Payment Actions</h3>

                            <div class="form-section">
                                <h4 style="margin-top:0;color:var(--accent);">Direct Transfer</h4>
                                <p style="font-size:0.8rem;color:var(--text-muted);margin-top:-0.5rem;">
                                    A 2.45% fee is deducted separately from your account.
                                </p>
                                <form action="<%= request.getContextPath() %>/transfer" method="POST">
                                    <input type="hidden" name="fromAccountNumber" value="<%= accNum %>">
                                    <div class="form-group">
                                        <label>Recipient Account #</label>
                                        <input type="text" name="toAccountNumber" maxlength="8" minlength="8" required placeholder="8 digits">
                                    </div>
                                    <div class="form-group">
                                        <label>Amount ($)</label>
                                        <input type="number" step="0.01" name="amount" required placeholder="0.00">
                                    </div>
                                    <div class="form-group">
                                        <label>Reference</label>
                                        <input type="text" name="description" placeholder="Rent, split, etc.">
                                    </div>
                                    <button type="submit" class="btn">Send Money</button>
                                </form>
                            </div>

                            <div class="form-section">
                                <h4 style="margin-top:0;color:var(--accent);">Schedule Payment</h4>
                                <p style="font-size:0.8rem;color:var(--text-muted);margin-top:-0.5rem;">
                                    2.45% fee is charged at execution time.
                                </p>
                                <form action="<%= request.getContextPath() %>/transfer/schedule" method="POST">
                                    <input type="hidden" name="fromAccountNumber" value="<%= accNum %>">
                                    <div class="form-group">
                                        <label>Recipient Account #</label>
                                        <input type="text" name="toAccountNumber" maxlength="8" minlength="8" required placeholder="8 digits">
                                    </div>
                                    <div class="form-group">
                                        <label>Amount ($)</label>
                                        <input type="number" step="0.01" name="amount" required placeholder="0.00">
                                    </div>
                                    <div class="form-group">
                                        <label>Date & Time</label>
                                        <input type="datetime-local" name="scheduledTime" required>
                                    </div>
                                    <div class="form-group">
                                        <label>Reference</label>
                                        <input type="text" name="description" placeholder="Future payment">
                                    </div>
                                    <button type="submit" class="btn" style="background:#4B5563;border-color:#4B5563;">Schedule</button>
                                </form>
                            </div>

                            <div class="form-section" style="margin-bottom:0;">
                                <h4 style="margin-top:0;color:#D97706;">Request Money</h4>
                                <p style="font-size:0.8rem;color:var(--text-muted);margin-top:-0.5rem;">
                                    The payer will be charged 2.45% fee when they approve.
                                </p>
                                <form action="<%= request.getContextPath() %>/user/transfers/request" method="POST">
                                    <div class="form-group">
                                        <label>Payer's Account #</label>
                                        <input type="text" name="payerAccountNumber" maxlength="8" minlength="8" required placeholder="8 digits">
                                    </div>
                                    <div class="form-group">
                                        <label>Amount ($)</label>
                                        <input type="number" step="0.01" name="amount" required placeholder="0.00">
                                    </div>
                                    <div class="form-group">
                                        <label>Reason</label>
                                        <input type="text" name="description" placeholder="Dinner, cab split...">
                                    </div>
                                    <button type="submit" class="btn" style="background:#D97706;border-color:#D97706;">Send Request</button>
                                </form>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- ============================================================ -->
            <!-- DEPOSIT TAB                                                   -->
            <!-- ============================================================ -->
            <div class="tab-content <%= "deposit".equals(activeTab) ? "active" : "" %>">
                <div class="two-col">
                    <div>
                        <!-- Deposit history table -->
                        <div class="card">
                            <h3 style="margin-top:0;">Deposit History</h3>
                            <% if (depositHistory == null || depositHistory.isEmpty()) { %>
                                <p style="color:var(--text-muted);font-size:0.9rem;">No deposits recorded yet.</p>
                            <% } else { %>
                            <table>
                                <thead>
                                    <tr>
                                        <th>Deposit ID</th>
                                        <th>Amount</th>
                                        <th>Date & Time</th>
                                        <th>Status</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% for (Transfer dep : depositHistory) { %>
                                    <tr>
                                        <td><strong><%= dep.getTransferId() %></strong></td>
                                        <td style="color:var(--success);font-weight:700;">+$<%= dep.getAmount() %></td>
                                        <td style="font-size:0.82rem;color:var(--text-muted);">
                                            <%= dep.getCompletedTime() != null ? dep.getCompletedTime().toString().replace("T"," ").substring(0,16) : "—" %>
                                        </td>
                                        <td><span class="status-badge status-<%= dep.getStatus() %>"><%= dep.getStatus() %></span></td>
                                    </tr>
                                    <% } %>
                                </tbody>
                            </table>
                            <% } %>
                        </div>
                    </div>

                    <!-- Deposit form -->
                    <div>
                        <div class="card">
                            <h3 style="margin-top:0;">Deposit Funds</h3>
                            <p style="color:var(--text-muted);font-size:0.88rem;margin-bottom:1.25rem;">
                                No fees on self-deposits.
                            </p>
                            <form action="<%= request.getContextPath() %>/user/accounts/deposit" method="POST">
                                <input type="hidden" name="accountId" value="<%= primaryAcc != null ? primaryAcc.getId() : "" %>">
                                <div class="form-group">
                                    <label>Account Number</label>
                                    <input type="text" readonly value="<%= accNum %>">
                                </div>
                                <div class="form-group">
                                    <label>Amount ($)</label>
                                    <input type="number" step="0.01" name="amount" required placeholder="100.00">
                                </div>
                                <button type="submit" class="btn"
                                    style="background:var(--success);border-color:var(--success);">Confirm Deposit</button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>

            <!-- ============================================================ -->
            <!-- PROFILE / SECURITY TAB                                        -->
            <!-- ============================================================ -->
            <div class="tab-content <%= "profile".equals(activeTab) ? "active" : "" %>">
                <div class="card" style="max-width:480px;margin:0 auto;">
                    <h3 style="margin-top:0;">Security Settings</h3>
                    <form action="<%= request.getContextPath() %>/user/profile/update" method="POST">
                        <div class="form-group">
                            <label>Full Name</label>
                            <input type="text" name="name"
                                value="<%= loggedInUser != null ? loggedInUser.getName() : "" %>"
                                placeholder="Your Name">
                        </div>
                        <div class="form-group">
                            <label>New Password (min 8 characters)</label>
                            <input type="password" name="password" placeholder="••••••••">
                        </div>
                        <div class="form-group">
                            <label>Confirm Password</label>
                            <input type="password" name="confirmPassword" placeholder="••••••••">
                        </div>
                        <button type="submit" class="btn">Save Changes</button>
                    </form>
                </div>
            </div>

        </div><!-- end main-workspace -->
    </div><!-- end dashboard-container -->

    <!-- WebSocket live notifications -->
    <script>
        var userId = '<%= loggedInUser != null ? loggedInUser.getId() : "" %>';
        if (userId) {
            var proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
            try {
                var ws = new WebSocket(proto + '//' + location.host + '<%= request.getContextPath() %>/notifications/' + userId);
                ws.onmessage = function(e) {
                    document.getElementById('toastMsg').textContent = e.data;
                    var t = document.getElementById('toast');
                    t.style.display = 'block';
                    setTimeout(function(){ t.style.display = 'none'; }, 5000);
                };
            } catch(ex) { console.log('WebSocket offline.'); }
        }
    </script>
</body>
</html>
