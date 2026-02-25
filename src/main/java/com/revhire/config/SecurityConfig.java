package com.revhire.config;
import org.springframework.http.HttpMethod;
import com.revhire.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/api/info","/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")


                        .requestMatchers(HttpMethod.GET, "/api/v1/jobseekers/profile/me").hasRole("JOB_SEEKER")


                        .requestMatchers(HttpMethod.GET, "/api/v1/jobseekers/profile/**").hasAnyRole("EMPLOYER", "ADMIN")


                        .requestMatchers(HttpMethod.GET, "/api/v1/jobseekers/resume/file/**").hasAnyRole("EMPLOYER", "ADMIN")


                        .requestMatchers("/api/v1/jobseekers/**").hasRole("JOB_SEEKER")


                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/jobs/**").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/jobs/**").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/jobs/**").hasRole("EMPLOYER")
                        .requestMatchers("/api/v1/employers/**").hasRole("EMPLOYER")


                        .requestMatchers("/api/v1/applications/**").authenticated()
                        .requestMatchers("/api/v1/favourites/**").authenticated()

                        .anyRequest().authenticated()

                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}