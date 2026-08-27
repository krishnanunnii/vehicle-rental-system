package servlet;

import dao.CustomerDAO;
import model.Customer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet endpoint for managing Customer registration and listing.
 * Implements mandatory server-side validation.
 */
@WebServlet("/api/customers")
public class CustomerServlet extends HttpServlet {

    private final CustomerDAO customerDAO = new CustomerDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String query = req.getParameter("query");
        String idParam = req.getParameter("id");
        PrintWriter out = resp.getWriter();

        if (idParam != null && !idParam.isEmpty()) {
            try {
                int id = Integer.parseInt(idParam);
                Customer c = customerDAO.getCustomerById(id);
                if (c != null) {
                    out.print("{\"success\": true, \"customer\": " + customerToJson(c) + "}");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\": false, \"message\": \"Customer not found\"}");
                }
            } catch (Exception e) {
                sendError(resp, out, "Invalid customer ID format.");
            }
            return;
        }

        List<Customer> list = customerDAO.searchCustomers(query);
        StringBuilder json = new StringBuilder();
        json.append("{\"success\": true, \"customers\": [");
        for (int i = 0; i < list.size(); i++) {
            json.append(customerToJson(list.get(i)));
            if (i < list.size() - 1) json.append(",");
        }
        json.append("]}");

        out.print(json.toString());
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        String name = req.getParameter("name");
        String ageParam = req.getParameter("age");
        String phone = req.getParameter("phone");
        String licenseNumber = req.getParameter("licenseNumber");
        String action = req.getParameter("action");
        String idParam = req.getParameter("id");

        if (name == null && req.getContentType() != null && req.getContentType().contains("application/json")) {
            Map<String, String> bodyMap = parseJsonBody(req);
            name = bodyMap.get("name");
            ageParam = bodyMap.get("age");
            phone = bodyMap.get("phone");
            licenseNumber = bodyMap.get("licenseNumber");
            action = bodyMap.get("action");
            idParam = bodyMap.get("id");
        }

        if ("DELETE".equalsIgnoreCase(action)) {
            if (idParam == null || idParam.isEmpty()) {
                sendError(resp, out, "Customer ID required for deletion.");
                return;
            }
            try {
                int id = Integer.parseInt(idParam);
                boolean deleted = customerDAO.deleteCustomer(id);
                if (deleted) {
                    out.print("{\"success\": true, \"message\": \"Customer deleted successfully.\"}");
                } else {
                    sendError(resp, out, "Customer could not be deleted.");
                }
            } catch (SQLException e) {
                sendError(resp, out, e.getMessage());
            } catch (Exception e) {
                sendError(resp, out, "Invalid customer ID format.");
            }
            return;
        }

        // --- Mandatory Server-side Validation ---
        if (name == null || name.trim().isEmpty()) {
            sendError(resp, out, "Customer name cannot be empty.");
            return;
        }
        name = name.trim();

        if (ageParam == null || ageParam.trim().isEmpty()) {
            sendError(resp, out, "Customer age is required.");
            return;
        }
        int age;
        try {
            age = Integer.parseInt(ageParam.trim());
            if (age < 18) {
                sendError(resp, out, "Customer must be at least 18 years old.");
                return;
            }
            if (age > 120) {
                sendError(resp, out, "Please enter a valid age.");
                return;
            }
        } catch (NumberFormatException e) {
            sendError(resp, out, "Customer age must be a valid integer number.");
            return;
        }

        if (phone == null || phone.trim().isEmpty()) {
            sendError(resp, out, "Phone number cannot be empty.");
            return;
        }
        phone = phone.trim().replaceAll("[^0-9]", ""); // keep digits
        if (phone.length() != 10) {
            sendError(resp, out, "Phone number must contain exactly 10 digits.");
            return;
        }

        if (licenseNumber == null || licenseNumber.trim().isEmpty()) {
            sendError(resp, out, "Driving license number cannot be empty.");
            return;
        }
        licenseNumber = licenseNumber.trim().toUpperCase();

        if (customerDAO.isLicenseExists(licenseNumber, 0)) {
            sendError(resp, out, "Driving license number '" + licenseNumber + "' is already registered.");
            return;
        }

        Customer customer = new Customer(name, age, phone, licenseNumber);

        try {
            boolean success = customerDAO.addCustomer(customer);
            if (success) {
                out.print("{\"success\": true, \"message\": \"Customer registered successfully!\", \"customer\": " + customerToJson(customer) + "}");
            } else {
                sendError(resp, out, "Failed to register customer in database.");
            }
        } catch (SQLException e) {
            sendError(resp, out, "Database error: " + e.getMessage());
        }
    }

    private void sendError(HttpServletResponse resp, PrintWriter out, String message) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        out.print("{\"success\": false, \"message\": \"" + escapeJson(message) + "\"}");
    }

    private String customerToJson(Customer c) {
        return "{" +
                "\"id\": " + c.getId() + "," +
                "\"name\": \"" + escapeJson(c.getName()) + "\"," +
                "\"age\": " + c.getAge() + "," +
                "\"phone\": \"" + escapeJson(c.getPhone()) + "\"," +
                "\"licenseNumber\": \"" + escapeJson(c.getLicenseNumber()) + "\"" +
                "}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private Map<String, String> parseJsonBody(HttpServletRequest req) {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader reader = req.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String body = sb.toString().trim();
            if (body.startsWith("{") && body.endsWith("}")) {
                body = body.substring(1, body.length() - 1);
                String[] pairs = body.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().replaceAll("^\"|\"$", "");
                        String val = kv[1].trim().replaceAll("^\"|\"$", "");
                        map.put(key, val);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return map;
    }
}
