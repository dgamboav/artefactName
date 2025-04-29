pipeline {
    agent any
    environment {
        ORGANIZATION_NAME = '$_ORGANIZATION_NAME_$'
        PROJECT_NAME = '$_PROJECT_NAME_$'
        GITHUB_REPO_URL = "https://github.com/${env.ORGANIZATION_NAME}/${env.PROJECT_NAME}.git"
        SONAR_SERVER_NAME = 'idea-sonarqube-instance' // Usar el nombre configurado en Jenkins
        SONAR_PROJECT_KEY = "${env.ORGANIZATION_NAME}:${env.PROJECT_NAME}" // Ejemplo de clave
        APP_IP = '$_APP_SERVER_IP_$'
        PHRASE = '$_APP_SERVER_PHRASE_$'
        REMOTE_PORT = '$_APP_SERVER_PORT_$'
        APP_URL = "http://${env.APP_IP}" // URL base de tu aplicación
        E2E_TESTS_DIR = 'src/test/resources/end-to-end' // Ruta relativa dentro del workspace
    }
    stages {
        stage('Checkout') {
            steps {
                git url: env.GITHUB_REPO_URL, branch: 'main' // o tu rama por defecto
            }
        }
		stage('Build') {
			steps {
				sh 'mvn clean package'
			}
			post {
				always {
					recordCoverage(
						tools: [[parser: 'JACOCO', reportPattern: 'target/site/jacoco/jacoco.xml']],
						id: 'jacoco',
						name: 'JaCoCo Coverage',
						sourceCodeRetention: 'EVERY_BUILD',
						qualityGates: [
							[threshold: 60.0, metric: 'LINE', baseline: 'PROJECT', unstable: true],
							[threshold: 60.0, metric: 'BRANCH', baseline: 'PROJECT', unstable: true]
						]
					)
				}
			}
		}
        stage('SonarQube analysis') {
            steps {
                script {
                    withSonarQubeEnv(credentialsId: 'sonarqube-token-test', installationName: 'idea-sonarqube-instance') {
                        sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.7.0.1746:sonar'
                    }
                }
            }
            post {
                always {
                    sleep(time: 20, unit: 'SECONDS')
                    timeout(time: 2, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
        }
		stage('Deploy') {
			steps {
				script {
					def remoteHost = "${env.APP_IP}"
					def remoteUser = '$_APP_SERVER_USER_$'
					def remoteDir = "/home/${remoteUser}/app" // Directorio donde desplegar en la instancia remota
					def appNameBase = "${env.PROJECT_NAME}"
					def appName = "${env.PROJECT_NAME}-${env.BUILD_ID}.jar" // Nombre del archivo JAR con el ID del build
					def workspaceDir = pwd() // Directorio del workspace de Jenkins
					def credId = 'pruebas-ssh-without-encrypt' // ID de tus credenciales SSH de Jenkins

                    withCredentials([sshUserPrivateKey(credentialsId: credId, keyFileVariable: 'identity', passphraseVariable: '', usernameVariable: remoteUser)]) {
					
					def remote = [
						name: remoteHost,
						host: remoteHost,
						user: remoteUser,
						identityFile: identity,
						allowAnyHosts: true // Considera configurar knownHosts para mayor seguridad en producción
					]
					
					// 1. Detener todas las aplicaciones con el nombre base
					echo "Deteniendo aplicaciones existentes con nombre base: ${appNameBase}"
					sshCommand remote: remote, command: "pkill -f '${appNameBase}'", failOnError: false

			    	echo "Validando disponibilidad del puerto ${env.REMOTE_PORT}..."

                    def checkPortCommand = "netstat -tuln | grep :${env.REMOTE_PORT}"
                    def result = sshCommand remote: remote, command: checkPortCommand, failOnError: false
                    def portInUse = false // Inicializamos a false

                    if (result != '') {
                        echo "Resultado del comando de validacion de puerto ${result}..."
                        portInUse = (result.exitStatus == 0)
                    } else {
                        echo "Advertencia: No se pudo obtener el código de salida del comando de verificación de puerto."
                        echo "Asumiendo que el puerto ${env.REMOTE_PORT} está libre."
                    }

                    if (portInUse) {
                        echo "El puerto ${env.REMOTE_PORT} ya está en uso. Intentando detener el proceso existente..."
                        def processIdCommand = "lsof -i tcp:${env.REMOTE_PORT} -t"
                        def processId = sshCommand(remote: remote, command: processIdCommand, returnStdout: true).trim()

                        if (processId) {
                            echo "Proceso con PID ${processId} encontrado. Intentando detenerlo..."
                            sshCommand remote: remote, command: "sudo kill -9 ${processId}", failOnError: false
                            sleep time: 5, unit: 'SECONDS'
                            // Vuelve a verificar si el puerto está libre (opcional)
                            portInUse = sshCommand(remote: remote, command: checkPortCommand, returnStatus: true).trim()
                            if (portInUse == 0) {
                                echo "El puerto ${env.REMOTE_PORT} ahora está libre."
                            } else {
                                error "No se pudo liberar el puerto ${env.REMOTE_PORT}. El despliegue del backend fallará."
                            }
                        } else {
                            echo "No se encontró ningún proceso usando el puerto ${env.REMOTE_PORT} (esto es inesperado)."
                        }
                    } else {
                        echo "El puerto ${env.REMOTE_PORT} está libre. Procediendo con el despliegue del backend."
                    }

					// 2. Esperar unos segundos para que se detengan las aplicaciones (opcional)
					sleep time: 5, unit: 'SECONDS'

					// 3. Borrar los archivos JAR antiguos (opcional, si deseas limpiar)
					echo "Borrando archivos JAR antiguos con nombre base: ${appNameBase}*.jar"
					sshCommand remote: remote, command: "rm -f ${remoteDir}/${appNameBase}*.jar", failOnError: false

                    // 4. Crear el directorio de despliegue
                        echo "Creando el directorio de despliegue: ${remoteDir}"
                        sshCommand remote: remote, command: "mkdir -p ${remoteDir}", failOnError: true

					// 5. Copiar el nuevo archivo JAR
					sshPut remote: remote, from: "${workspaceDir}/target/${appName}", into: remoteDir, failOnError: true

					// 6. Ejecutar la nueva aplicación en segundo plano
					sshCommand remote: remote, command: "nohup java -jar ${remoteDir}/${appName} > ${remoteDir}/application.log 2>&1 &", failOnError: true

					// 7. Esperar unos segundos para que se levante la aplicacion (opcional)
					sleep time: 20, unit: 'SECONDS'

					echo "Aplicación ${appName} desplegada."
					
                    }
				}
			}
		}
		stage('E2E Tests') {
            steps {
                script {
                    def workspaceDir = pwd()
                    def e2eDir = "${workspaceDir}/${env.E2E_TESTS_DIR}"
                    def appBaseUrl = env.APP_URL
                    def createdEntityId = null // Variable para almacenar el ID de la entidad creada

                    // Definir el orden de las operaciones y el nombre del archivo JSON asociado (si aplica)
                    def operationConfig = [
                        create:   'crear.json',
                        update:   'actualizar.json',
                        'get-all': null,         // No requiere archivo JSON para la petición
                        'get-one': null,         // No requiere archivo JSON para la petición inicial
                        delete:   'eliminar.json'
                    ]

                    // Listar las carpetas de entidades
                    def entityFolders = sh(script: "ls -d ${e2eDir}/*", returnStdout: true).trim().split('\n')

                    entityFolders.each { entityFolder ->
                        def entityName = entityFolder.split('/').last()
                        def endpointBase = "${appBaseUrl}:${env.REMOTE_PORT}/${entityName}/api"

                        operationConfig.each { operation, filename ->
                            def jsonFile = filename ? "${entityFolder}/${filename}" : null
                            def requestBody = jsonFile ? readFile(file: jsonFile).trim() : null
                            def httpMethod = null
                            def currentEndpoint = endpointBase

                            try {
                                def response = null
                                def responseCode = null
                                def responseBody = null

                            if (operation == 'create' && jsonFile) {
                                httpMethod = 'POST'
                                def curlCommand = "curl -X POST -H 'Content-Type: application/json' -d '${requestBody}' '${currentEndpoint}'"
                            
                                // Obtener el cuerpo de la respuesta
                                def responseBodyString = sh(script: "${curlCommand}", returnStdout: true).trim()
                            
                                echo "Cuerpo de la respuesta (string): ${responseBodyString}"
                            
                                responseBody = readJSON text: responseBodyString
                            
                                echo "E2E Test '${entityName} - ${operation}' (${httpMethod}) ejecutado. Respuesta: ${responseBody}"
                            
                                if (responseBody?.id) {
                                    echo "Test '${entityName} - ${operation}' (${httpMethod}) PASSED. ID creado: ${responseBody.id}"
                                    createdEntityId = responseBody.id // Guardar el ID creado
                                } else {
                                    error "Test '${entityName} - ${operation}' (${httpMethod}) FAILED: No 'id' encontrado en la respuesta: ${responseBody}"
                                }
                            }else if (operation == 'update' && jsonFile && createdEntityId) {
                                    httpMethod = 'PUT'
                                    currentEndpoint = "${endpointBase}/${createdEntityId}"
                                    response = sh(script: "curl -X PUT -H 'Content-Type: application/json' -d '${requestBody}' '${currentEndpoint}' -w '%{http_code}' -o /dev/null", returnStdout: true).trim()
                                    responseCode = response.split('\n')[-1]

                                    echo "E2E Test '${entityName} - ${operation}' (${httpMethod}) ejecutado. Código: ${responseCode}"

                                    if (responseCode == '204' || responseCode == '200') { // Ajusta el código de éxito para UPDATE
                                        echo "Test '${entityName} - ${operation}' (${httpMethod}) PASSED."
                                    } else {
                                        error "Test '${entityName} - ${operation}' (${httpMethod}) FAILED: Código ${responseCode}"
                                    }
                                } else if (operation == 'get-all') {
                                    httpMethod = 'GET'
                                    response = sh(script: "curl -X GET -H 'Content-Type: application/json' '${currentEndpoint}' -w '%{http_code}' -o /dev/null", returnStdout: true).trim()
                                    responseCode = response.split('\n')[-1]

                                    echo "E2E Test '${entityName} - ${operation}' (${httpMethod}) ejecutado. Código: ${responseCode}"
                                    if (responseCode == '200') {
                                        echo "Test '${entityName} - ${operation}' (${httpMethod}) PASSED. Lista recibida."
                                    } else {
                                        error "Test '${entityName} - ${operation}' (${httpMethod}) FAILED: Código ${responseCode}"
                                    }
                                } else if (operation == 'get-one' && createdEntityId) {
                                    httpMethod = 'GET'
                                    currentEndpoint = "${endpointBase}/${createdEntityId}"
                                    def curlCommand = "curl -X GET -H 'Content-Type: application/json' -d '${requestBody}' '${currentEndpoint}'"
                            
                                    // Obtener el cuerpo de la respuesta
                                    def responseBodyString = sh(script: "${curlCommand}", returnStdout: true).trim()
                                
                                    echo "Cuerpo de la respuesta (string): ${responseBodyString}"
                                
                                    responseBody = readJSON text: responseBodyString
                                
                                    echo "E2E Test '${entityName} - ${operation}' (${httpMethod}) ejecutado. Respuesta: ${responseBody}"
                                
                                    if (responseBody?.id == createdEntityId) {
                                        echo "Test '${entityName} - ${operation}' (${httpMethod}) PASSED. ID consultado: ${responseBody.id}"
                                    } else {
                                        error "Test '${entityName} - ${operation}' (${httpMethod}) FAILED: No 'id' encontrado en la respuesta: ${responseBody}"
                                    }
                                } else if (operation == 'delete' && createdEntityId) {
                                    httpMethod = 'DELETE'
                                    currentEndpoint = "${endpointBase}/${createdEntityId}"
                                    response = sh(script: "curl -X DELETE -H 'Content-Type: application/json' '${currentEndpoint}' -w '%{http_code}' -o /dev/null", returnStdout: true).trim()
                                    responseCode = response.split('\n')[-1]

                                    echo "E2E Test '${entityName} - ${operation}' (${httpMethod}) ejecutado. Código: ${responseCode}"

                                    if (responseCode == '204') {
                                        echo "Test '${entityName} - ${operation}' (${httpMethod}) PASSED."
                                    } else {
                                        error "Test '${entityName} - ${operation}' (${httpMethod}) FAILED: Código ${responseCode}"
                                    }
                                } else if (filename && (operation == 'update' || operation == 'delete')) {
                                    echo "Skipping '${entityName} - ${operation}' - No ID creado previamente."
                                }
                            } catch (Exception err) {
                                error "Error al ejecutar el test '${entityName} - ${operation}' (${httpMethod}): ${err.getMessage()}"
                            } finally {
                                sh 'rm -f response.body'
                            }
                        }
                    }
                    echo "Pruebas End-to-End completadas."
                }
            }
            post {
                failure {
                    echo "Algunas pruebas End-to-End fallaron."
                }
                success {
                    echo "Todas las pruebas End-to-End pasaron."
                }
            }
		}
    }
}
