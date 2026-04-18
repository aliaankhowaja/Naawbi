package com.ibwaan.naawbi.model;

public class Session {
    private static Session instance;
    private User currentUser;

    private Session() {}

    public static Session getInstance() {
        if (instance == null) {
            synchronized (Session.class) {
                if (instance == null) {
                    instance = new Session();
                }
            }
        }
        return instance;
    }

    public void login(User user)  { this.currentUser = user; }
    public void logout()          { this.currentUser = null; }
    public boolean isLoggedIn()   { return currentUser != null; }
    public int getUserId() {
        if (currentUser == null) throw new IllegalStateException("No user logged in");
        return currentUser.getId();
    }
    public String getUsername() {
        if (currentUser == null) throw new IllegalStateException("No user logged in");
        return currentUser.getUsername();
    }
    public String getRole() {
        if (currentUser == null) throw new IllegalStateException("No user logged in");
        return currentUser.getRole();
    }
    public boolean isInstructor() {
        if (currentUser == null) throw new IllegalStateException("No user logged in");
        return "instructor".equals(currentUser.getRole());
    }
}
