package servlet;

import dao.VehicleDAO;
import model.Bike;
import model.Car;
import model.Truck;
import model.Vehicle;

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
 * Servlet endpoint for managing Vehicles (GET, POST, PUT, DELETE).
 * Enforces strict server-side validation.
 */
@WebServlet("/api/vehicles")
public class VehicleServlet extends HttpServlet {

    private final VehicleDAO vehicleDAO = new VehicleDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String query = req.getParameter("query");
        String type = req.getParameter("type");
        String availability = req.getParameter("availability");
        String idParam = req.getParameter("id");

        PrintWriter out = resp.getWriter();

        if (idParam != null && !idParam.isEmpty()) {
            try {
                int id = Integer.parseInt(idParam);
                Vehicle v = vehicleDAO.getVehicleById(id);
                if (v != null) {
                    out.print("{\"success\": true, \"vehicle\": " + vehicleToJson(v) + "}");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\": false, \"message\": \"Vehicle not found\"}");
                }
            } catch (NumberFormatException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\": false, \"message\": \"Invalid vehicle ID\"}");
            }
            return;
        }

        List<Vehicle> list = vehicleDAO.searchAndFilterVehicles(query, type, availability);
        StringBuilder json = new StringBuilder();
        json.append("{\"success\": true, \"vehicles\": [");
        for (int i = 0; i < list.size(); i++) {
            json.append(vehicleToJson(list.get(i)));
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

        String code = req.getParameter("code");
        String name = req.getParameter("name");
        String vehicleNumber = req.getParameter("vehicleNumber");
        String type = req.getParameter("type");
        String rentParam = req.getParameter("rentPerDay");
        String action = req.getParameter("action");
        String idParam = req.getParameter("id");

        // Handle JSON body if form params are empty
        if (code == null && req.getContentType() != null && req.getContentType().contains("application/json")) {
            Map<String, String> params = parseJsonBody(req);
            code = params.get("code");
            name = params.get("name");
            vehicleNumber = params.get("vehicleNumber");
            type = params.get("type");
            rentParam = params.get("rentPerDay");
            action = params.get("action");
            idParam = params.get("id");
        }

        if ("DELETE".equalsIgnoreCase(action)) {
            doDeleteById(idParam, resp, out);
            return;
        }

        if ("UPDATE".equalsIgnoreCase(action) || (idParam != null && !idParam.isEmpty())) {
            doUpdateVehicle(idParam, code, name, vehicleNumber, type, rentParam, resp, out);
            return;
        }

        // --- Server-side Validation for Add Vehicle ---
        if (code == null || code.trim().isEmpty()) {
            sendError(resp, out, "Vehicle code cannot be empty.");
            return;
        }
        code = code.trim().toUpperCase();

        if (name == null || name.trim().isEmpty()) {
            sendError(resp, out, "Vehicle name cannot be empty.");
            return;
        }
        name = name.trim();

        if (vehicleNumber == null || vehicleNumber.trim().isEmpty()) {
            sendError(resp, out, "Vehicle number cannot be empty.");
            return;
        }
        vehicleNumber = vehicleNumber.trim().toUpperCase();

        if (type == null || type.trim().isEmpty()) {
            sendError(resp, out, "Vehicle type must be selected.");
            return;
        }
        type = type.trim();

        double rentPerDay = 0.0;
        try {
            rentPerDay = Double.parseDouble(rentParam);
            if (rentPerDay <= 0) {
                sendError(resp, out, "Rent per day must be a positive number.");
                return;
            }
        } catch (Exception e) {
            sendError(resp, out, "Invalid rent per day format.");
            return;
        }

        if (vehicleDAO.isCodeExists(code, 0)) {
            sendError(resp, out, "Vehicle code '" + code + "' already exists in database.");
            return;
        }

        if (vehicleDAO.isVehicleNumberExists(vehicleNumber, 0)) {
            sendError(resp, out, "Vehicle registration number '" + vehicleNumber + "' already exists.");
            return;
        }

        // Instantiate concrete subclass (Polymorphism)
        Vehicle v;
        if ("Bike".equalsIgnoreCase(type)) {
            v = new Bike(code, name, vehicleNumber, rentPerDay, true);
        } else if ("Truck".equalsIgnoreCase(type)) {
            v = new Truck(code, name, vehicleNumber, rentPerDay, true);
        } else {
            v = new Car(code, name, vehicleNumber, rentPerDay, true);
        }

        try {
            boolean success = vehicleDAO.addVehicle(v);
            if (success) {
                out.print("{\"success\": true, \"message\": \"Vehicle added successfully!\", \"vehicle\": " + vehicleToJson(v) + "}");
            } else {
                sendError(resp, out, "Failed to insert vehicle into database.");
            }
        } catch (SQLException e) {
            sendError(resp, out, "Database error: " + e.getMessage());
        }
    }

    private void doUpdateVehicle(String idParam, String code, String name, String vehicleNumber, String type, String rentParam, HttpServletResponse resp, PrintWriter out) {
        try {
            int id = Integer.parseInt(idParam);
            Vehicle existing = vehicleDAO.getVehicleById(id);
            if (existing == null) {
                sendError(resp, out, "Vehicle not found.");
                return;
            }

            if (code != null && !code.trim().isEmpty()) {
                code = code.trim().toUpperCase();
                if (vehicleDAO.isCodeExists(code, id)) {
                    sendError(resp, out, "Vehicle code '" + code + "' already exists.");
                    return;
                }
                existing.setCode(code);
            }

            if (name != null && !name.trim().isEmpty()) {
                existing.setName(name.trim());
            }

            if (vehicleNumber != null && !vehicleNumber.trim().isEmpty()) {
                vehicleNumber = vehicleNumber.trim().toUpperCase();
                if (vehicleDAO.isVehicleNumberExists(vehicleNumber, id)) {
                    sendError(resp, out, "Vehicle number '" + vehicleNumber + "' already exists.");
                    return;
                }
                existing.setVehicleNumber(vehicleNumber);
            }

            if (rentParam != null && !rentParam.trim().isEmpty()) {
                double rent = Double.parseDouble(rentParam);
                if (rent <= 0) {
                    sendError(resp, out, "Rent per day must be greater than zero.");
                    return;
                }
                existing.setRentPerDay(rent);
            }

            boolean updated = vehicleDAO.updateVehicle(existing);
            if (updated) {
                out.print("{\"success\": true, \"message\": \"Vehicle updated successfully!\", \"vehicle\": " + vehicleToJson(existing) + "}");
            } else {
                sendError(resp, out, "Failed to update vehicle in database.");
            }

        } catch (Exception e) {
            sendError(resp, out, "Error updating vehicle: " + e.getMessage());
        }
    }

    private void doDeleteById(String idParam, HttpServletResponse resp, PrintWriter out) {
        if (idParam == null || idParam.isEmpty()) {
            sendError(resp, out, "Vehicle ID required for deletion.");
            return;
        }
        try {
            int id = Integer.parseInt(idParam);
            boolean deleted = vehicleDAO.deleteVehicle(id);
            if (deleted) {
                out.print("{\"success\": true, \"message\": \"Vehicle deleted successfully.\"}");
            } else {
                sendError(resp, out, "Vehicle could not be deleted.");
            }
        } catch (SQLException e) {
            sendError(resp, out, e.getMessage());
        } catch (Exception e) {
            sendError(resp, out, "Invalid vehicle ID format.");
        }
    }

    private void sendError(HttpServletResponse resp, PrintWriter out, String message) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        out.print("{\"success\": false, \"message\": \"" + escapeJson(message) + "\"}");
    }

    private String vehicleToJson(Vehicle v) {
        return "{" +
                "\"id\": " + v.getId() + "," +
                "\"code\": \"" + escapeJson(v.getCode()) + "\"," +
                "\"name\": \"" + escapeJson(v.getName()) + "\"," +
                "\"vehicleNumber\": \"" + escapeJson(v.getVehicleNumber()) + "\"," +
                "\"type\": \"" + escapeJson(v.getVehicleType()) + "\"," +
                "\"rentPerDay\": " + v.getRentPerDay() + "," +
                "\"available\": " + v.isAvailable() +
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
