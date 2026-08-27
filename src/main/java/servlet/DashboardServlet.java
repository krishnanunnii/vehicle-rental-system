package servlet;

import dao.RentalDAO;
import dao.VehicleDAO;
import model.Vehicle;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Servlet endpoint for Dashboard statistics and vehicle list summary.
 */
@WebServlet("/api/dashboard")
public class DashboardServlet extends HttpServlet {

    private final RentalDAO rentalDAO = new RentalDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> metrics = rentalDAO.getDashboardMetrics();
        List<Vehicle> vehicles = vehicleDAO.getAllVehicles();

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"success\": true,");
        json.append("\"metrics\": {");
        json.append("\"totalVehicles\": ").append(metrics.get("totalVehicles")).append(",");
        json.append("\"availableVehicles\": ").append(metrics.get("availableVehicles")).append(",");
        json.append("\"rentedVehicles\": ").append(metrics.get("rentedVehicles")).append(",");
        json.append("\"totalCustomers\": ").append(metrics.get("totalCustomers")).append(",");
        json.append("\"activeRentals\": ").append(metrics.get("activeRentals")).append(",");
        json.append("\"totalRevenue\": ").append(metrics.get("totalRevenue"));
        json.append("},");

        json.append("\"vehicles\": [");
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle v = vehicles.get(i);
            json.append("{");
            json.append("\"id\": ").append(v.getId()).append(",");
            json.append("\"code\": \"").append(escapeJson(v.getCode())).append("\",");
            json.append("\"name\": \"").append(escapeJson(v.getName())).append("\",");
            json.append("\"vehicleNumber\": \"").append(escapeJson(v.getVehicleNumber())).append("\",");
            json.append("\"type\": \"").append(escapeJson(v.getVehicleType())).append("\",");
            json.append("\"rentPerDay\": ").append(v.getRentPerDay()).append(",");
            json.append("\"available\": ").append(v.isAvailable());
            json.append("}");
            if (i < vehicles.size() - 1) json.append(",");
        }
        json.append("]");
        json.append("}");

        PrintWriter out = resp.getWriter();
        out.print(json.toString());
        out.flush();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
