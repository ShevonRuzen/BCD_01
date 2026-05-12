package lk.jiat.bcd.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.jiat.bcd.model.User;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/submit")
public class UserServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String name = req.getParameter("name");
        String mobile = req.getParameter("mobile");

        User u = new User();
        u.setName(name);
        u.setMobile(mobile);

        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
        out.println("Name : " + u.getName());
        out.println("Mobile : " + u.getMobile());

    }
}