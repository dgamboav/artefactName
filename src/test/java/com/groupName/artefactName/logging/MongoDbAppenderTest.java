package com.groupName.artefactName.logging; // Ajusta el paquete según tu proyecto

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.status.StatusManager;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MongoDbAppender Unit Tests")
class MongoDbAppenderTest {

    private MongoDbAppender appender;

    @Mock
    private MongoClient mockMongoClient;
    @Mock
    private MongoDatabase mockMongoDatabase;
    @Mock
    private MongoCollection<Document> mockMongoCollection;
    @Mock
    private LoggerContext mockLoggerContext;
    @Mock // <-- ¡Nuevo mock para StatusManager!
    private StatusManager mockStatusManager;

    // Mock estático para MongoClients.create()
    private MockedStatic<MongoClients> mockMongoClientsStatic;

    @BeforeEach
    void setUp() {
        appender = new MongoDbAppender();
        appender.setContext(mockLoggerContext);

        // Configurar el LoggerContext para devolver nuestro mockStatusManager
        when(mockLoggerContext.getStatusManager()).thenReturn(mockStatusManager); // <-- ¡Línea clave!

        // Mockear el comportamiento estático de MongoClients.create()
        mockMongoClientsStatic = mockStatic(MongoClients.class);
        mockMongoClientsStatic.when(() -> MongoClients.create(any(MongoClientSettings.class)))
                .thenReturn(mockMongoClient);

        lenient().when(mockMongoClient.getDatabase(anyString())).thenReturn(mockMongoDatabase);
        lenient().when(mockMongoDatabase.getCollection(anyString())).thenReturn(mockMongoCollection);
    }

    @AfterEach
    void tearDown() {
        if (mockMongoClientsStatic != null) {
            mockMongoClientsStatic.close();
        }
    }

    @Test
    @DisplayName("should start successfully with valid URI and collection name")
    void shouldStartSuccessfullyWithValidUriAndCollectionName() {
        appender.setUri("mongodb://localhost:27017/testdb");
        appender.setCollectionName("test_collection");
        appender.setApplicationName("TestApp");
        appender.start();
        assertTrue(appender.isStarted());
        mockMongoClientsStatic.verify(() -> MongoClients.create(any(MongoClientSettings.class)));
        verify(mockMongoClient).getDatabase("testdb");
        verify(mockMongoDatabase).getCollection("test_collection");
    }

    @Test
    @DisplayName("should not start if URI is null or empty")
    void shouldNotStartIfUriIsNullOrEmpty() {
        appender.setUri(null);
        appender.setCollectionName("test_collection");
        appender.start();
        assertFalse(appender.isStarted());
        verifyNoInteractions(mockMongoClient);
        appender = new MongoDbAppender();
        appender.setContext(mockLoggerContext); // Necesario para resetear el contexto
        when(mockLoggerContext.getStatusManager()).thenReturn(mockStatusManager); // Reconfigurar el mockStatusManager
        appender.setUri("");
        appender.setCollectionName("test_collection");
        appender.start();
        assertFalse(appender.isStarted());
        verifyNoInteractions(mockMongoClient);
        // Ya se verificó la adición del error en la primera parte de esta prueba
    }

    @Test
    @DisplayName("should not start if collection name is null or empty")
    void shouldNotStartIfCollectionNameIsNullOrEmpty() {
        appender.setUri("mongodb://localhost:27017/testdb");
        appender.setCollectionName(null);
        appender.start();
        assertFalse(appender.isStarted());
        verifyNoInteractions(mockMongoClient);
    }

    @Test
    @DisplayName("should stop and close MongoDB client")
    void shouldStopAndCloseMongoClient() {
        appender.setUri("mongodb://localhost:27017/testdb");
        appender.setCollectionName("test_collection");
        appender.start();
        assertTrue(appender.isStarted());
        appender.stop();
        assertFalse(appender.isStarted());
        verify(mockMongoClient).close();
    }

    @Test
    @DisplayName("should append INFO log event to MongoDB")
    void shouldAppendInfoLogEventToMongoDB() {
        appender.setUri("mongodb://localhost:27017/testdb");
        appender.setCollectionName("test_collection");
        appender.setApplicationName("MyTestApp");
        appender.start();

        ILoggingEvent mockEvent = mock(ILoggingEvent.class);
        when(mockEvent.getTimeStamp()).thenReturn(System.currentTimeMillis());
        when(mockEvent.getLevel()).thenReturn(Level.INFO);
        when(mockEvent.getThreadName()).thenReturn("test-thread");
        when(mockEvent.getLoggerName()).thenReturn("com.groupName.test");
        when(mockEvent.getFormattedMessage()).thenReturn("Test INFO message.");
        when(mockEvent.getMDCPropertyMap()).thenReturn(Collections.emptyMap());
        when(mockEvent.getThrowableProxy()).thenReturn(null);

        appender.doAppend(mockEvent);

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(mockMongoCollection).insertOne(documentCaptor.capture());

        Document capturedDoc = documentCaptor.getValue();
        assertNotNull(capturedDoc);
        assertEquals(Level.INFO.toString(), capturedDoc.getString("level"));
        assertEquals("test-thread", capturedDoc.getString("thread"));
        assertEquals("com.groupName.test", capturedDoc.getString("logger"));
        assertEquals("Test INFO message.", capturedDoc.getString("message"));
        assertEquals("MyTestApp", capturedDoc.getString("application"));
        assertNotNull(capturedDoc.getLong("timestamp"));
        assertNotNull(capturedDoc.get("datetime"));
        assertFalse(capturedDoc.containsKey("exception"));
        assertFalse(capturedDoc.containsKey("mdc"));
    }

    @Test
    @DisplayName("should append ERROR log event with Throwable to MongoDB")
    void shouldAppendErrorLogEventWithThrowableToMongoDB() {
        appender.setUri("mongodb://localhost:27017/testdb");
        appender.setCollectionName("test_collection");
        appender.setApplicationName("ErrorApp");
        appender.start();

        LoggingEvent loggingEvent = new LoggingEvent();
        loggingEvent.setTimeStamp(System.currentTimeMillis());
        loggingEvent.setLevel(Level.ERROR);
        loggingEvent.setThreadName("error-thread");
        loggingEvent.setLoggerName("com.civislend.error");
        loggingEvent.setMessage("Error occurred!");
        loggingEvent.setMDCPropertyMap(Collections.emptyMap());

        RuntimeException testException = new RuntimeException("Test Exception Message", new IllegalArgumentException("Caused by test"));
        loggingEvent.setThrowableProxy(new ThrowableProxy(testException));

        loggingEvent.setLoggerContext(mockLoggerContext); // Importante para que el stack trace se genere

        appender.doAppend(loggingEvent);

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(mockMongoCollection).insertOne(documentCaptor.capture());

        Document capturedDoc = documentCaptor.getValue();
        assertNotNull(capturedDoc);
        assertEquals(Level.ERROR.toString(), capturedDoc.getString("level"));
        assertEquals("Error occurred!", capturedDoc.getString("message"));
        assertEquals("ErrorApp", capturedDoc.getString("application"));

        Document exceptionDoc = (Document) capturedDoc.get("exception");
        assertNotNull(exceptionDoc);
        assertEquals(RuntimeException.class.getName(), exceptionDoc.getString("class"));
        assertEquals("Test Exception Message", exceptionDoc.getString("message"));
        assertEquals("Caused by test", exceptionDoc.getString("cause"));
    }

    @Test
    @DisplayName("should append log event with MDC properties to MongoDB")
    void shouldAppendLogEventWithMDCPropertiesToMongoDB() {
        appender.setUri("mongodb://localhost:27017/testdb");
        appender.setCollectionName("test_collection");
        appender.setApplicationName("MDCApp");
        appender.start();

        ILoggingEvent mockEvent = mock(ILoggingEvent.class);
        when(mockEvent.getTimeStamp()).thenReturn(System.currentTimeMillis());
        when(mockEvent.getLevel()).thenReturn(Level.DEBUG);
        when(mockEvent.getThreadName()).thenReturn("mdc-thread");
        when(mockEvent.getLoggerName()).thenReturn("com.civislend.mdc");
        when(mockEvent.getFormattedMessage()).thenReturn("Message with MDC.");

        Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("userId", "123");
        mdcMap.put("transactionId", "abc-456");
        when(mockEvent.getMDCPropertyMap()).thenReturn(mdcMap);
        when(mockEvent.getThrowableProxy()).thenReturn(null);

        appender.doAppend(mockEvent);

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(mockMongoCollection).insertOne(documentCaptor.capture());

        Document capturedDoc = documentCaptor.getValue();
        assertNotNull(capturedDoc);
        assertEquals("Message with MDC.", capturedDoc.getString("message"));

        Document mdcDoc = (Document) capturedDoc.get("mdc");
        assertNotNull(mdcDoc); // Esto debería ser mdcDoc
        assertEquals("123", mdcDoc.getString("userId"));
        assertEquals("abc-456", mdcDoc.getString("transactionId"));
    }

    @Test
    @DisplayName("should handle MongoDB insertion exception gracefully")
    void shouldHandleMongoDBInsertionExceptionGracefully() {
        appender.setUri("mongodb://localhost:27017/testdb");
        appender.setCollectionName("test_collection");
        appender.setApplicationName("ErrorHandlingApp");
        appender.start();

        ILoggingEvent mockEvent = mock(ILoggingEvent.class);
        when(mockEvent.getTimeStamp()).thenReturn(System.currentTimeMillis());
        when(mockEvent.getLevel()).thenReturn(Level.INFO);
        when(mockEvent.getThreadName()).thenReturn("faulty-thread");
        when(mockEvent.getLoggerName()).thenReturn("com.civislend.fault");
        when(mockEvent.getFormattedMessage()).thenReturn("This log should fail.");
        when(mockEvent.getMDCPropertyMap()).thenReturn(Collections.emptyMap());
        when(mockEvent.getThrowableProxy()).thenReturn(null);

        doThrow(new RuntimeException("MongoDB connection lost during insert")).when(mockMongoCollection).insertOne(any(Document.class));

        appender.doAppend(mockEvent);
        verify(mockMongoCollection).insertOne(any(Document.class));

    }

    @Test
    @DisplayName("should not append if appender is not started")
    void shouldNotAppendIfAppenderIsNotStarted() {
        ILoggingEvent mockEvent = mock(ILoggingEvent.class);
        appender.doAppend(mockEvent);
        verifyNoInteractions(mockMongoCollection);
    }

    @Test
    @DisplayName("should not append if logsCollection is null (e.g., after failed start)")
    void shouldNotAppendIfLogsCollectionIsNull() {
        appender.setUri("invalid_uri");
        appender.setCollectionName("test_collection");
        appender.setApplicationName("TestApp");
        appender.start();

        assertFalse(appender.isStarted());
        ILoggingEvent mockEvent = mock(ILoggingEvent.class);

        appender.doAppend(mockEvent);

        verifyNoInteractions(mockMongoCollection);
    }
}