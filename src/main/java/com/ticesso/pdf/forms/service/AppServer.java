package com.ticesso.pdf.forms.service;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Main class to start app server.
 */
public class AppServer {

    public static void main(String[] args) throws Exception {
        Server jettyServer = new Server(4567);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        ServletHolder jerseyServlet = context.addServlet(ServletContainer.class, "/app/*");
        jerseyServlet.setInitOrder(0);
        jerseyServlet.setInitParameter(
                "javax.ws.rs.Application", FormsApplication.class.getCanonicalName());

        final ServletHolder contentServlet = context.addServlet(DefaultServlet.class, "/static/*");
        jerseyServlet.setInitOrder(1);
        contentServlet.setInitParameter("resourceBase", "content");
        contentServlet.setInitParameter("dirAllowed", "true");
        contentServlet.setInitParameter("pathInfoOnly", "true");

        jettyServer.setHandler(context);

        try {
            jettyServer.start();
            jettyServer.join();
        } finally {
            jettyServer.destroy();
        }
    }
}
