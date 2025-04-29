package com.groupName.artefactName.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorsConfigTest {

    @Test
    void testAddCorsMappings() {

        String[] origins = {"http://localhost:5173","http://front-server:puerto-front","http://front-server"};
        String[] methods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};

        // 1. Crear una instancia de la clase de configuración
        CorsConfig corsConfig = new CorsConfig();

        // 2. Crear un mock del CorsRegistry
        CorsRegistry corsRegistry = Mockito.mock(CorsRegistry.class);

        // 3. Simular el método addMapping para que devuelva un mock de CorsRegistration
        CorsRegistration corsRegistration = Mockito.mock(CorsRegistration.class);
        when(corsRegistry.addMapping("/**")).thenReturn(corsRegistration);

        when(corsRegistration.allowedOrigins(origins)).thenReturn(corsRegistration);
        when(corsRegistration.allowedMethods(methods)).thenReturn(corsRegistration);
        when(corsRegistration.allowedHeaders(Mockito.anyString())).thenReturn(corsRegistration);
        when(corsRegistration.allowCredentials(Mockito.anyBoolean())).thenReturn(corsRegistration);

        // 4. Ejecutar el método que queremos probar
        corsConfig.addCorsMappings(corsRegistry);

        // 5. Verificar que los métodos de configuración CORS se llamaron correctamente
        verify(corsRegistry).addMapping("/**");
        verify(corsRegistration).allowedOrigins(origins);
        verify(corsRegistration).allowedMethods(methods);
        verify(corsRegistration).allowedHeaders("*");
        verify(corsRegistration).allowCredentials(true);
        verify(corsRegistration).maxAge(3600);
    }
}
