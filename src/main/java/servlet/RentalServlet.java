package servlet;

import dao.RentalDAO;
import model.Rental;

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
 * Servlet endpoint for Rental operations (Book vehicle, fetch history, receipt payload).
 */
@WebServlet("/api/rentals")
public class RentalServlet extends HttpServlet {

    private final RentalDAO rentalDAO = new RentalDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        String idParam = req.getParameter("id");
        String codeParam = req.getParameter("code");
        String query = req.getParameter("query");
        String status = req.getParameter("status");

        if (idParam != null && !idParam.isEmpty()) {
            try {
                int id = Integer.parseInt(idParam);
                Rental r = rentalDAO.getRentalById(id);
                if (r != null) {
                    out.print("{\"success\": true, \"rental\": " + rentalToJson(r) + "}");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\": false, \"message\": \"Rental receipt not found\"}");
                }
            } catch (Exception e) {
                sendError(resp, out, "Invalid rental ID.");
            }
            return;
        }

        if (codeParam != null && !codeParam.isEmpty()) {
            Rental r = rentalDAO.getRentalByCode(codeParam);
            if (r != null) {
                out.print("{\"success\": true, \"rental\": " + rentalToJson(r) + "}");
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"success\": false, \"message\": \"Rental code not found\"}");
            }
            return;
        }

        List<Rental> list = rentalDAO.searchRentals(query, status);
        StringBuilder json = new StringBuilder();
        json.append("{\"success\": true, \"rentals\": [");
        for (int i = 0; i < list.size(); i++) {
            json.append(rentalToJson(list.get(i)));
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

        String custParam = req.getParameter("customerId");
        String vehParam = req.getParameter("vehicleId");
        String daysParam = req.getParameter("days");

        if (custParam == null && req.getContentType() != null && req.getContentType().contains("application/json")) {
            Map<String, String> bodyMap = parseJsonBody(req);
            custParam = bodyMap.get("customerId");
            vehParam = bodyMap.get("vehicleId");
            daysParam = bodyMap.get("days");
        }

        // --- Mandatory Server-side Validation ---
        if (custParam == null || custParam.trim().isEmpty()) {
            sendError(resp, out, "Please select a customer.");
            return;
        }
        int customerId;
        try {
            customerId = Integer.parseInt(custParam.trim());
        } catch (NumberFormatException e) {
            sendError(resp, out, "Invalid customer selection.");
            return;
        }

        if (vehParam == null || vehParam.trim().isEmpty()) {
            sendError(resp, out, "Please select an available vehicle.");
            return;
        }
        int vehicleId;
        try {
            vehicleId = Integer.parseInt(vehParam.trim());
        } catch (NumberFormatException e) {
            sendError(resp, out, "Invalid vehicle selection.");
            return;
        }

        if (daysParam == null || daysParam.trim().isEmpty()) {
            sendError(resp, out, "Number of rental days is required.");
            return;
        }
        int days;
        try {
            days = Integer.parseInt(daysParam.trim());
            if (days <= 0) {
                sendError(resp, out, "Number of days must be an integer greater than zero.");
                return;
            }
        } catch (NumberFormatException e) {
            sendError(resp, out, "Rental days must be a valid integer number.");
            return;
        }

        try {
            Rental rental = rentalDAO.processRental(customerId, vehicleId, days);
            out.print("{\"success\": true, \"message\": \"Booking Successful!\", \"rental\": " + rentalToJson(rental) + "}");
            out.flush();

        } catch (SQLException e) {
            sendError(resp, out, e.getMessage());
        } catch (Exception e) {
            sendError(resp, out, "Unexpected error processing rental: " + e.getMessage());
        }
    }

    private void sendError(HttpServletResponse resp, PrintWriter out, String message) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        out.print("{\"success\": false, \"message\": \"" + escapeJson(message) + "\"}");
        out.flush();
    }

    private String rentalToJson(Rental r) {
        return "{" +
                "\"id\": " + r.getId() + "," +
                "\"rentalCode\": \"" + escapeJson(r.getRentalCode()) + "\"," +
                "\"customerId\": " + r.getCustomerId() + "," +
                "\"customerName\": \"" + escapeJson(r.getCustomerName()) + "\"," +
                "\"customerAge\": " + r.getCustomerAge() + "," +
                "\"customerPhone\": \"" + escapeJson(r.getCustomerPhone()) + "\"," +
                "\"customerLicense\": \"" + escapeJson(r.getCustomerLicense()) + "\"," +
                "\"vehicleId\": " + r.getVehicleId() + "," +
                "\"vehicleCode\": \"" + escapeJson(r.getVehicleCode()) + "\"," +
                "\"vehicleName\": \"" + escapeJson(r.getVehicleName()) + "\"," +
                "\"vehicleNumber\": \"" + escapeJson(r.getVehicleNumber()) + "\"," +
                "\"vehicleType\": \"" + escapeJson(r.getVehicleType()) + "\"," +
                "\"rentPerDay\": " + r.getRentPerDay() + "," +
                "\"rentalDate\": \"" + (r.getRentalDate() != null ? r.getRentalDate().toString() : "") + "\"," +
                "\"returnDate\": \"" + (r.getReturnDate() != null ? r.getReturnDate().toString() : "") + "\"," +
                "\"days\": " + r.getDays() + "," +
                "\"totalAmount\": " + r.getTotalAmount() + "," +
                "\"status\": \"" + escapeJson(r.getStatus()) + "\"" +
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
