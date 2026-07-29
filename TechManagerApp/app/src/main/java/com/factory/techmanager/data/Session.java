package com.factory.techmanager.data;

public class Session {
    public String deptId; // "admin" or a dept id
    public String role;   // "admin" | "dept"

    public Session(String deptId, String role) {
        this.deptId = deptId;
        this.role = role;
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }
}
