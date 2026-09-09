package havillah_backend.dto;
import lombok.Data;

        @Data

public class VendorResponse {

    private Long id;
    private String businessName;
    private String email;
    private String phone;
    private String address;
    private Boolean active;

    public VendorResponse(
            Long id,
            String businessName,
            String email,
            String phone,
            String address,
            Boolean active
    ) {
        this.id = id;
        this.businessName = businessName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.active = active;
    }


}