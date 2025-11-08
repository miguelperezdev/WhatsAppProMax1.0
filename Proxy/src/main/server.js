import express from "express";
import net from "net";
import bodyParser from "body-parser";
import cors from "cors"; // Importante para que tu cliente web pueda conectarse

const app = express();
app.use(cors()); // Habilita CORS
app.use(bodyParser.json());

const TCP_HOST = "localhost";
const TCP_PORT = 5000;

// MAPA DE CONEXIONES ACTIVAS
// Clave: username, Valor: { socket: net.Socket, responseQueue: Array }
// 'responseQueue' guardará las promesas (resolve/reject) de las peticiones HTTP que esperan respuesta de Java
const activeConnections = new Map();

// Función auxiliar para conectar un nuevo usuario
function connectUser(username) {
    return new Promise((resolve, reject) => {
        if (activeConnections.has(username)) {
            // Si ya existe una conexión, reúsala (o ciérrala y abre una nueva, según prefieras)
            // Por simplicidad, aquí la reusamos, aunque lo ideal sería verificar si sigue viva.
             resolve(activeConnections.get(username));
             return;
        }

        const socket = new net.Socket();
        const userSession = {
            socket: socket,
            responseQueue: [] // Cola para manejar respuestas asíncronas
        };

        socket.connect(TCP_PORT, TCP_HOST, () => {
            console.log(`[${username}] Conectado al servidor Java`);
            // Enviamos el login inmediatamente al conectar
            socket.write(`type:login|username:${username}`);
        });

        socket.on('data', (data) => {
            const message = data.toString();
            console.log(`[${username}] Recibido de Java: ${message}`);

            // PROCESAMIENTO DE RESPUESTAS
            // Aquí viene la parte delicada: saber qué respuesta corresponde a qué petición HTTP.
            // Una estrategia simple es asumir que Java responde en orden (FIFO).

            if (message.includes('type:login_success')) {
                 activeConnections.set(username, userSession);
                 resolve(userSession);
            } else if (message.includes('type:login_error')) {
                 socket.end(); // Cerramos si falló el login
                 reject(new Error(message));
            } else {
                // Para cualquier otra respuesta, buscamos quién la estaba esperando
                if (userSession.responseQueue.length > 0) {
                    const { resolve: httpResolve } = userSession.responseQueue.shift(); // Sacamos al primero de la fila
                    httpResolve(message);
                } else {
                    // Si nadie la espera, es una notificación push (ej: mensaje recibido de otro usuario)
                    // Por ahora lo ignoramos o solo lo logueamos. En el futuro, esto iría por WebSocket al frontend.
                    console.log(`[${username}] Notificación push ignorada por ahora: ${message}`);
                }
            }
        });

        socket.on('error', (err) => {
            console.error(`[${username}] Error de socket: ${err.message}`);
            activeConnections.delete(username);
            reject(err);
             // También deberíamos rechazar todas las promesas en la cola
             userSession.responseQueue.forEach(({ reject: httpReject }) => httpReject(err));
             userSession.responseQueue = [];
        });

        socket.on('close', () => {
            console.log(`[${username}] Conexión cerrada`);
            activeConnections.delete(username);
        });
    });
}

// Función auxiliar para enviar un comando y esperar respuesta
function sendCommand(username, command) {
    return new Promise((resolve, reject) => {
        const session = activeConnections.get(username);
        if (!session) {
            reject(new Error("Usuario no conectado. Debe hacer login primero."));
            return;
        }

        // Agregamos nuestra promesa a la cola de espera de este usuario
        session.responseQueue.push({ resolve, reject });
        
        // Enviamos el comando
        session.socket.write(command);
    });
}

// === ENDPOINTS ===

// LOGIN (Crea la conexión persistente)
app.post("/api/login", async (req, res) => {
    const { username } = req.body;
    if (!username) return res.status(400).json({ error: "Falta username" });

    try {
        await connectUser(username);
        res.json({ ok: true, message: "Login exitoso" });
    } catch (err) {
        res.status(500).json({ ok: false, error: err.message });
    }
});

// ENVIAR MENSAJE PRIVADO (Reusa la conexión)
app.post("/api/sendMessage", async (req, res) => {
    const { from, to, content } = req.body;
    const cmd = `type:private_message|from:${from}|to:${to}|content:${content}`;
    
    try {
        // Esperamos la confirmación "type:message_sent_ok" que AÑADISTE a tu servidor Java
        const response = await sendCommand(from, cmd);
        res.json({ ok: true, java_response: response });
    } catch (err) {
        res.status(500).json({ ok: false, error: err.message });
    }
});

// CREAR GRUPO
app.post("/api/createGroup", async (req, res) => {
    const { group_name, creator } = req.body;
    const cmd = `type:create_group|group_name:${group_name}|creator:${creator}`;

    try {
        const response = await sendCommand(creator, cmd);
        res.json({ ok: true, java_response: response });
    } catch (err) {
        res.status(500).json({ ok: false, error: err.message });
    }
});

// OBTENER GRUPOS
app.get("/api/groups/:username", async (req, res) => {
    const { username } = req.params;
    const cmd = `type:get_groups|username:${username}`;

    try {
        const response = await sendCommand(username, cmd);
        // Aquí podrías parsear 'response' para devolver un JSON limpio en lugar del string crudo de Java
        res.json({ ok: true, java_response: response });
    } catch (err) {
        res.status(500).json({ ok: false, error: err.message });
    }
});

// INICIAR SERVIDOR
app.listen(3000, () => {
    console.log("Proxy stateful corriendo en http://localhost:3000");
});