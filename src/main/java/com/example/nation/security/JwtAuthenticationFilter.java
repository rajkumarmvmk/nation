package com.example.nation.security;



import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
@Slf4j
@Component
//@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  @Autowired
  private   JwtService jwtService;

  @Autowired
  private  UserDetailsService userDetailsService;


  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    if (request.getServletPath().contains("/api/v1/auth/authenticate")) {
      filterChain.doFilter(request, response);
      return;
    }
    if (request.getServletPath().contains("/api/v1/auth/refresh-token")) {
      filterChain.doFilter(request, response);
      return;
    }
    if(request.getServletPath().contains("/swagger-ui/")) {
      filterChain.doFilter(request, response);
      return;
    }
      if(request.getServletPath().contains("/v3/api-docs")){
        filterChain.doFilter(request,response);
        return;
    }
    if(request.getServletPath().contains("/actuator/")){
      filterChain.doFilter(request,response);
      return;
    }
    final String authHeader = request.getHeader("Authorization");
    final String jwt;
    final String userEmail;
    if (authHeader == null ||!authHeader.startsWith("Bearer ")) {

      response.setContentType(APPLICATION_JSON_VALUE);
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

      final Map<String, Object> body = new HashMap<>();
      body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
      body.put("error", "Unauthorized");
      body.put("message", "ACCESS DENIED");
      body.put("path", request.getServletPath());
    new ObjectMapper().writeValue(response.getOutputStream(),body);
      return;
    }
    jwt = authHeader.substring(7);
    userEmail = jwtService.extractUsername(jwt);
    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

      UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

      if (jwtService.isTokenValid(jwt, userDetails)) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
        );
        authToken.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request)
        );
        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request, response);
        return;
      }else{
        response.setContentType(APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", "token expired");
        body.put("path", request.getServletPath());
        new ObjectMapper().writeValue(response.getOutputStream(),body);
        return;

      }

    }
     filterChain.doFilter(request, response);
  }
}
