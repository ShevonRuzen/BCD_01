package lk.jiat.bcd.controller;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.jiat.bcd.model.User;

import java.io.IOException;
import java.util.ArrayList;

@WebServlet("/signup")
public class SignUp extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String name = req.getParameter("name");
        String mobile = req.getParameter("mobile");
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        ServletContext context = getServletContext();
        ArrayList<User> users = new ArrayList<>();

        if (context.getAttribute("users") == null) {
            users = (ArrayList<User>) context.getAttribute("users");
        } else {
            context.setAttribute("users", users);
        }

        if (!name.isEmpty() && !mobile.isEmpty() && !email.isEmpty() && !password.isEmpty()) {

        }

    }
}
