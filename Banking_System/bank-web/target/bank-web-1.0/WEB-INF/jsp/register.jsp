<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Build Bank | Register</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">
</head>
<body>
    <div style="display:flex; justify-content:center; align-items:center; min-height:100vh; padding: 1rem;">
        <div class="card" style="width: 100%; max-width: 450px; padding: 2.5rem 2rem;">
            <div class="text-center mb-2">
                <h2 style="margin-bottom:0.25rem;">Build Bank</h2>
                <span style="color: var(--text-muted); font-size: 0.85rem;">Create New Customer Account</span>
            </div>
            
            <% if (request.getAttribute("error") != null) { %>
                <div class="error"><%= request.getAttribute("error") %></div>
            <% } %>
            
            <form action="<%= request.getContextPath() %>/register" method="POST">
                <div class="form-group">
                    <label>Full Name</label>
                    <input type="text" name="name" required placeholder="John Doe">
                </div>
                <div class="form-group">
                    <label>Email Address</label>
                    <input type="email" name="email" required placeholder="john@domain.com">
                </div>
                <div class="form-group">
                    <label>Password (min 8 characters)</label>
                    <input type="password" name="password" required placeholder="••••••••">
                </div>
                <div class="form-group">
                    <label>Confirm Password</label>
                    <input type="password" name="confirmPassword" required placeholder="••••••••">
                </div>
                <button type="submit" class="btn">Register</button>
            </form>
            <div class="text-center mt-2">
                <a href="<%= request.getContextPath() %>/login" style="color: var(--accent); text-decoration: none; font-size: 0.875rem; font-weight: 500;">Already registered? Sign In</a>
            </div>
        </div>
    </div>
</body>
</html>
