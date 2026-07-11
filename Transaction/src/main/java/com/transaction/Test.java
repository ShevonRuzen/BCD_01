package com.transaction;

import jakarta.persistence.EntityManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/test")
public class Test extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        EntityManager em = ManagerFactory.getEntityManager();
        System.out.println("EntityManager: " + em);
        List<Object[]> resultList = em.createNativeQuery("select * from user").getResultList();
        resultList.forEach( r ->{
            System.out.println(r[0] +" : "+r[1]+" : "+r[2]+" : "+r[3]);
        });
    }
}
