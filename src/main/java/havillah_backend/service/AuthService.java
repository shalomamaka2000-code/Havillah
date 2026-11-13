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
import havillah_backend.exception.ResourceNotFoundException;
import havillah_backend.repository.RoleRepository;
import havillah_backend.repository.UserRepository;
import havillah_backend.repository.VendorRepository;
import havillah_backend.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final VendorRepository vendorRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository,
            RoleRepository roleRepository,
            VendorRepository vendorRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.vendorRepository = vendorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizedEmail,
                            request.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtService.generateToken(userDetails);

            User user = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            log.info("User authenticated successfully. Role: {}", user.getRole().getName());

            return new LoginResponse(token, user.getEmail(), user.getRole().getName());
        } catch (AuthenticationException ex) {
            log.warn("Authentication failed");
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid email or password"
            );
        }
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Customer registration rejected: email already exists");
            throw new DuplicateResourceException("Email already exists");
        }

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Customer role not found"));

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(normalizedEmail);
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(customerRole);

        User savedUser = userRepository.save(user);
        log.info("Customer account registered successfully. User ID: {}", savedUser.getId());

        return mapUserResponse(savedUser);
    }

    @Transactional
    public VendorRegistrationResponse registerVendor(VendorRegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)
                || vendorRepository.existsByEmail(normalizedEmail)) {
            log.warn("Vendor registration rejected: email already exists");
            throw new DuplicateResourceException("Email already exists");
        }

        Role vendorRole = roleRepository.findByName("VENDOR")
                .orElseThrow(() -> new ResourceNotFoundException("Vendor role not found"));

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(normalizedEmail);
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(vendorRole);

        User savedUser = userRepository.save(user);

        Vendor vendor = new Vendor();
        vendor.setBusinessName(request.getBusinessName().trim());
        vendor.setEmail(normalizedEmail);
        vendor.setPhone(request.getPhone());
        vendor.setAddress(request.getAddress());
        vendor.setActive(true);
        vendor.setUser(savedUser);

        Vendor savedVendor = vendorRepository.save(vendor);

        log.info("Vendor account registered successfully. Vendor ID: {}", savedVendor.getId());

        return new VendorRegistrationResponse(
                savedUser.getId(),
                savedVendor.getId(),
                savedUser.getEmail(),
                savedVendor.getBusinessName(),
                savedUser.getRole().getName()
        );
    }

    private UserResponse mapUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole().getName());
        return response;
    }
}
