package model;

/**
 * Model class representing a Customer in the Vehicle Rental Management System.
 */
public class Customer {
    private int id;
    private String name;
    private int age;
    private String phone;
    private String licenseNumber;

    public Customer() {
    }

    public Customer(int id, String name, int age, String phone, String licenseNumber) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
    }

    public Customer(String name, int age, String phone, String licenseNumber) {
        this.name = name;
        this.age = age;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }
}
