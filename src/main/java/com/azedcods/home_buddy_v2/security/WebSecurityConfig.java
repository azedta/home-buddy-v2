package com.azedcods.home_buddy_v2.security;

import com.azedcods.home_buddy_v2.enums.AppRole;
import com.azedcods.home_buddy_v2.model.auth.Role;
import com.azedcods.home_buddy_v2.model.auth.User;
import com.azedcods.home_buddy_v2.repository.auth.RoleRepository;
import com.azedcods.home_buddy_v2.repository.auth.UserRepository;
import com.azedcods.home_buddy_v2.security.jwt.AuthEntryPointJwt;
import com.azedcods.home_buddy_v2.security.jwt.AuthTokenFilter;
import com.azedcods.home_buddy_v2.security.services.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;


import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/signin", "/api/auth/signup").permitAll()
                        .requestMatchers("/api/auth/user", "/api/auth/username").authenticated()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // --- Swagger / OpenAPI ---
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/v2/api-docs",
                                "/webjars/**",
                                "/configuration/ui",
                                "/configuration/security"
                        ).permitAll()

                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers("/images/**").permitAll()

                        // ✅ Dose module: restrict by role at the route level
                        // Users can still access their own via method-level checks (recommended).
                        .requestMatchers("/api/doses/**").hasAnyRole("USER", "CAREGIVER", "ADMIN")
                        .requestMatchers("/api/v2/dose-occurrences/**").hasAnyRole("USER", "CAREGIVER", "ADMIN")

                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
        http.cors(cors -> {});

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedOrigin("https://home-buddy-v2.vercel.app");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public CommandLineRunner initData(RoleRepository roleRepository,
                                      UserRepository userRepository,
                                      PasswordEncoder passwordEncoder,
                                      PlatformTransactionManager txManager) {

        TransactionTemplate tx = new TransactionTemplate(txManager);

        return args -> tx.executeWithoutResult(status -> {
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseGet(() -> roleRepository.save(new Role(AppRole.ROLE_USER)));

            Role caregiverRole = roleRepository.findByRoleName(AppRole.ROLE_CAREGIVER)
                    .orElseGet(() -> roleRepository.save(new Role(AppRole.ROLE_CAREGIVER)));

            Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                    .orElseGet(() -> roleRepository.save(new Role(AppRole.ROLE_ADMIN)));

            // IMPORTANT: use mutable sets (Hibernate-friendly)
            Set<Role> caregiverRoles = new java.util.HashSet<>(java.util.Set.of(caregiverRole));
            Set<Role> adminRoles = new java.util.HashSet<>(java.util.Set.of(userRole, caregiverRole, adminRole));

            if (!userRepository.existsByUserName("caregiver1")) {
                User caregiver1 = new User("caregiver1", "caregiver1@example.com", passwordEncoder.encode("password2"));
                caregiver1.setFullname("Test Caregiver");
                caregiver1.setRoles(caregiverRoles);
                userRepository.save(caregiver1);
            }

            if (!userRepository.existsByUserName("admin")) {
                User admin = new User("admin", "admin@example.com", passwordEncoder.encode("adminPass"));
                admin.setFullname("Admin");
                admin.setRoles(adminRoles);
                userRepository.save(admin);
            }

            userRepository.findByUserName("caregiver1").ifPresent(u -> {
                u.setRoles(caregiverRoles);
                userRepository.save(u);
            });

            userRepository.findByUserName("admin").ifPresent(u -> {
                u.setRoles(adminRoles);
                userRepository.save(u);
            });
        });
    }

}
