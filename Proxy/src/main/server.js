import express from "express";
import net from "net";
import bodyParser from "body-parser";
import cors from "cors";
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
console.log('=== PROXY STARTUP ===');
console.log('PID:', process.pid);
console.log('__filename:', __filename);
console.log('__dirname:', __dirname);
console.log('Timestamp:', new Date().toISOString());

const LOG_PATH = path.resolve(__dirname, '..', '..', 'proxy.log');
function appendLog(...args){
    try{
        const line = args.map(a => (typeof a === 'string' ? a : JSON.stringify(a))).join(' ') + '\n';
        fs.appendFileSync(LOG_PATH, new Date().toISOString() + ' ' + line);
    }catch(e){ }
}
appendLog('PROXY STARTUP', 'PID:', process.pid, '__filename:', __filename);
process.on('uncaughtException', (err) => {
    console.error('Uncaught exception:', err);
    appendLog('Uncaught exception:', err && err.stack ? err.stack : String(err));
});
process.on('unhandledRejection', (reason) => {
    console.error('Unhandled rejection:', reason);
    appendLog('Unhandled rejection:', reason && reason.stack ? reason.stack : String(reason));
});

const app = express();

// Configuración detallada de CORS
app.use(cors({
    origin: 'http://localhost:8081',
    methods: ['GET', 'POST'],
    allowedHeaders: ['Content-Type'],
    credentials: true
}));

// Middleware para procesar JSON
app.use(bodyParser.json());
app.use((req, res, next) => { console.log(`${req.method} ${req.path}`, req.body); next(); });

const TCP_HOST = "localhost";
const TCP_PORT = 5000;

const activeConnections = new Map();
function connectUser(username) {
    return new Promise((resolve, reject) => {
        if (activeConnections.has(username)) { resolve(activeConnections.get(username)); return; }
        const socket = new net.Socket();
        const userSession = { socket: socket, responseQueue: [] };
        socket.connect(TCP_PORT, TCP_HOST, () => { console.log(`[${username}] Conectado al servidor Java`); socket.write(`type:login|username:${username}\n`); });
        socket.on('data', (data) => {
            const message = data.toString();
            console.log(`[${username}] Recibido de Java: ${message}`);
            if (message.includes('type:login_success')) { activeConnections.set(username, userSession); resolve(userSession); }
            else if (message.includes('type:login_error')) { socket.end(); reject(new Error(message)); }
            else { if (userSession.responseQueue.length > 0) { const { resolve: httpResolve } = userSession.responseQueue.shift(); httpResolve(message); } else { console.log(`[${username}] Notificación push ignorada por ahora: ${message}`); } }
        });
        socket.on('error', (err) => { console.error(`[${username}] Error de socket: ${err.message}`); activeConnections.delete(username); reject(err); userSession.responseQueue.forEach(({ reject: httpReject }) => httpReject(err)); userSession.responseQueue = []; });
        socket.on('close', () => { console.log(`[${username}] Conexión cerrada`); activeConnections.delete(username); });
    });
}

function sendCommand(username, command) {
    return new Promise((resolve, reject) => {
        const session = activeConnections.get(username);
        if (!session) { reject(new Error("Usuario no conectado. Debe hacer login primero.")); return; }
        session.responseQueue.push({ resolve, reject });
        session.socket.write(command + "\n");
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

// Endpoint de prueba
app.get('/api/test', (req, res) => {
    res.json({ message: 'Servidor proxy funcionando correctamente' });
});

// INICIAR SERVIDOR
// Servir el cliente web (frontend)
app.use(express.static(path.join(__dirname, '..', '..', '..', 'Web-Client')));
app.listen(3000, () => {
    console.log("Proxy stateful corriendo en http://localhost:3000");
});