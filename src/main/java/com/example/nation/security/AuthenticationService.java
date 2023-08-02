package com.example.nation.security;



import com.example.nation.exception.customException.BaseErrorCodes;
import com.example.nation.exception.customException.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

//@RequiredArgsConstructor
//@Service
@Slf4j
@Component
public class AuthenticationService {
  @Autowired
  private  UserRepository repository;
@Autowired
 private  BCryptPasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;
@Autowired
  private  AuthenticationManager authenticationManager;



  public AuthenticationResponse register(RegisterRequest request) throws BusinessException  {
   Optional<User> userRecord =repository.getByEmail(request.getEmail());
    if(userRecord.isEmpty() || !request.getEmail().equals(userRecord.get().getEmail())){
      log.info(String.valueOf(repository.findByEmail(request.getEmail())));
      var user = User.builder()
              .firstname(request.getFirstname())
              .lastname(request.getLastname())
              .email(request.getEmail())
              .password(passwordEncoder.encode(request.getPassword()))
              .role(request.getRole())
              .build();
       repository.save(user);
      var jwtToken = jwtService.generateToken(user);
      var refreshToken = jwtService.generateRefreshToken(user);
      return AuthenticationResponse.builder()
              .accessToken(jwtToken)
              .refreshToken(refreshToken)
              .build();
    }else{
      throw new BusinessException(BaseErrorCodes.DUPLICATE_RECORD.name(),BaseErrorCodes.DUPLICATE_RECORD.message());
    }
  }

  public AuthenticationResponse authenticate(AuthenticationRequest request) throws BusinessException {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getEmail(),
            request.getPassword()
        )
    );
    var user = repository.findByEmail(request.getEmail())
        .orElseThrow(()->new BusinessException(BaseErrorCodes.RECORD_NOT_FOUND.name(),BaseErrorCodes.RECORD_NOT_FOUND.message()));
    var jwtToken = jwtService.generateToken(user);
    var refreshToken = jwtService.generateRefreshToken(user);
    return AuthenticationResponse.builder()
        .accessToken(jwtToken)
            .refreshToken(refreshToken)
        .build();
  }



  public void refreshToken(
          HttpServletRequest request,
          HttpServletResponse response
  ) throws IOException, BusinessException {
    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    final String refreshToken;
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
    refreshToken = authHeader.substring(7);
    userEmail = jwtService.extractUsername(refreshToken);
    if (userEmail != null) {
      var user = this.repository.findByEmail(userEmail)
              .orElseThrow(()->new BusinessException(BaseErrorCodes.RECORD_NOT_FOUND.name(),BaseErrorCodes.RECORD_NOT_FOUND.message()));
      if (jwtService.isTokenValid(refreshToken, user)) {
        var accessToken = jwtService.generateToken(user);
        var authResponse = AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
      }else{
        response.setContentType(APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", "token expired");
        body.put("path", request.getServletPath());
        new ObjectMapper().writeValue(response.getOutputStream(),body);
      }
    }else{
      response.setContentType(APPLICATION_JSON_VALUE);
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

      final Map<String, Object> body = new HashMap<>();
      body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
      body.put("error", "Unauthorized");
      body.put("message", "invalid Token");
      body.put("path", request.getServletPath());
      new ObjectMapper().writeValue(response.getOutputStream(),body);

    }
  }
}
