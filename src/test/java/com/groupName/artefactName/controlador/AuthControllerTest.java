package com.groupName.artefactName.controlador;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Test
    void testLoginSuccess() {
        // Simular las credenciales correctas
        Map<String, String> credentials = Map.of("username", "unity", "password", "unity");

        // Ejecutar el método de login
        ResponseEntity<?> response = authController.login(credentials);

        // Verificar la respuesta
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("prototipo-token-valido", body.get("token"));
    }

    @Test
    void testLoginFailureWrongUsername() {
        // Simular credenciales con usuario incorrecto
        Map<String, String> credentials = Map.of("username", "otro", "password", "unity");

        // Ejecutar el método de login
        ResponseEntity<?> response = authController.login(credentials);

        // Verificar la respuesta
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Credenciales inválidas", body.get("message"));
    }

    @Test
    void testLoginFailureWrongPassword() {
        // Simular credenciales con contraseña incorrecta
        Map<String, String> credentials = Map.of("username", "unity", "password", "otra");

        // Ejecutar el método de login
        ResponseEntity<?> response = authController.login(credentials);

        // Verificar la respuesta
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Credenciales inválidas", body.get("message"));
    }

    @Test
    void testLoginFailureMissingCredentials() {
        // Simular credenciales incompletas (solo usuario)
        Map<String, String> credentialsUsernameOnly = Map.of("username", "unity");
        ResponseEntity<?> responseUsernameOnly = authController.login(credentialsUsernameOnly);
        assertEquals(HttpStatus.UNAUTHORIZED, responseUsernameOnly.getStatusCode());
        assertNotNull(responseUsernameOnly.getBody());
        Map<String, String> bodyUsernameOnly = (Map<String, String>) responseUsernameOnly.getBody();
        assertEquals("Credenciales inválidas", bodyUsernameOnly.get("message"));

        // Simular credenciales incompletas (solo contraseña)
        Map<String, String> credentialsPasswordOnly = Map.of("password", "unity");
        ResponseEntity<?> responsePasswordOnly = authController.login(credentialsPasswordOnly);
        assertEquals(HttpStatus.UNAUTHORIZED, responsePasswordOnly.getStatusCode());
        assertNotNull(responsePasswordOnly.getBody());
        Map<String, String> bodyPasswordOnly = (Map<String, String>) responsePasswordOnly.getBody();
        assertEquals("Credenciales inválidas", bodyPasswordOnly.get("message"));

        // Simular credenciales vacías
        Map<String, String> emptyCredentials = Map.of();
        ResponseEntity<?> emptyResponse = authController.login(emptyCredentials);
        assertEquals(HttpStatus.UNAUTHORIZED, emptyResponse.getStatusCode());
        assertNotNull(emptyResponse.getBody());
        Map<String, String> emptyBody = (Map<String, String>) emptyResponse.getBody();
        assertEquals("Credenciales inválidas", emptyBody.get("message"));
    }
}
