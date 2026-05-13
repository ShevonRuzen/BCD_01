<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Sign In</title>
    <style>
        :root {
            --primary-color: #4f46e5;
            --primary-hover: #4338ca;
            --bg-color: #f3f4f6;
            --text-main: #1f2937;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: var(--bg-color);
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            margin: 0;
        }

        .signin-container {
            background: #ffffff;
            padding: 2.5rem;
            border-radius: 12px;
            box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
            width: 100%;
            max-width: 380px;
        }

        h2 {
            margin-top: 0;
            color: var(--text-main);
            text-align: center;
            font-weight: 600;
            margin-bottom: 0.5rem;
        }

        .subtitle {
            text-align: center;
            color: #6b7280;
            font-size: 0.9rem;
            margin-bottom: 2rem;
        }

        .form-group {
            margin-bottom: 1.2rem;
        }

        label {
            display: block;
            margin-bottom: 0.5rem;
            font-size: 0.9rem;
            font-weight: 500;
            color: #4b5563;
        }

        input {
            width: 100%;
            padding: 0.75rem;
            border: 1px solid #d1d5db;
            border-radius: 6px;
            box-sizing: border-box;
            transition: all 0.2s ease;
        }

        input:focus {
            outline: none;
            border-color: var(--primary-color);
            box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
        }

        .flex-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 1.5rem;
        }

        .forgot-password {
            font-size: 0.85rem;
            color: var(--primary-color);
            text-decoration: none;
        }

        button {
            width: 100%;
            padding: 0.75rem;
            background-color: var(--primary-color);
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 1rem;
            font-weight: 600;
            cursor: pointer;
            transition: background 0.2s;
        }

        button:hover {
            background-color: var(--primary-hover);
        }

        .footer-text {
            text-align: center;
            font-size: 0.85rem;
            margin-top: 1.5rem;
            color: #6b7280;
        }

        .footer-text a {
            color: var(--primary-color);
            text-decoration: none;
            font-weight: 600;
        }
    </style>
</head>
<body>

<div class="signin-container">
    <h2>Welcome Back</h2>
    <p class="subtitle">Please enter your details</p>

    <form action="signin" method="post">
        <div class="form-group">
            <label for="email">Email Address</label>
            <input type="email" id="email" name="email" placeholder="Enter your email" required>
        </div>

        <div class="form-group">
            <label for="password">Password</label>
            <input type="password" id="password" name="password" placeholder="••••••••" required>
        </div>

        <div class="flex-row">
            <div style="display: flex; align-items: center;">
                <input type="checkbox" id="remember" style="width: auto; margin-right: 8px;">
                <label for="remember" style="margin-bottom: 0; font-size: 0.85rem; cursor: pointer;">Remember me</label>
            </div>
            <a href="#" class="forgot-password">Forgot password?</a>
        </div>

        <button type="submit">Sign In</button>
    </form>

    <div class="footer-text">
        Don't have an account? <a href="signup.jsp">Sign up for free</a>
    </div>
</div>

</body>
</html>