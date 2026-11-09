package havillah_backend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {

    public static void main(String[] args) {

        String password = System.getenv("ADMIN_PASSWORD");

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD environment variable is not set"
            );
        }

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        String encodedPassword = encoder.encode(password);

        System.out.println(encodedPassword);
    }
}