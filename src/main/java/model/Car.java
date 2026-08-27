package model;

/**
 * Concrete subclass representing a Car.
 * Demonstrates Inheritance and Method Overriding.
 */
public class Car extends Vehicle {

    public Car() {
        super();
    }

    public Car(int id, String code, String name, String vehicleNumber, double rentPerDay, boolean available) {
        super(id, code, name, vehicleNumber, rentPerDay, available);
    }

    public Car(String code, String name, String vehicleNumber, double rentPerDay, boolean available) {
        super(code, name, vehicleNumber, rentPerDay, available);
    }

    @Override
    public String getVehicleType() {
        return "Car";
    }
}
