# WhatsAppProMax1.0

Este proyecto se desarrolla como solución al Taller de Conexión de Computación en Internet. Es una aplicación de chat de escritorio construida en Java que implementa una arquitectura cliente-servidor para facilitar la comunicación multimedia. La aplicación soporta mensajería de texto, notas de voz y llamadas en tiempo real utilizando Sockets TCP para la señalización y UDP para la transmisión de audio P2P.

## Integrantes

- Miguel Pérez
- Juan Manuel Zuluaga
- Alejandro Rendón

## Prerrequisitos

Para compilar y ejecutar este proyecto, necesitarás:

- **Java Development Kit (JDK):** Versión 11 o superior.
- **IDE (Recomendado):** IntelliJ IDEA, Eclipse o VS Code con soporte para Java.

## Instrucciones de Ejecución

Es fundamental seguir el orden correcto: primero se debe iniciar el servidor y luego los clientes que se conectarán a él.

### 1. Ejecutar el Servidor (MainServer) desde el IDE

- En tu IDE, busca el archivo `MainServer.java` dentro del paquete `ui`.
- Haz clic derecho sobre el archivo y selecciona la opción `Run 'MainServer.main()'`.
- Verás un mensaje en la consola confirmando que el servidor se ha iniciado. Deja esta terminal abierta.

### 2. Ejecutar los Clientes (MainClient) desde el IDE

- Busca el archivo `MainClient.java` en el mismo paquete `ui`.
- Haz clic derecho y selecciona `Run 'MainClient.main()'`. Se abrirá una nueva consola para este cliente.
- Para simular otro usuario, repite el paso anterior. El IDE te permitirá tener varias instancias del cliente corriendo simultáneamente.

## Uso de la Aplicación

### Funcionamiento General

Al iniciar cada cliente, se te pedirá un nombre de usuario único. Una vez dentro, un menú numérico te permitirá acceder a todas las funciones. A continuación se detalla cada opción.

### Descripción Detallada del Menú

1. **Mensaje de texto:**  
   Permite enviar un mensaje escrito. El sistema te preguntará si deseas enviarlo a un usuario específico (privado) o a un grupo.

2. **Nota de voz:**  
   Inicia el proceso para enviar un mensaje de audio. Deberás indicar el destinatario (usuario o grupo) y la duración en segundos que deseas grabar.

3. **Llamada:**  
   Comienza una llamada de voz en tiempo real. Al seleccionar esta opción, deberás especificar el usuario o grupo al que deseas llamar.

4. **Crear grupo:**  
   Te permite registrar un nuevo grupo en el servidor. Deberás proporcionar un nombre único para el grupo que quieres crear.

5. **Unirse a grupo:**  
   Con esta opción puedes unirte a un grupo que ya exista en el servidor.

6. **Ver historial:**  
   Esta opción está diseñada para el historial de texto. El programa guarda todas las conversaciones en archivos `.txt` dentro de la carpeta `data/history`. Puedes navegar a este directorio y abrir los archivos con cualquier editor de texto para ver el historial completo.

7. **Usuarios en línea:**  
   Muestra una lista de todos los nombres de usuario que están conectados al servidor en ese momento.

8. **Grupos:**  
   Despliega una lista con todos los grupos que han sido creados en el servidor.

9. **Reproducir audios guardados:**  
   Abre un submenú donde puedes ver una lista de todas las notas de voz que has enviado y recibido. Puedes seleccionar un audio por su número para escucharlo. Los archivos de audio (`.wav`) se guardan físicamente en la carpeta `data/audio`.

0. **Salir:**  
   Cierra la sesión del usuario, notifica al servidor de la desconexión y termina la aplicación cliente.

### Comandos Especiales para Llamadas

Cuando hay una llamada en curso o entrante, aparecen opciones especiales que se activan con letras:

- **a. Aceptar llamada:**  
  Si recibes una llamada, esta opción aparecerá en el menú. Escríbela para aceptar la comunicación.

- **r. Rechazar llamada:**  
  Si recibes una llamada y no deseas contestar, usa esta opción para rechazarla.

- **c. Colgar llamada:**  
  Durante una llamada activa, esta opción te permitirá finalizarla. Es importante que ambos usuarios cuelguen para terminar la conexión correctamente.
