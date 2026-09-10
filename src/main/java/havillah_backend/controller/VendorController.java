package havillah_backend.controller;

import havillah_backend.dto.VendorRequest;
import havillah_backend.dto.VendorResponse;
import havillah_backend.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping
    public ResponseEntity<VendorResponse> createVendor(
            @Valid @RequestBody VendorRequest request
    ) {
        return ResponseEntity.ok(
                vendorService.createVendor(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<VendorResponse>> getAllVendors() {
        return ResponseEntity.ok(
                vendorService.getAllVendors()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorResponse> getVendorById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                vendorService.getVendorById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<VendorResponse> updateVendor(
            @PathVariable Long id,
            @Valid @RequestBody VendorRequest request
    ) {
        return ResponseEntity.ok(
                vendorService.updateVendor(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVendor(
            @PathVariable Long id
    ) {
        vendorService.deleteVendor(id);

        return ResponseEntity.noContent().build();
    }
}
