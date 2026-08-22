package havillah_backend.dto;

import lombok.Data;

@Data

public class LoginResponse {

    private String token;
    private String email;
    private String role;

    public LoginResponse() {
    }

    public LoginResponse(String token, String email, String role) {
        this.token = token;
        this.email = email;
        this.role = role;
    }

}