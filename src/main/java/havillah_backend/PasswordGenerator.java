package havillah_backend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        String password = "NwwAdmin123";

        String encodedPassword =
                encoder.encode(password);

        System.out.println(encodedPassword);
    }
}