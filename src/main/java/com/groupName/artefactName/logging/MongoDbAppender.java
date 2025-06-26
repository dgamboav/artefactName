package com.groupName.artefactName.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Logback Appender personalizado para persistir eventos de log en MongoDB.
 * Requiere la dependencia de 'mongodb-driver-sync' (normalmente ya incluida con spring-boot-starter-data-mongodb).
 *
 * Configuración en logback-spring.xml:
 * <appender name="MONGO_SYNC" class="com.yourcompany.yourapp.logging.MongoDbAppender">
 * <uri>mongodb://localhost:27017/your_database_name</uri>
 * <collectionName>application_logs</collectionName>
 * </appender>
 *
 * Para uso asíncrono, envolver con AsyncAppender:
 * <appender name="MONGO" class="ch.qos.logback.classic.AsyncAppender">
 * <appender-ref ref="MONGO_SYNC" />
 * </appender>
 */
public class MongoDbAppender extends AppenderBase<ILoggingEvent> {

    private String uri; // URI de conexión a MongoDB
    private String collectionName; // Nombre de la colección donde se guardarán los logs
    private MongoClient mongoClient; // transient para evitar serialización
    private MongoCollection<Document> logsCollection; // transient para evitar serialización

    /**
     * Setter para la URI de conexión a MongoDB. Logback lo llamará desde la configuración XML.
     * @param uri La URI de conexión.
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Setter para el nombre de la colección de logs. Logback lo llamará desde la configuración XML.
     * @param collectionName El nombre de la colección.
     */
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * Método invocado por Logback cuando el Appender es inicializado.
     * Aquí se establece la conexión a MongoDB.
     */
    @Override
    public void start() {
        if (uri == null || uri.trim().isEmpty()) {
            addError("MongoDB URI is not set for MongoDbAppender.");
            return;
        }
        if (collectionName == null || collectionName.trim().isEmpty()) {
            addError("Collection name is not set for MongoDbAppender.");
            return;
        }

        try {
            ConnectionString connectionString = new ConnectionString(uri);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .build();
            mongoClient = MongoClients.create(settings);

            // Extrae el nombre de la base de datos de la URI
            String databaseName = connectionString.getDatabase();
            if (databaseName == null || databaseName.trim().isEmpty()) {
                addError("MongoDB URI does not specify a database name.");
                return;
            }

            MongoDatabase database = mongoClient.getDatabase(databaseName);
            logsCollection = database.getCollection(collectionName);

            super.start(); // Llama al método start de la clase base
            addInfo("MongoDbAppender started successfully. Logging to database '" + databaseName + "', collection '" + collectionName + "'.");

        } catch (Exception e) {
            addError("Failed to start MongoDbAppender: " + e.getMessage(), e);
            // Asegurarse de que el appender no se marque como "started" si falla la inicialización
            stop();
        }
    }

    /**
     * Método invocado por Logback cuando la aplicación se detiene o el Appender es detenido.
     * Aquí se cierra la conexión a MongoDB.
     */
    @Override
    public void stop() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                addInfo("MongoDbAppender stopped and MongoDB client closed.");
            } catch (Exception e) {
                addError("Error closing MongoDB client for MongoDbAppender: " + e.getMessage(), e);
            } finally {
                mongoClient = null;
                logsCollection = null;
            }
        }
        super.stop(); // Llama al método stop de la clase base
    }

    /**
     * Método principal donde se procesa cada evento de log.
     * Se convierte el evento de Logback en un documento de MongoDB y se inserta.
     * @param eventObject El evento de logging a procesar.
     */
    @Override
    protected void append(ILoggingEvent eventObject) {
        // Asegúrate de que el appender esté iniciado y la colección sea accesible
        if (!isStarted() || logsCollection == null) {
            return;
        }

        try {
            Document logDoc = new Document();
            // Marca de tiempo del evento (en milisegundos desde la época Unix)
            logDoc.append("timestamp", eventObject.getTimeStamp());
            // Convierte a LocalDateTime para mayor legibilidad si se prefiere
            logDoc.append("datetime", LocalDateTime.ofInstant(Instant.ofEpochMilli(eventObject.getTimeStamp()), ZoneOffset.UTC));
            logDoc.append("level", eventObject.getLevel().toString());
            logDoc.append("thread", eventObject.getThreadName());
            logDoc.append("logger", eventObject.getLoggerName());
            logDoc.append("message", eventObject.getFormattedMessage());
            logDoc.append("application", "YourApplicationName"); // Puedes externalizar esto si quieres

            // Añadir información del Throwable si existe
            IThrowableProxy throwableProxy = eventObject.getThrowableProxy();
            if (throwableProxy != null) {
                Document exceptionDoc = new Document();
                exceptionDoc.append("class", throwableProxy.getClassName());
                exceptionDoc.append("message", throwableProxy.getMessage());
                // Obtener el stack trace completo
                String stackTrace = Stream.of(throwableProxy.getStackTraceElementProxyArray())
                        .map(StackTraceElementProxy::getSTEAsString)
                        .collect(Collectors.joining("\n"));
                exceptionDoc.append("stackTrace", stackTrace);

                // Si hay una causa raíz (chained exception)
                if (throwableProxy.getCause() != null) {
                    exceptionDoc.append("cause", throwableProxy.getCause().getMessage());
                }
                logDoc.append("exception", exceptionDoc);
            }

            // Añadir datos del MDC (Mapped Diagnostic Context) si existen
            if (eventObject.getMDCPropertyMap() != null && !eventObject.getMDCPropertyMap().isEmpty()) {
                logDoc.append("mdc", new Document(eventObject.getMDCPropertyMap()));
            }

            // Insertar el documento en la colección de MongoDB
            logsCollection.insertOne(logDoc);

        } catch (Exception e) {
            // Este catch es crítico para evitar que los fallos de logging bloqueen la aplicación.
            // Los errores aquí no deben lanzarse, solo deben registrarse de alguna forma alternativa
            // (ej. a la consola o a un archivo de emergencia) o ignorarse para no afectar la app.
            addError("Failed to log event to MongoDB: " + e.getMessage(), e);
        }
    }
}
