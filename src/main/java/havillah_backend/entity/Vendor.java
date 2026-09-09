package havillah_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@Entity
@Table(name = "vendors")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    private String address;

    @Column(nullable = false)
    private Boolean active = true;

    public Vendor() {
    }

    public Vendor(
            String businessName,
            String email,
            String phone,
            String address
    ) {
        this.businessName = businessName;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }


}