package lk.jiat.bcd.controller;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lk.jiat.bcd.model.User;

import java.io.IOException;
import java.util.ArrayList;


@WebServlet("/signin")

public class SignIn extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        ServletContext context = getServletContext();

        ArrayList<User>users;

        if (context.getAttribute("users") == null) {
            resp.sendRedirect("signup.jsp");
        }else {
            users = (ArrayList<User>) context.getAttribute("users");

            User user = null;

            for (User u : users) {
                if(u.getEmail().equals(email) && u.getPassword().equals(password)) {
                    user = u;
                    break;


                }
            }
            if (user !=null){
                HttpSession session = req.getSession();
                session.setAttribute("email", email);
                session.setAttribute("name", user.getName());
                session.setAttribute("mobile", user.getMobile());

                resp.sendRedirect("profile");

            }else {
                resp.getWriter().write("Invalid email or password");
            }
        }
    }
}