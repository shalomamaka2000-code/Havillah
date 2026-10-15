package havillah_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Import;

@Getter
@Setter


@Entity
@Table(
        /*
        This prevents a customer from saving the same
        product multiple times. Different customers can
        still save that same product.
         */
        name = "wishlist_items",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "product_id"}
        )
)
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /*
    The related user and product are not automatically
    loaded every time a wishlist item is retrieved. We can
    load the data we need through the service layer.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
/*
This records when the product was saved, which will
be useful when displaying the newest wishlist items first.
*/
    @Column(nullable = false, updatable = false)
    private LocalDateTime addedAt;

    public WishlistItem() {
    }

    public WishlistItem(User user, Product product) {
        this.user = user;
        this.product = product;
    }

    @PrePersist
    public void prePersist() {
        this.addedAt = LocalDateTime.now();
    }

}