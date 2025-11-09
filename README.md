# WhatsAppWebProMax

Este proyecto implementa una versión web del sistema de chat **WhatsAppProMax**, permitiendo la comunicación entre un cliente web moderno y el backend original en Java a través de un proxy intermedio.

## Arquitectura del Sistema

El sistema sigue una arquitectura de 3 capas:

1. **Cliente Web (Frontend):**  
   Interfaz de usuario desarrollada en HTML, CSS y JavaScript.  
   Se comunica exclusivamente vía HTTP con el proxy.

2. **Servidor Proxy (Mediador):**  
   Servidor Node.js con Express.  
   Actúa como traductor, convirtiendo las peticiones HTTP REST del cliente web en comandos TCP persistentes hacia el backend Java.  
   Mantiene el estado de las conexiones.

3. **Backend Java (Servidor TCP):**  
   El servidor original de la Tarea 1.  
   Gestiona la lógica de negocio, usuarios y mensajes a través de sockets TCP.

## Estructura del Proyecto

```plaintext
WhatsAppWebProMax/
│
├── ServidorJava/         # Servidor Backend (Java TCP)
├── Proxy/                # Servidor Intermediario (Node.js Express)
└── Web-Client/           # Cliente Frontend (HTML/CSS/JS)
```

## Instrucciones de Ejecución

Para que el sistema funcione, se deben iniciar los componentes en el siguiente orden:

### 1. Iniciar el Backend (Java)

El servidor TCP debe estar corriendo primero para aceptar conexiones del proxy.

```bash
cd ServidorJava
# Usando Gradle (recomendado)
./gradlew run
# O ejecutando el .jar compilado si ya existe
java -jar build/libs/WhatsAppProMax1.0.jar
```

El servidor iniciará por defecto en el puerto 5000.

### 2. Iniciar el Proxy (Node.js)

El intermediario que conecta la web con Java.

```bash
cd Proxy
# Instalar dependencias (solo la primera vez)
npm install
# Iniciar el servidor
npm start
# O si usas nodemon para desarrollo:
# nodemon src/server.js
```

El proxy iniciará escuchando peticiones HTTP en `http://localhost:3000`.

### 3. Abrir el Cliente Web

Simplemente abre el archivo `index.html` en tu navegador favorito.

```plaintext
Ruta: Web-Client/index.html
```

No requiere servidor web adicional para pruebas locales.

## Flujo de Comunicación (Ejemplo: Enviar Mensaje)

1. Usuario Web: Escribe un mensaje y hace clic en "Enviar".  
2. Cliente JS: Envía una petición `POST /api/sendMessage` con el JSON del mensaje al Proxy (Puerto 3000).  
3. Proxy Express:  
   - Recibe la petición HTTP.  
   - Identifica la conexión TCP activa del usuario remitente.  
   - Envía el comando TCP crudo (ej: `type:private_message|...`) al servidor Java (Puerto 5000).  
4. Servidor Java: Procesa el mensaje y lo reenvía al destinatario. Devuelve una confirmación TCP al proxy.  
5. Proxy Express: Recibe la confirmación TCP y responde con un `HTTP 200 OK` al cliente web.  
6. Cliente JS: Muestra el mensaje en la interfaz del chat.

## Integrantes del Grupo

- Alejandro Rendon
- Juan Manuel Zuluaga 
- Miguel Perez
