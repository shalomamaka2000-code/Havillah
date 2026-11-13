package havillah_backend.service;

import havillah_backend.dto.VendorRequest;
import havillah_backend.dto.VendorResponse;
import havillah_backend.entity.Vendor;
import havillah_backend.exception.DuplicateResourceException;
import havillah_backend.exception.ResourceNotFoundException;
import havillah_backend.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock
    private VendorRepository vendorRepository;

    @InjectMocks
    private VendorService vendorService;

    private Vendor vendor;

    @BeforeEach
    void setUp() {
        vendor = new Vendor(
                "Tech World",
                "tech@example.com",
                "08012345678",
                "Lagos"
        );
        vendor.setId(5L);
    }

    @Test
    void createVendorSuccessfully() {
        VendorRequest request = request(
                "Tech World", "tech@example.com", "08012345678", "Lagos"
        );

        when(vendorRepository.existsByEmail("tech@example.com")).thenReturn(false);
        when(vendorRepository.save(any(Vendor.class)))
                .thenAnswer(invocation -> {
                    Vendor saved = invocation.getArgument(0);
                    saved.setId(5L);
                    return saved;
                });

        VendorResponse response = vendorService.createVendor(request);

        assertEquals(5L, response.getId());
        assertEquals("Tech World", response.getBusinessName());
        assertEquals("tech@example.com", response.getEmail());
        assertEquals("08012345678", response.getPhone());
        assertEquals("Lagos", response.getAddress());
        assertTrue(response.getActive());
        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    void createVendorWithExistingEmailIsRejected() {
        VendorRequest request = request(
                "Tech World", "tech@example.com", "08012345678", "Lagos"
        );

        when(vendorRepository.existsByEmail("tech@example.com")).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> vendorService.createVendor(request)
        );

        verify(vendorRepository, never()).save(any(Vendor.class));
    }

    @Test
    void getAllVendorsReturnsVendors() {
        when(vendorRepository.findAll()).thenReturn(List.of(vendor));

        List<VendorResponse> response = vendorService.getAllVendors();

        assertEquals(1, response.size());
        assertEquals("Tech World", response.get(0).getBusinessName());
        assertEquals("tech@example.com", response.get(0).getEmail());
    }

    @Test
    void getVendorByIdReturnsVendor() {
        when(vendorRepository.findById(5L)).thenReturn(Optional.of(vendor));

        VendorResponse response = vendorService.getVendorById(5L);

        assertEquals(5L, response.getId());
        assertEquals("Tech World", response.getBusinessName());
        assertEquals("tech@example.com", response.getEmail());
    }

    @Test
    void getMissingVendorIsRejected() {
        when(vendorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vendorService.getVendorById(999L)
        );
    }

    @Test
    void updateVendorSuccessfully() {
        VendorRequest request = request(
                "Tech World NG",
                "sales@techworld.com",
                "08087654321",
                "Abuja"
        );

        when(vendorRepository.findById(5L)).thenReturn(Optional.of(vendor));
        when(vendorRepository.save(vendor)).thenReturn(vendor);

        VendorResponse response = vendorService.updateVendor(5L, request);

        assertEquals("Tech World NG", response.getBusinessName());
        assertEquals("sales@techworld.com", response.getEmail());
        assertEquals("08087654321", response.getPhone());
        assertEquals("Abuja", response.getAddress());
        verify(vendorRepository).save(vendor);
    }

    @Test
    void updateMissingVendorIsRejected() {
        VendorRequest request = request(
                "Tech World NG",
                "sales@techworld.com",
                "08087654321",
                "Abuja"
        );

        when(vendorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vendorService.updateVendor(999L, request)
        );

        verify(vendorRepository, never()).save(any(Vendor.class));
    }

    @Test
    void deleteVendorSuccessfully() {
        when(vendorRepository.existsById(5L)).thenReturn(true);

        assertDoesNotThrow(() -> vendorService.deleteVendor(5L));

        verify(vendorRepository).deleteById(5L);
    }

    @Test
    void deleteMissingVendorIsRejected() {
        when(vendorRepository.existsById(999L)).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> vendorService.deleteVendor(999L)
        );

        verify(vendorRepository, never()).deleteById(anyLong());
    }

    private VendorRequest request(
            String businessName,
            String email,
            String phone,
            String address
    ) {
        VendorRequest request = new VendorRequest();
        request.setBusinessName(businessName);
        request.setEmail(email);
        request.setPhone(phone);
        request.setAddress(address);
        return request;
    }
}
