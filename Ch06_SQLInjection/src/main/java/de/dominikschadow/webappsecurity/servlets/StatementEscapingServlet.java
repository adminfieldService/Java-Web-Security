/*
 * Copyright (C) 2015 Dominik Schadow, info@dominikschadow.de
 *
 * This file is part of the Java-Web-Security project.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.dominikschadow.webappsecurity.servlets;

import de.dominikschadow.webappsecurity.domain.Customer;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.codecs.OracleCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet using a normal Statement to query the in-memory-database.
 * User input is escaped with ESAPI and used in the SQL query afterwards.
 *
 * @author Dominik Schadow
 */
@WebServlet(name = "StatementEscapingServlet", urlPatterns = {"/StatementEscapingServlet"})
public class StatementEscapingServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatementEscapingServlet.class);
    private static final long serialVersionUID = 1L;
    private transient Connection con = null;

    @PostConstruct
    public void init() {
        try {
        	Class.forName("org.hsqldb.jdbcDriver");
            con = DriverManager.getConnection("jdbc:hsqldb:res:/customerDB; shutdown=true", "sa", "");
        } catch (ClassNotFoundException | SQLException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            if (con != null) {
                con.close();
            }
        } catch (SQLException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        String name = request.getParameter("name");
        LOGGER.info("Received " + name + " as POST parameter");

        String safeName = ESAPI.encoder().encodeForSQL(new OracleCodec(), name);
        LOGGER.info("Escaped name is " + safeName);

        String query = "SELECT * FROM customer WHERE name = '" + safeName + "' ORDER BY CUST_ID";
        List<Customer> customers = new ArrayList<>();

        LOGGER.info("Final SQL query " + query);

        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                Customer customer = new Customer();
                customer.setCustId(rs.getInt(1));
                customer.setName(rs.getString(2));
                customer.setStatus(rs.getString(3));
                customer.setOrderLimit(rs.getInt(4));

                customers.add(customer);
            }
        } catch (SQLException ex) {
            LOGGER.error(ex.getMessage(), ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }

        response.setContentType("text/html");

        try (PrintWriter out = response.getWriter()) {
            out.println("<html>");
            out.println("<head><link rel=\"stylesheet\" type=\"text/css\" href=\"styles.css\" /></head>");
            out.println("<body>");
            out.println("<h1>Ch06_SQLInjection - Statement with Escaping</h1>");
            out.println("<p><strong>Input</strong> " + name + "</p>");
            out.println("<h2>Customer Data</h2>");
            out.println("<table>");
            out.println("<tr>");
            out.println("<th>ID</th>");
            out.println("<th>Name</th>");
            out.println("<th>Status</th>");
            out.println("<th>Order Limit</th>");
            out.println("</tr>");

            for (Customer customer : customers) {
                out.println("<tr>");
                out.println("<td>" + customer.getCustId() + "</td>");
                out.println("<td>" + customer.getName() + "</td>");
                out.println("<td>" + customer.getStatus() + "</td>");
                out.println("<td>" + customer.getOrderLimit() + "</td>");
                out.println("</tr>");
            }

            out.println("<table>");
            out.println("</body>");
            out.println("</html>");
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }
}
