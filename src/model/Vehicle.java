package model;

/**
 * Abstract Vehicle class demonstrating Object-Oriented Programming principles:
 * - Abstraction: Contains abstract method getVehicleType()
 * - Encapsulation: Private instance variables accessible via getters/setters
 */
public abstract class Vehicle {
    private int id;
    private String code;
    private String name;
    private String vehicleNumber;
    private double rentPerDay;
    private boolean available;

    public Vehicle() {
    }

    public Vehicle(int id, String code, String name, String vehicleNumber, double rentPerDay, boolean available) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.vehicleNumber = vehicleNumber;
        this.rentPerDay = rentPerDay;
        this.available = available;
    }

    public Vehicle(String code, String name, String vehicleNumber, double rentPerDay, boolean available) {
        this.code = code;
        this.name = name;
        this.vehicleNumber = vehicleNumber;
        this.rentPerDay = rentPerDay;
        this.available = available;
    }

    // Abstract method to be overridden by subclasses (Polymorphism)
    public abstract String getVehicleType();

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public double getRentPerDay() {
        return rentPerDay;
    }

    public void setRentPerDay(double rentPerDay) {
        this.rentPerDay = rentPerDay;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return getVehicleType() + "{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", vehicleNumber='" + vehicleNumber + '\'' +
                ", rentPerDay=" + rentPerDay +
                ", available=" + available +
                '}';
    }
}
