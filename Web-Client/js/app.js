console.log('Iniciando aplicación...');

// Configuración del cliente
const API_URL = 'http://localhost:3000/api';

// Verificar conexión con el servidor
(async function verificarServidor() {
    try {
        const response = await fetch(`${API_URL}/test`);
        const data = await response.json();
        console.log('Conexión con el servidor:', data.message);
    } catch (err) {
        console.error('Error al conectar con el servidor:', err);
    }
})();

// Funciones de API
async function apiLogin(username) {
    try {
        console.log('Intentando login con:', username);
        const res = await fetch(`${API_URL}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username })
        });
        console.log('Respuesta del servidor:', res);
        if (!res.ok) {
            const error = await res.text();
            console.error('Error del servidor:', error);
            throw new Error(`Login failed: ${error}`);
        }
        const data = await res.json();
        console.log('Login exitoso:', data);
        return data;
    } catch (err) {
        console.error('Error en login:', err);
        throw err;
    }
}

async function apiSendMessage(from, to, content) {
    try {
        const res = await fetch(`${API_URL}/sendMessage`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ from, to, content })
        });
        if (!res.ok) throw new Error('Error al enviar mensaje');
        return await res.json();
    } catch (err) {
        console.error('Error enviando mensaje:', err);
        throw err;
    }
}

async function apiCreateGroup(group_name, creator) {
    try {
        const res = await fetch(`${API_URL}/createGroup`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ group_name, creator })
        });
        if (!res.ok) throw new Error('Error creando grupo');
        return await res.json();
    } catch (err) {
        console.error('Error creando grupo:', err);
        throw err;
    }
}

async function apiGetGroups(username) {
    try {
        const res = await fetch(`${API_URL}/groups/${username}`);
        if (!res.ok) throw new Error('Error obteniendo grupos');
        return await res.json();
    } catch (err) {
        console.error('Error obteniendo grupos:', err);
        throw err;
    }
}

// Asegurarnos de que el DOM está cargado
document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM cargado, configurando eventos...');
    const loginForm = document.getElementById('login-form');
    const usernameInput = document.getElementById('username');
    const loginStatus = document.getElementById('login-status');

	const contactsSection = document.getElementById('contacts-section');
	const contactsList = document.getElementById('contacts-list');
	const btnCreateGroup = document.getElementById('btn-create-group');
	const createGroupForm = document.getElementById('create-group-form');
	const groupNameInput = document.getElementById('group-name');
	const groupMembersInput = document.getElementById('group-members');
	const cancelCreate = document.getElementById('cancel-create');

	const chatSection = document.getElementById('chat-section');
	const chatTitle = document.getElementById('chat-title');
	const chatSubtitle = document.getElementById('chat-subtitle');
	const chatLog = document.getElementById('chat-log');
	const messageForm = document.getElementById('message-form');
	const messageInput = document.getElementById('message');

	let state = {
		user: null,
		contacts: [], // { id, name, type: 'user'|'group', members: [] }
		messages: {}, // key: chatId, value: [{from, text, time}]
		activeChat: null
	};

	function renderContacts() {
		contactsList.innerHTML = '';
		if (state.contacts.length === 0) {
			const li = document.createElement('li');
			li.className = 'muted';
			li.textContent = 'No hay contactos aún. Crea uno o un grupo.';
			contactsList.appendChild(li);
			return;
		}

		state.contacts.forEach(c => {
			const li = document.createElement('li');
			li.dataset.id = c.id;
			const avatar = document.createElement('div');
			avatar.className = 'avatar';
			avatar.textContent = c.name.charAt(0).toUpperCase();
			const meta = document.createElement('div');
			meta.style.flex = '1';
			const name = document.createElement('div');
			name.className = 'contact-name';
			name.textContent = c.name;
			const sub = document.createElement('div');
			sub.className = 'contact-meta';
			sub.textContent = c.type === 'group' ? `Grupo • ${c.members.length} miembros` : 'Contacto';
			meta.appendChild(name);
			meta.appendChild(sub);
			li.appendChild(avatar);
			li.appendChild(meta);
			li.addEventListener('click', () => openChat(c.id));
			contactsList.appendChild(li);
		});
	}

	function openChat(id) {
		const c = state.contacts.find(x => x.id === id);
		if (!c) return;
		state.activeChat = id;
		chatTitle.textContent = c.name + (c.type === 'group' ? ' (grupo)' : '');
		chatSubtitle.textContent = c.type === 'group' ? `Miembros: ${c.members.join(', ')}` : '';
		chatSection.classList.remove('hidden');
		messageForm.classList.remove('hidden');
		renderMessages(id);
	}

	function renderMessages(chatId) {
		chatLog.innerHTML = '';
		const msgs = state.messages[chatId] || [];
		msgs.forEach(m => {
			const div = document.createElement('div');
			div.className = 'message ' + (m.from === state.user ? 'me' : 'their');
			div.innerHTML = `<div class="content">${escapeHtml(m.text)}</div><span class="meta">${m.from} • ${formatTime(m.time)}</span>`;
			chatLog.appendChild(div);
		});
		chatLog.scrollTop = chatLog.scrollHeight;
	}

	function escapeHtml(s) {
		return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\"/g,'&quot;');
	}

	function formatTime(ts) {
		const d = new Date(ts);
		return d.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'});
	}

	// Configurar el manejador del formulario de login
	loginForm.addEventListener('submit', async e => {
		e.preventDefault();
		console.log('Formulario enviado');
		
		const name = usernameInput.value.trim();
		console.log('Nombre ingresado:', name);
		
		if (!name) {
			loginStatus.textContent = 'Ingresa un nombre válido.';
			return;
		}
		
		loginStatus.textContent = 'Conectando...';
		
		try {
			console.log(`Intentando conectar a ${API_URL}/login`);
			const response = await fetch(`${API_URL}/login`, {
				method: 'POST',
				headers: { 
					'Content-Type': 'application/json',
					'Accept': 'application/json'
				},
				credentials: 'include',
				mode: 'cors',
				body: JSON.stringify({ username: name })
			});
			
			console.log('Respuesta del servidor:', response);
			
			if (!response.ok) {
				const errorText = await response.text();
				throw new Error(`Error del servidor: ${errorText}`);
			}
			
			const data = await response.json();
			console.log('Datos de respuesta:', data);
			
			state.user = name;
			loginStatus.textContent = `Hola, ${name}`;
			contactsSection.classList.remove('hidden');
			usernameInput.disabled = true;
			loginForm.querySelector('button').disabled = true;
			// intentar cargar grupos del usuario (no bloquear el login si falla)
			try {
				const groupsData = await apiGetGroups(name);
				let groups = [];
				if (Array.isArray(groupsData)) {
					groups = groupsData;
				} else if (groupsData && groupsData.ok && groupsData.java_response) {
					// el proxy puede devolver { ok: true, java_response: '...' }
					// intentamos interpretar java_response como JSON array o como lista separada por comas
					try {
						const parsed = JSON.parse(groupsData.java_response);
						if (Array.isArray(parsed)) groups = parsed;
					} catch (e) {
						// no es JSON; intentar split por comas
						groups = String(groupsData.java_response).split(',').map(s => s.trim()).filter(Boolean);
					}
				} else if (typeof groupsData === 'string') {
					groups = groupsData.split(',').map(s => s.trim()).filter(Boolean);
				}

				groups.forEach(groupName => {
					const group = {
						id: 'g:' + groupName.toLowerCase().replace(/\s+/g,'_'),
						name: groupName,
						type: 'group',
						members: []
					};
					state.contacts.push(group);
					state.messages[group.id] = [];
				});
				renderContacts();
			} catch (err) {
				console.warn('No se pudieron cargar los grupos (no crítico):', err);
			}
		} catch (err) {
			loginStatus.textContent = 'Error al conectar: ' + err.message;
		}
	});

	function addContact(name) {
		const id = 'u:' + name.toLowerCase().replace(/\s+/g,'_');
		const contact = { id, name, type: 'user', members: [name] };
		state.contacts.push(contact);
		state.messages[id] = state.messages[id] || [];
	}

	// Group creation
	btnCreateGroup.addEventListener('click', () => {
		createGroupForm.classList.remove('hidden');
		btnCreateGroup.disabled = true;
	});

	cancelCreate.addEventListener('click', () => {
		createGroupForm.classList.add('hidden');
		btnCreateGroup.disabled = false;
		groupNameInput.value = '';
		groupMembersInput.value = '';
	});

	createGroupForm.addEventListener('submit', async e => {
		e.preventDefault();
		const gname = groupNameInput.value.trim();
		const membersRaw = groupMembersInput.value.trim();
		if (!gname || !membersRaw) return;
		const members = membersRaw.split(',').map(s => s.trim()).filter(Boolean);

		try {
			await apiCreateGroup(gname, state.user);
			const id = 'g:' + gname.toLowerCase().replace(/\s+/g,'_');
			const group = { id, name: gname, type: 'group', members };
			state.contacts.push(group);
			state.messages[id] = state.messages[id] || [];
			renderContacts();
			createGroupForm.classList.add('hidden');
			btnCreateGroup.disabled = false;
			groupNameInput.value = '';
			groupMembersInput.value = '';
		} catch (err) {
			console.error('Error al crear grupo:', err);
			alert('No se pudo crear el grupo: ' + err.message);
		}
	});

	// Message sending
	messageForm.addEventListener('submit', async e => {
		e.preventDefault();
		const text = messageInput.value.trim();
		if (!text || !state.activeChat) return;
		const chatId = state.activeChat;
		const chat = state.contacts.find(c => c.id === chatId);
		if (!chat) return;

		try {
			await apiSendMessage(state.user, chat.name, text);
			const msg = { from: state.user, text, time: Date.now() };
			state.messages[chatId] = state.messages[chatId] || [];
			state.messages[chatId].push(msg);
			messageInput.value = '';
			renderMessages(chatId);
		} catch (err) {
			console.error('Error al enviar mensaje:', err);
			alert('No se pudo enviar el mensaje: ' + err.message);
		}
	});

	// Helper: open first chat when available
	function openFirstIfNone() {
		if (!state.activeChat && state.contacts.length > 0) {
			openChat(state.contacts[0].id);
		}
	}

	// Expose a simple API to add contacts manually via button
	const btnNewContact = document.getElementById('btn-new-contact');
	btnNewContact.addEventListener('click', () => {
		const name = prompt('Nombre del nuevo contacto:');
		if (name) {
			addContact(name.trim());
			renderContacts();
			openFirstIfNone();
		}
	});

});