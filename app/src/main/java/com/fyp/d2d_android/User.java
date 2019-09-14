package com.fyp.d2d_android;

public class User {
    private String FirstName;
    private String LastName;
    private int Age;

    public String getFirstName() {
        return FirstName;
    }

    public void setFirstName(String firstName) {
        FirstName = firstName;
    }

    public String getLastName() {
        return LastName;
    }

    public void setLastName(String lastName) {
        LastName = lastName;
    }

    public int getAge() {
        return Age;
    }

    public void setAge(int age) {
        Age = age;
    }

    public User create(){
        User user =new User();
        user.setFirstName("default");
        user.setLastName("default");
        user.setAge(0);
        return user;
    }
}