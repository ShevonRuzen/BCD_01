package lk.jiat.bcd.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/profile")
public class Profile extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();

        if(session.getAttribute("email") != null) {

            resp.getWriter().write(session.getAttribute("name").toString());
            resp.getWriter().write(session.getAttribute("email").toString());
            resp.getWriter().write(session.getAttribute("mobile").toString());


        }else {
            resp.sendRedirect("signin.jsp");
        }
    }
}