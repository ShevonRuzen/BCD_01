package com.techmart.api;

import com.techmart.monitoring.PerformanceMetricsCollector;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/metrics")
public class MetricsEndpoint extends HttpServlet {

    @EJB
    private PerformanceMetricsCollector metricsCollector;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String benchmarkParam = req.getParameter("benchmark");
        if ("true".equalsIgnoreCase(benchmarkParam)) {
            // Trigger JNDI vs CDI benchmark
            double[] benchmarkResults = metricsCollector.runJndiVsCdiBenchmark();
            resp.getWriter().write(String.format(
                "{\"benchmarkRun\": true, \"cdiTimeMs\": %.4f, \"jndiTimeMs\": %.4f, \"ratio\": %.2f}",
                benchmarkResults[0], benchmarkResults[1], (benchmarkResults[1] / Math.max(benchmarkResults[0], 0.0001))
            ));
        } else {
            // Return regular telemetry
            resp.getWriter().write(String.format(
                "{\"totalCheckouts\": %d, \"successfulCheckouts\": %d, \"failedCheckouts\": %d, " +
                "\"activeSessions\": %d, \"cacheHitRate\": %.2f, \"avgCheckoutTimeMs\": %.4f, \"avgDbPersistenceTimeMs\": %.4f}",
                metricsCollector.getTotalCheckouts(),
                metricsCollector.getSuccessfulCheckouts(),
                metricsCollector.getFailedCheckouts(),
                metricsCollector.getActiveSessions(),
                metricsCollector.getCacheHitRate(),
                metricsCollector.getAverageCheckoutTimeMs(),
                metricsCollector.getAverageDbPersistenceTimeMs()
            ));
        }
    }
}
