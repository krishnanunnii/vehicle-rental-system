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
 * Servlet endpoint for Vehicle Returns.
 */
@WebServlet("/api/returns")
public class ReturnVehicleServlet extends HttpServlet {

    private final RentalDAO rentalDAO = new RentalDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        List<Rental> activeRentals = rentalDAO.getActiveRentals();
        StringBuilder json = new StringBuilder();
        json.append("{\"success\": true, \"activeRentals\": [");
        for (int i = 0; i < activeRentals.size(); i++) {
            json.append(rentalToJson(activeRentals.get(i)));
            if (i < activeRentals.size() - 1) json.append(",");
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

        String idParam = req.getParameter("rentalId");
        if (idParam == null && req.getContentType() != null && req.getContentType().contains("application/json")) {
            Map<String, String> bodyMap = parseJsonBody(req);
            idParam = bodyMap.get("rentalId");
        }

        if (idParam == null || idParam.trim().isEmpty()) {
            sendError(resp, out, "Rental ID is required for return processing.");
            return;
        }

        try {
            int rentalId = Integer.parseInt(idParam.trim());
            boolean success = rentalDAO.returnVehicle(rentalId);
            if (success) {
                out.print("{\"success\": true, \"message\": \"Vehicle returned successfully! Status updated to Available.\"}");
            } else {
                sendError(resp, out, "Failed to process vehicle return.");
            }
        } catch (SQLException e) {
            sendError(resp, out, e.getMessage());
        } catch (NumberFormatException e) {
            sendError(resp, out, "Invalid rental ID format.");
        }
    }

    private void sendError(HttpServletResponse resp, PrintWriter out, String message) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        out.print("{\"success\": false, \"message\": \"" + escapeJson(message) + "\"}");
    }

    private String rentalToJson(Rental r) {
        return "{" +
                "\"id\": " + r.getId() + "," +
                "\"rentalCode\": \"" + escapeJson(r.getRentalCode()) + "\"," +
                "\"customerId\": " + r.getCustomerId() + "," +
                "\"customerName\": \"" + escapeJson(r.getCustomerName()) + "\"," +
                "\"customerPhone\": \"" + escapeJson(r.getCustomerPhone()) + "\"," +
                "\"vehicleId\": " + r.getVehicleId() + "," +
                "\"vehicleCode\": \"" + escapeJson(r.getVehicleCode()) + "\"," +
                "\"vehicleName\": \"" + escapeJson(r.getVehicleName()) + "\"," +
                "\"vehicleNumber\": \"" + escapeJson(r.getVehicleNumber()) + "\"," +
                "\"vehicleType\": \"" + escapeJson(r.getVehicleType()) + "\"," +
                "\"rentPerDay\": " + r.getRentPerDay() + "," +
                "\"rentalDate\": \"" + (r.getRentalDate() != null ? r.getRentalDate().toString() : "") + "\"," +
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
