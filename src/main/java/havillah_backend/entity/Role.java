
package havillah_backend.entity;

import jakarta.persistence.*;
/*JPA ANNOTATION= @Entity tells jpa this is a java class needed to be stored in a db */
    @Entity
    //@Table tells jpa to store the db in a table called Role
    @Table(name = "roles")
    public class Role {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, unique = true)
        private String name;

        public Role() {}

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
