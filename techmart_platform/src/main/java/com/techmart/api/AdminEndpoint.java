package com.techmart.api;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/admin")
@RolesAllowed({"ADMIN"})
public class AdminEndpoint {
    
    @GET
    @Path("/metrics")
    public Response getSystemMetrics() {
        // Only accessible to ADMIN role
        // Stub for metrics collector
        return Response.ok("{\"metrics\": \"metrics_data\"}").build();
    }
}
