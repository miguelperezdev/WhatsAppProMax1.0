// Simple front-end logic for login, contacts, groups, and chat UI (in-memory only)

document.addEventListener('DOMContentLoaded', () => {
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

	loginForm.addEventListener('submit', e => {
		e.preventDefault();
		const name = usernameInput.value.trim();
		if (!name) {
			loginStatus.textContent = 'Ingresa un nombre válido.';
			return;
		}
		state.user = name;
		loginStatus.textContent = `Hola, ${name}`;
		// show contacts and chat area
		contactsSection.classList.remove('hidden');
		// hide login inputs but keep greeting
		usernameInput.disabled = true;
		loginForm.querySelector('button').disabled = true;
		// prepopulate a couple of example contacts
		if (state.contacts.length === 0) {
			addContact('Alice');
			addContact('Bob');
		}
		renderContacts();
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

	createGroupForm.addEventListener('submit', e => {
		e.preventDefault();
		const gname = groupNameInput.value.trim();
		const membersRaw = groupMembersInput.value.trim();
		if (!gname || !membersRaw) return;
		const members = membersRaw.split(',').map(s => s.trim()).filter(Boolean);
		const id = 'g:' + gname.toLowerCase().replace(/\s+/g,'_');
		const group = { id, name: gname, type: 'group', members };
		state.contacts.push(group);
		state.messages[id] = state.messages[id] || [];
		renderContacts();
		createGroupForm.classList.add('hidden');
		btnCreateGroup.disabled = false;
		groupNameInput.value = '';
		groupMembersInput.value = '';
	});

	// Message sending
	messageForm.addEventListener('submit', e => {
		e.preventDefault();
		const text = messageInput.value.trim();
		if (!text || !state.activeChat) return;
		const chatId = state.activeChat;
		const msg = { from: state.user, text, time: Date.now() };
		state.messages[chatId] = state.messages[chatId] || [];
		state.messages[chatId].push(msg);
		messageInput.value = '';
		renderMessages(chatId);
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
