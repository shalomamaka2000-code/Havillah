package havillah_backend.dto;

public class VendorRegistrationResponse {

    private Long userId;
    private Long vendorId;
    private String email;
    private String businessName;
    private String role;

    public VendorRegistrationResponse(
            Long userId,
            Long vendorId,
            String email,
            String businessName,
            String role
    ) {
        this.userId = userId;
        this.vendorId = vendorId;
        this.email = email;
        this.businessName = businessName;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public String getEmail() {
        return email;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getRole() {
        return role;
    }
}
