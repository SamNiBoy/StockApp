package com.sn.model;

public class User {
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getAccounttype() {
        return accounttype;
    }
    public void setAccounttype(int accounttype) {
        this.accounttype = accounttype;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public String getMail() {
        return mail;
    }
    public void setMail(String mail) {
        this.mail = mail;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    private String username;
    private String name;
    private int accounttype;
    private String phone;
    private String mail;
    private String address;
    public User(String un, String n, int at, String p, String m, String a)
    {
         username = un;
         name = n;
         accounttype = at;
         phone = p;
         mail = m;
         address = a;
    }
}
