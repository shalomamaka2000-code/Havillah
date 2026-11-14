package havillah_backend.controller;

import havillah_backend.dto.VendorRequest;
import havillah_backend.dto.VendorResponse;
import havillah_backend.security.CustomUserDetailsService;
import havillah_backend.security.JwtService;
import havillah_backend.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VendorController.class)
@AutoConfigureMockMvc(addFilters = false)
class VendorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VendorService vendorService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private VendorResponse sampleVendor() {
        return new VendorResponse(
                1L,
                "Tech Store",
                "vendor@example.com",
                "08012345678",
                "Lagos, Nigeria",
                true
        );
    }

    @Test
    void getAllVendorsReturnsVendorList() throws Exception {
        when(vendorService.getAllVendors()).thenReturn(List.of(sampleVendor()));

        mockMvc.perform(get("/api/vendors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].businessName").value("Tech Store"))
                .andExpect(jsonPath("$[0].email").value("vendor@example.com"));

        verify(vendorService).getAllVendors();
    }

    @Test
    void getVendorByIdReturnsVendor() throws Exception {
        when(vendorService.getVendorById(1L)).thenReturn(sampleVendor());

        mockMvc.perform(get("/api/vendors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.businessName").value("Tech Store"))
                .andExpect(jsonPath("$.active").value(true));

        verify(vendorService).getVendorById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createVendorReturnsCreatedForAdmin() throws Exception {
        when(vendorService.createVendor(any(VendorRequest.class))).thenReturn(sampleVendor());

        mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessName": "Tech Store",
                                  "email": "vendor@example.com",
                                  "phone": "08012345678",
                                  "address": "Lagos, Nigeria"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.businessName").value("Tech Store"));

        verify(vendorService).createVendor(any(VendorRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createVendorReturnsBadRequestWhenRequiredFieldsAreInvalid() throws Exception {
        mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessName": "",
                                  "email": "not-an-email",
                                  "phone": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(vendorService, never()).createVendor(any(VendorRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateVendorReturnsUpdatedVendor() throws Exception {
        when(vendorService.updateVendor(eq(1L), any(VendorRequest.class)))
                .thenReturn(sampleVendor());

        mockMvc.perform(put("/api/vendors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessName": "Tech Store",
                                  "email": "vendor@example.com",
                                  "phone": "08012345678",
                                  "address": "Lagos, Nigeria"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Tech Store"));

        verify(vendorService).updateVendor(eq(1L), any(VendorRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteVendorReturnsNoContent() throws Exception {
        doNothing().when(vendorService).deleteVendor(1L);

        mockMvc.perform(delete("/api/vendors/1"))
                .andExpect(status().isNoContent());

        verify(vendorService).deleteVendor(1L);
    }
}
