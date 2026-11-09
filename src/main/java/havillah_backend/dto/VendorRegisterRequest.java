package havillah_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorRegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 80, message = "First name must not exceed 80 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 80, message = "Last name must not exceed 80 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "A valid email address is required")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Size(max = 25, message = "Phone number must not exceed 25 characters")
    private String phone;

    @NotBlank(message = "Business name is required")
    @Size(max = 120, message = "Business name must not exceed 120 characters")
    private String businessName;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;
}
