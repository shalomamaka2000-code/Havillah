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

    // Nullable temporarily so existing vendor rows can remain during migration.
    // New vendor registrations always create this relationship.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

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
