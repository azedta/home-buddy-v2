package com.azedcods.home_buddy_v2.security.response;

import java.util.List;

public class UserInfoResponse {
    private Long id;
    private String fullname;
    private String username;
    private String email;
    private List<String> roles;

    public UserInfoResponse(
            Long id,
            String fullname,
            String username,
            String email,
            List<String> roles
    ) {
        this.id = id;
        this.fullname = fullname;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }



    public Long getId() { return id; }
    public String getFullname() { return fullname; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public List<String> getRoles() { return roles; }


    public void setId(Long id) { this.id = id; }
    public void setFullname(String fullname) { this.fullname = fullname; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}
