package com.groupName.artefactName.controlador;

import com.groupName.artefactName.exceptions.BusinessLogicException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        webRequest = new ServletWebRequest(request);
    }

    @Test
    void handleResourceNotFoundException_shouldReturnNotFound() {
        ConfigDataResource configDataResourceMock = mock(ConfigDataResource.class); // Crear un mock de ConfigDataResource
        ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(configDataResourceMock);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFoundException(exception, webRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
        assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), response.getBody().getError());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleBusinessLogicException_shouldReturnBadRequest() {
        String errorMessage = "Business logic error";
        BusinessLogicException exception = new BusinessLogicException(errorMessage);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusinessLogicException(exception, webRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), response.getBody().getError());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleMethodArgumentNotValidException_shouldReturnBadRequestWithValidationErrors() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        FieldError fieldError1 = new FieldError("objectName", "field1", "Error message 1");
        FieldError fieldError2 = new FieldError("objectName", "field2", "Error message 2");
        List<FieldError> fieldErrors = List.of(fieldError1, fieldError2);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValidException(exception);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), response.getBody().getError());
        assertEquals("Validation failed for arguments.", response.getBody().getMessage());
        assertEquals("[field1: Error message 1, field2: Error message 2]", response.getBody().getPath()); // Assuming 'path' in ErrorResponse holds the errors string
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleHttpRequestMethodNotSupportedException_shouldReturnConflict() {
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("GET", Collections.emptyList());

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleHttpRequestMethodNotSupportedException(exception, webRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
        assertEquals(HttpStatus.CONFLICT.getReasonPhrase(), response.getBody().getError());
        assertEquals("Method not supported.", response.getBody().getMessage());
        assertEquals("Request method 'GET' is not supported", response.getBody().getPath()); // Assuming 'path' holds the exception message
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleHttpMediaTypeNotSupportedException_shouldReturnConflict() {
        String errorMessage = "Media type not supported";
        HttpMediaTypeNotSupportedException exception = new HttpMediaTypeNotSupportedException(errorMessage, Collections.emptyList());

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMediaTypeNotSupportedException(exception, webRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
        assertEquals(HttpStatus.CONFLICT.getReasonPhrase(), response.getBody().getError());
        assertEquals("Media type not supported.", response.getBody().getMessage());
        assertEquals(errorMessage, response.getBody().getPath()); // Assuming 'path' holds the exception message
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        String errorMessage = "Generic error";
        Exception exception = new RuntimeException(errorMessage);
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(exception, webRequest);
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), response.getBody().getError());
        assertEquals("An unexpected error occurred.", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleDataIntegrityViolationException_shouldReturnConflict() {
        String errorMessage = "Constraint violation occurred";
        DataIntegrityViolationException exception = new DataIntegrityViolationException(errorMessage);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolationException(exception, webRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
        assertEquals(HttpStatus.CONFLICT.getReasonPhrase(), response.getBody().getError());
        assertEquals("Data integrity violation.", response.getBody().getMessage());
        assertEquals(errorMessage, response.getBody().getPath()); // Asumiendo que el mensaje de la excepción se guarda en 'path'
        assertNotNull(response.getBody().getTimestamp());
    }
}
