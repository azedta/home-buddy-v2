package com.azedcods.home_buddy_v2.controller;

import com.azedcods.home_buddy_v2.model.AppRole;
import com.azedcods.home_buddy_v2.model.Role;
import com.azedcods.home_buddy_v2.model.User;
import com.azedcods.home_buddy_v2.repository.RoleRepository;
import com.azedcods.home_buddy_v2.repository.UserRepository;
import com.azedcods.home_buddy_v2.security.jwt.JwtUtils;
import com.azedcods.home_buddy_v2.security.request.LoginRequest;
import com.azedcods.home_buddy_v2.security.request.SignupRequest;
import com.azedcods.home_buddy_v2.security.response.MessageResponse;
import com.azedcods.home_buddy_v2.security.response.UserInfoResponse;
import com.azedcods.home_buddy_v2.security.services.UserDetailsImpl;
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

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
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
            return new ResponseEntity<>(map, HttpStatus.NOT_FOUND); // keeping your current behavior
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
                roles,
                jwtCookie.toString()
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

        userRepository.save(user);

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
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        UserInfoResponse response = new UserInfoResponse(
                userDetails.getId(),
                userDetails.getFullname(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        );


        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/signout")
    public ResponseEntity<?> signoutUser() {
        ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new MessageResponse("You've been signed out!"));
    }
}
