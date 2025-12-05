package com.example.website.config;

import com.example.website.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                // 1. PUBLIC ACCESS (Unrestricted)
                .requestMatchers(
                    "/", // Root URL
                    "/dashboard",
                    "/about",
                    "/view/**",     // Detail page views
                    "/register",
                    "/css/**",
                    "/sitemap.xml", 
                    "/robots.txt",
                    "/js/**",
                    "/uploads/**",
                    "/images/**"    // Static/Public files
                ).permitAll()
                
                // 2. AUTHENTICATED ACCESS (Login Required for Interaction)
                // Liking and Commenting now require a logged-in user.
                .requestMatchers(
                    "/like/**",     
                    "/comment/**"  
                ).permitAll() // 💡 CRITICAL: Only authenticated users can like/comment
                
                // 3. ADMIN ACCESS (Only ADMIN role can access)
                .requestMatchers(
                    "/admin/dashboard", // 💡 ADDED: Admin dashboard is admin-only
                    "/admin/upload",
                    "/admin/delete/**" 
                ).hasRole("ADMIN")

                // 4. FALLBACK: Any other request (unlisted or internal APIs) requires authentication
                .anyRequest().authenticated()
            )

     
            // Custom Login Page
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )

            // Logout Configuration
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // Disable CSRF for simpler development. Reconfigure for production!
             .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}