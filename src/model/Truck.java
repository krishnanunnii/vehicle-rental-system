package model;

/**
 * Concrete subclass representing a Truck.
 * Demonstrates Inheritance and Method Overriding.
 */
public class Truck extends Vehicle {

    public Truck() {
        super();
    }

    public Truck(int id, String code, String name, String vehicleNumber, double rentPerDay, boolean available) {
        super(id, code, name, vehicleNumber, rentPerDay, available);
    }

    public Truck(String code, String name, String vehicleNumber, double rentPerDay, boolean available) {
        super(code, name, vehicleNumber, rentPerDay, available);
    }

    @Override
    public String getVehicleType() {
        return "Truck";
    }
}
