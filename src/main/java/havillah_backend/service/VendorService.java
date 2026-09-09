package havillah_backend.service;

import havillah_backend.dto.VendorRequest;
import havillah_backend.dto.VendorResponse;
import havillah_backend.entity.Vendor;
import havillah_backend.repository.VendorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    public VendorResponse createVendor(VendorRequest request) {

        if (vendorRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Vendor email already exists");
        }

        Vendor vendor = new Vendor();

        vendor.setBusinessName(request.getBusinessName());
        vendor.setEmail(request.getEmail());
        vendor.setPhone(request.getPhone());
        vendor.setAddress(request.getAddress());

        Vendor savedVendor = vendorRepository.save(vendor);

        return new VendorResponse(
                savedVendor.getId(),
                savedVendor.getBusinessName(),
                savedVendor.getEmail(),
                savedVendor.getPhone(),
                savedVendor.getAddress(),
                savedVendor.getActive()
        );
    }

    public List<VendorResponse> getAllVendors() {

        return vendorRepository.findAll()
                .stream()
                .map(vendor -> new VendorResponse(
                        vendor.getId(),
                        vendor.getBusinessName(),
                        vendor.getEmail(),
                        vendor.getPhone(),
                        vendor.getAddress(),
                        vendor.getActive()
                ))
                .toList();
    }
}
