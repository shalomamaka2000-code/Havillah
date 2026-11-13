package havillah_backend.service;

import havillah_backend.dto.LoginRequest;
import havillah_backend.dto.LoginResponse;
import havillah_backend.dto.RegisterRequest;
import havillah_backend.dto.UserResponse;
import havillah_backend.dto.VendorRegisterRequest;
import havillah_backend.dto.VendorRegistrationResponse;
import havillah_backend.entity.Role;
import havillah_backend.entity.User;
import havillah_backend.entity.Vendor;
import havillah_backend.exception.DuplicateResourceException;
import havillah_backend.repository.RoleRepository;
import havillah_backend.repository.UserRepository;
import havillah_backend.repository.VendorRepository;
import havillah_backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCustomerSavesNormalizedEmailAndEncodedPassword() {
        Role customerRole = role("CUSTOMER");
        RegisterRequest request = customerRequest();

        when(userRepository.existsByEmail("customer@example.com")).thenReturn(false);
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("StrongPass123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(101L);
            return user;
        });

        UserResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("customer@example.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals("CUSTOMER", savedUser.getRole().getName());
        assertEquals(101L, response.getId());
        verify(passwordEncoder).encode("StrongPass123");
    }

    @Test
    void registerRejectsDuplicateCustomerEmail() {
        RegisterRequest request = customerRequest();
        when(userRepository.existsByEmail("customer@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
        verify(roleRepository, never()).findByName(anyString());
    }

    @Test
    void registerVendorCreatesLinkedUserAndVendorProfile() {
        Role vendorRole = role("VENDOR");
        VendorRegisterRequest request = vendorRequest();

        when(userRepository.existsByEmail("vendor@example.com")).thenReturn(false);
        when(vendorRepository.existsByEmail("vendor@example.com")).thenReturn(false);
        when(roleRepository.findByName("VENDOR")).thenReturn(Optional.of(vendorRole));
        when(passwordEncoder.encode("StrongPass123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(202L);
            return user;
        });
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(invocation -> {
            Vendor vendor = invocation.getArgument(0);
            vendor.setId(303L);
            return vendor;
        });

        VendorRegistrationResponse response = authService.registerVendor(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<Vendor> vendorCaptor = ArgumentCaptor.forClass(Vendor.class);
        verify(userRepository).save(userCaptor.capture());
        verify(vendorRepository).save(vendorCaptor.capture());

        assertEquals("vendor@example.com", userCaptor.getValue().getEmail());
        assertEquals("encoded-password", userCaptor.getValue().getPassword());
        assertSame(userCaptor.getValue(), vendorCaptor.getValue().getUser());
        assertEquals("My Store", vendorCaptor.getValue().getBusinessName());
        assertEquals(202L, response.getUserId());
        assertEquals(303L, response.getVendorId());
        assertEquals("VENDOR", response.getRole());
    }

    @Test
    void loginReturnsJwtForAuthenticatedUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail(" CUSTOMER@EXAMPLE.COM ");
        request.setPassword("StrongPass123");

        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("customer@example.com")
                .password("encoded-password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();

        var authenticated = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        Role customerRole = role("CUSTOMER");
        User user = new User();
        user.setEmail("customer@example.com");
        user.setRole(customerRole);

        when(authenticationManager.authenticate(any())).thenReturn(authenticated);
        when(jwtService.generateToken(principal)).thenReturn("test.jwt.token");
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));

        LoginResponse response = authService.login(request);

        assertEquals("test.jwt.token", response.getToken());
        assertEquals("customer@example.com", response.getEmail());
        assertEquals("CUSTOMER", response.getRole());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("customer@example.com");
        request.setPassword("wrong-password");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(jwtService, never()).generateToken(any(UserDetails.class));
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    private RegisterRequest customerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Ada");
        request.setLastName("Okafor");
        request.setEmail(" CUSTOMER@EXAMPLE.COM ");
        request.setPassword("StrongPass123");
        request.setPhone("08000000001");
        return request;
    }

    private VendorRegisterRequest vendorRequest() {
        VendorRegisterRequest request = new VendorRegisterRequest();
        request.setFirstName("Tunde");
        request.setLastName("Bello");
        request.setEmail(" VENDOR@EXAMPLE.COM ");
        request.setPassword("StrongPass123");
        request.setPhone("08000000002");
        request.setBusinessName(" My Store ");
        request.setAddress("Lagos");
        return request;
    }
}
