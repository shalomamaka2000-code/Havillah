package havillah_backend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class RoleTestController {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        return "ADMIN access granted!";
    }

    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String customerAccess() {
        return "CUSTOMER access granted!";
    }

    @GetMapping("/vendor")
    @PreAuthorize("hasRole('VENDOR')")
    public String vendorAccess() {
        return "VENDOR access granted!";
    }
}