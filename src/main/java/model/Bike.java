package model;

/**
 * Concrete subclass representing a Bike.
 * Demonstrates Inheritance and Method Overriding.
 */
public class Bike extends Vehicle {

    public Bike() {
        super();
    }

    public Bike(int id, String code, String name, String vehicleNumber, double rentPerDay, boolean available) {
        super(id, code, name, vehicleNumber, rentPerDay, available);
    }

    public Bike(String code, String name, String vehicleNumber, double rentPerDay, boolean available) {
        super(code, name, vehicleNumber, rentPerDay, available);
    }

    @Override
    public String getVehicleType() {
        return "Bike";
    }
}
