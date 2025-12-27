package com.azedcods.home_buddy_v2.controller;

import com.azedcods.home_buddy_v2.enums.AppRole;
import com.azedcods.home_buddy_v2.model.auth.Role;
import com.azedcods.home_buddy_v2.model.auth.User;
import com.azedcods.home_buddy_v2.repository.auth.RoleRepository;
import com.azedcods.home_buddy_v2.repository.auth.UserRepository;
import com.azedcods.home_buddy_v2.security.jwt.JwtUtils;
import com.azedcods.home_buddy_v2.security.request.LoginRequest;
import com.azedcods.home_buddy_v2.security.request.SignupRequest;
import com.azedcods.home_buddy_v2.security.response.MessageResponse;
import com.azedcods.home_buddy_v2.security.response.UserInfoResponse;
import com.azedcods.home_buddy_v2.security.services.UserDetailsImpl;
import com.azedcods.home_buddy_v2.service.robot.RobotBootstrapService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    private RobotBootstrapService robotBootstrapService;


    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
        } catch (AuthenticationException exception) {
            Map<String, Object> map = new HashMap<>();
            map.put("message", "Bad credentials");
            map.put("status", false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(map);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());

        UserInfoResponse response = new UserInfoResponse(
                userDetails.getId(),
                userDetails.getFullname(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(response);
    }

        @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUserName(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user
        User user = new User(
                signUpRequest.getFullname(),
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword())
        );

        Set<Role> roles = resolveRoles(signUpRequest.getRole());
        user.setRoles(roles);

        User saved = userRepository.save(user);
        robotBootstrapService.getOrCreateRobotForUser(saved);
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    private Set<Role> resolveRoles(Set<String> strRoles) {
        Set<Role> roles = new HashSet<>();

        // Default role
        if (strRoles == null || strRoles.isEmpty()) {
            roles.add(findRole(AppRole.ROLE_USER));
            return roles;
        }

        for (String r : strRoles) {
            String role = (r == null) ? "" : r.trim().toLowerCase();

            switch (role) {
                case "admin" -> roles.add(findRole(AppRole.ROLE_ADMIN));
                case "caregiver" -> roles.add(findRole(AppRole.ROLE_CAREGIVER));
                default -> roles.add(findRole(AppRole.ROLE_USER));
            }
        }

        return roles;
    }

    private Role findRole(AppRole appRole) {
        return roleRepository.findByRoleName(appRole)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found: " + appRole));
    }

    @GetMapping("/username")
    public String currentUserName(Authentication authentication) {
        if (authentication != null) return authentication.getName();
        return "";
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserDetails(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetailsImpl userDetails)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        UserInfoResponse response = new UserInfoResponse(
                userDetails.getId(),
                userDetails.getFullname(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        );

        return ResponseEntity.ok(response);
    }


    @PostMapping("/signout")
    public ResponseEntity<?> signoutUser() {
        ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new MessageResponse("You've been signed out!"));

    }
}
