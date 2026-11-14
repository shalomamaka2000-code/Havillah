package havillah_backend.controller;

import havillah_backend.security.CustomUserDetailsService;
import havillah_backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoleTestController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoleTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UsernamePasswordAuthenticationToken principal(String role) {
        return new UsernamePasswordAuthenticationToken(
                role.toLowerCase() + "@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    @Test
    void adminEndpointReturnsAccessGranted() throws Exception {
        mockMvc.perform(get("/api/test/admin").principal(principal("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string("ADMIN access granted!"));
    }

    @Test
    void customerEndpointReturnsAccessGranted() throws Exception {
        mockMvc.perform(get("/api/test/customer").principal(principal("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(content().string("CUSTOMER access granted!"));
    }

    @Test
    void vendorEndpointReturnsAccessGranted() throws Exception {
        mockMvc.perform(get("/api/test/vendor").principal(principal("VENDOR")))
                .andExpect(status().isOk())
                .andExpect(content().string("VENDOR access granted!"));
    }
}
