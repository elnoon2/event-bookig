(function () {
    const SESSION_KEY = 'badyaAdminSession';
    let scannerActive = false;
    let html5QrCode = null;
    let lastScannedToken = '';
    let lastScannedAtMs = 0;
    let scanInProgress = false;
    const loadedTabs = new Set(['events']);

    function apiBase() {
        if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
            return window.location.origin;
        }
        return (window.ADMIN_CONFIG && window.ADMIN_CONFIG.API_BASE_URL) || 'http://localhost:5000';
    }

    function siteBase() {
        const configured = window.ADMIN_CONFIG && window.ADMIN_CONFIG.SITE_BASE_URL;
        if (configured && String(configured).trim()) return String(configured).replace(/\/+$/, '');
        if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
            return window.location.origin;
        }
        return '';
    }

    function profileUrlForUserId(userId) {
        const base = siteBase();
        if (!base) return `profile.html?userId=${encodeURIComponent(userId)}`;
        return `${base}/profile.html?userId=${encodeURIComponent(userId)}`;
    }

    function qrImgUrlForText(text, size = 180) {
        const qs = new URLSearchParams({
            data: text,
            size: `${size}x${size}`,
            margin: '10'
        });
        return `https://api.qrserver.com/v1/create-qr-code/?${qs.toString()}`;
    }

    function getSession() {
        try {
            // First check sessionStorage
            const raw = sessionStorage.getItem(SESSION_KEY);
            if (raw) {
                const s = JSON.parse(raw);
                if (s) {
                    s.role = 'admin'; // ensure role is set
                    return s;
                }
            }
            // Fallback to localStorage currentUser
            const localRaw = localStorage.getItem('currentUser');
            if (localRaw) {
                const u = JSON.parse(localRaw);
                if (u && u.role === 'admin') {
                    return u;
                }
            }
            return null;
        } catch {
            return null;
        }
    }

    function setSession(data) {
        sessionStorage.setItem(SESSION_KEY, JSON.stringify(data));
        // Sync to localStorage
        localStorage.setItem('currentUser', JSON.stringify({
            id: data.id,
            name: data.name,
            email: data.email,
            role: 'admin'
        }));
    }

    function clearSession() {
        sessionStorage.removeItem(SESSION_KEY);
        try {
            const localRaw = localStorage.getItem('currentUser');
            if (localRaw) {
                const u = JSON.parse(localRaw);
                if (u && u.role === 'admin') {
                    localStorage.removeItem('currentUser');
                }
            }
        } catch (e) {
            console.error(e);
        }
    }

    function el(id) {
        return document.getElementById(id);
    }

    function toast(msg, type = 'success') {
        const t = document.createElement('div');
        t.className = `admin-toast ${type}`;
        t.textContent = msg;
        document.body.appendChild(t);
        setTimeout(() => t.remove(), 3500);
    }

    function escapeHtml(s) {
        const d = document.createElement('div');
        d.textContent = s == null ? '' : String(s);
        return d.innerHTML;
    }

    function eventId(ev) {
        return ev._id != null ? ev._id : ev.id;
    }

    function normalizeTimeForApi(t) {
        if (!t) return '10:00:00';
        const s = String(t);
        return s.length === 5 ? `${s}:00` : s;
    }

    function timeInputValue(t) {
        if (!t) return '';
        const s = String(t);
        return s.length >= 5 ? s.slice(0, 5) : s;
    }

    function dateInputValue(d) {
        if (d == null) return '';
        if (Array.isArray(d) && d.length >= 3) {
            return `${d[0]}-${String(d[1]).padStart(2, '0')}-${String(d[2]).padStart(2, '0')}`;
        }
        return String(d).split('T')[0];
    }

    function fmtDate(d) {
        if (d == null || d === '') return '—';
        const iso = dateInputValue(d);
        const parts = iso.split('-').map(Number);
        if (parts.length === 3 && parts.every(n => !Number.isNaN(n))) {
            const x = new Date(parts[0], parts[1] - 1, parts[2]);
            return escapeHtml(x.toLocaleDateString(undefined, { dateStyle: 'medium' }));
        }
        return escapeHtml(String(d));
    }

    function fmtDateTime(iso) {
        if (!iso) return '—';
        const x = new Date(iso);
        if (Number.isNaN(x.getTime())) return escapeHtml(String(iso));
        return escapeHtml(x.toLocaleString());
    }

    function showLogin() {
        const login = el('login-screen');
        const app = el('admin-app');
        if (login) {
            login.hidden = false;
            login.style.display = 'flex';
        }
        if (app) {
            app.hidden = true;
            app.style.display = 'none';
        }
    }

    function showApp(session) {
        const login = el('login-screen');
        const app = el('admin-app');
        if (login) {
            login.hidden = true;
            login.style.display = 'none';
        }
        if (app) {
            app.hidden = false;
            app.style.display = 'flex'; // The CSS uses flex for #admin-app
        }
        const welcome = el('admin-welcome');
        if (welcome && session) {
            welcome.textContent = `${session.name || 'Admin'} · ${session.email || ''}`;
        }
    }

    function requireSession() {
        const s = getSession();
        if (!s || s.role !== 'admin') {
            showLogin();
            return null;
        }
        return s;
    }

    async function loginRequest(email, password) {
        const res = await fetch(`${apiBase()}/api/admin/authenticate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        const data = await res.json().catch(() => ({}));
        if (!res.ok) throw new Error(data.message || 'Sign in failed');
        return data;
    }


    async function loadEvents() {
        if (!requireSession()) return;
        try {
            const res = await fetch(`${apiBase()}/api/events`);
            const data = await res.json();
            window.__adminEvents = Array.isArray(data) ? data : [];
            renderEvents();
            renderCreatedEvents();
            populateScannerEventSelect();
        } catch {
            toast('Failed to load events', 'error');
        }
    }

    function renderEvents() {
        const tbody = el('events-table-body');
        const list = window.__adminEvents || [];
        const s = getSession();
        tbody.innerHTML = '';
        el('events-empty').hidden = list.length > 0;
        list.forEach(ev => {
            const id = eventId(ev);
            const remaining = ev.ticketsAvailable != null ? Math.max(0, Number(ev.ticketsAvailable) || 0) : '—';
            const canEdit = s && ev.createdByAdminId != null && String(ev.createdByAdminId) === String(s.id);
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${escapeHtml(ev.title)}</td>
                <td>${escapeHtml(remaining)}</td>
                <td>${fmtDate(ev.date)}</td>
                <td>${escapeHtml(ev.venue)}</td>
                <td>${escapeHtml(ev.category)}</td>
                <td>
                    <button type="button" class="admin-btn admin-btn-sm admin-btn-primary" data-action="edit-event" data-id="${id}">Edit</button>
                    <button type="button" class="admin-btn admin-btn-sm admin-btn-danger" data-action="delete-event" data-id="${id}">Remove</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    function renderCreatedEvents() {
        const s = getSession();
        const tbody = el('created-events-table-body');
        if (!tbody) return;
        const list = (window.__adminEvents || []).filter(
            ev => s && ev.createdByAdminId != null && String(ev.createdByAdminId) === String(s.id)
        );
        tbody.innerHTML = '';
        el('created-events-empty').hidden = list.length > 0;
        list.forEach(ev => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${escapeHtml(ev.title)}</td>
                <td>${fmtDate(ev.date)}</td>
                <td>${escapeHtml(ev.venue)}</td>
                <td>Approved</td>
            `;
            tbody.appendChild(tr);
        });
    }

    function populateProfile() {
        const s = getSession();
        if (!s) return;
        const nameEl = el('profile-name');
        const emailEl = el('profile-email');
        if (nameEl) nameEl.textContent = s.name || '—';
        if (emailEl) emailEl.textContent = s.email || '—';
    }

    function populateScannerEventSelect() {
        const sel = el('scanner-event-select');
        if (!sel) return;
        // Show ALL events in the scanner dropdown, not just those created by current admin
        const list = window.__adminEvents || [];
        sel.innerHTML = '<option value="">Select event to check in...</option>';
        list.forEach(ev => {
            const id = eventId(ev);
            const opt = document.createElement('option');
            opt.value = String(id);
            opt.textContent = `${ev.title} (${dateInputValue(ev.date) || 'no date'})`;
            sel.appendChild(opt);
        });
    }

    async function deleteEventById(id) {
        const s = requireSession();
        if (!s) return;
        if (!confirm('Delete this event? Related bookings will be removed.')) return;
        try {
            const res = await fetch(`${apiBase()}/api/admin/events/${id}`, {
                method: 'DELETE'
            });
            if (!res.ok) {
                const errData = await res.json().catch(() => ({}));
                throw new Error(errData.message || 'Delete failed');
            }
            toast('Event removed');
            await loadEvents();
        } catch (err) {
            toast('Error: ' + err.message, 'error');
        }
    }

    function openEventModal(isEdit, ev) {
        el('event-modal').classList.add('open');
        el('event-modal-title').textContent = isEdit ? 'Edit event' : 'Add event';
        el('ev-id').value = isEdit && ev ? String(eventId(ev)) : '';
        el('ev-title').value = ev?.title || '';
        el('ev-description').value = ev?.description || '';
        el('ev-category').value = ev?.category || '';
        el('ev-date').value = ev ? dateInputValue(ev.date) : '';
        el('ev-time').value = ev ? timeInputValue(ev.time) : '';
        el('ev-venue').value = ev?.venue || '';
        el('ev-capacity').value = ev?.capacity != null ? ev.capacity : '';
        el('ev-price').value = ev?.price != null ? ev.price : '';
        el('ev-image').value = ev?.image || '';
        el('ev-organizer').value = ev?.organizer || '';
        el('ev-contact-email').value = ev?.contactEmail || '';
        
        // Handle target faculties checkboxes
        const targetStr = ev?.targetFaculties || '';
        const faculties = targetStr.split(',');
        document.querySelectorAll('.target-faculty').forEach(cb => {
            cb.checked = faculties.includes(cb.value);
        });
    }

    function closeEventModal() {
        el('event-modal').classList.remove('open');
    }

    async function saveEvent(e) {
        e.preventDefault();
        const s = requireSession();
        if (!s) return;
        const id = el('ev-id').value.trim();
        
        const fileInput = el('ev-file-upload');
        let imageUrl = el('ev-image').value.trim();

        // Step 1: Upload file if selected
        if (fileInput && fileInput.files.length > 0) {
            const formData = new FormData();
            formData.append('file', fileInput.files[0]);
            
            toast('Uploading media...', 'info');
            try {
                const uploadResponse = await fetch(`${apiBase()}/api/upload`, {
                    method: 'POST',
                    body: formData
                });
                
                if (uploadResponse.ok) {
                    const uploadData = await uploadResponse.json();
                    imageUrl = uploadData.url;
                } else {
                    toast('Media upload failed. Using existing/default image.', 'error');
                }
            } catch (uploadError) {
                console.error('Upload error:', uploadError);
                toast('Media upload failed.', 'error');
            }
        }

        const body = {
            title: el('ev-title').value.trim(),
            description: el('ev-description').value.trim(),
            category: el('ev-category').value,
            date: el('ev-date').value,
            time: normalizeTimeForApi(el('ev-time').value),
            venue: el('ev-venue').value.trim(),
            capacity: parseInt(el('ev-capacity').value, 10),
            price: parseFloat(el('ev-price').value),
            image: imageUrl || 'https://images.unsplash.com/photo-1540575467063-178a50c2e87c',
            organizer: el('ev-organizer').value.trim(),
            contactEmail: el('ev-contact-email').value.trim(),
            targetFaculties: Array.from(document.querySelectorAll('.target-faculty:checked')).map(cb => cb.value).join(',') || 'All'
        };
        const url = id ? `${apiBase()}/api/admin/events/${id}` : `${apiBase()}/api/admin/events`;
        const method = id ? 'PUT' : 'POST';
        try {
            const res = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Save failed');
            }
            toast(id ? 'Event updated' : 'Event created');
            closeEventModal();
            // Clear file input
            if (fileInput) fileInput.value = '';
            await loadEvents();
        } catch (err) {
            toast(err.message || 'Could not save event', 'error');
        }
    }

    async function loadUsers() {
        if (!requireSession()) return;
        try {
            const res = await fetch(`${apiBase()}/api/users`);
            const data = await res.json();
            window.__adminUsers = Array.isArray(data) ? data : [];
            renderUsers();
        } catch {
            toast('Failed to load users', 'error');
        }
    }

    function renderUsers() {
        const tbody = el('users-table-body');
        const list = window.__adminUsers || [];
        tbody.innerHTML = '';
        el('users-empty').hidden = list.length > 0;
        list.forEach(u => {
            const tr = document.createElement('tr');
            const profileUrl = profileUrlForUserId(u.id);
            const qrImg = qrImgUrlForText(profileUrl, 180);
            tr.innerHTML = `
                <td>${escapeHtml(u.id)}</td>
                <td>${escapeHtml(u.name)}</td>
                <td>${escapeHtml(u.email)}</td>
                <td class="admin-qr-cell">
                    <img class="admin-qr-img" alt="QR code for ${escapeHtml(u.name)} profile" src="${qrImg}" loading="lazy" decoding="async">
                    <div class="admin-qr-actions">
                        <a href="${escapeHtml(profileUrl)}" target="_blank" rel="noopener">Open</a>
                        <button type="button" class="admin-btn admin-btn-sm admin-btn-ghost" data-action="copy-profile-link" data-id="${u.id}">Copy link</button>
                    </div>
                </td>
                <td>
                    <button type="button" class="admin-btn admin-btn-sm admin-btn-primary" data-action="edit-user" data-id="${u.id}">Edit</button>
                    <button type="button" class="admin-btn admin-btn-sm admin-btn-danger" data-action="delete-user" data-id="${u.id}">Remove</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    function openUserModal(u) {
        el('user-modal').classList.add('open');
        el('usr-id').value = String(u.id);
        el('usr-name').value = u.name || '';
        el('usr-email').value = u.email || '';
        el('usr-password').value = '';
    }

    function closeUserModal() {
        el('user-modal').classList.remove('open');
    }

    async function saveUser(e) {
        e.preventDefault();
        if (!requireSession()) return;
        const id = el('usr-id').value;
        const name = el('usr-name').value.trim();
        const email = el('usr-email').value.trim();
        const password = el('usr-password').value;
        const body = { name, email };
        if (password) body.password = password;
        try {
            const res = await fetch(`${apiBase()}/api/admin/users/${id}`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.message || 'Update failed');
            }
            toast('User updated');
            closeUserModal();
            await loadUsers();
        } catch (err) {
            toast(err.message || 'Could not update user', 'error');
        }
    }

    async function deleteUserById(id) {
        if (!requireSession()) return;
        if (!confirm('Remove this user? They will no longer be able to sign in.')) return;
        try {
            const res = await fetch(`${apiBase()}/api/admin/users/${id}`, { method: 'DELETE' });
            if (!res.ok) throw new Error('Delete failed');
            toast('User removed');
            await loadUsers();
        } catch {
            toast('Could not remove user (they may have bookings or attendance records).', 'error');
        }
    }

    async function loadBookings() {
        if (!requireSession()) return;
        try {
            const res = await fetch(`${apiBase()}/api/admin/bookings`);
            const data = await res.json();
            window.__adminBookings = Array.isArray(data) ? data : [];
            renderBookings();
        } catch (err) {
            toast('Failed to load bookings', 'error');
        }
    }

    function renderBookings() {
        const tbody = el('bookings-table-body');
        const list = window.__adminBookings || [];
        tbody.innerHTML = '';
        el('bookings-empty').hidden = list.length > 0;
        list.forEach(b => {
            const title = b.event && b.event.title ? b.event.title : `Event #${b.eventId}`;
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${escapeHtml(title)}</td>
                <td>${escapeHtml(b.studentName)}</td>
                <td>${escapeHtml(b.studentEmail)}</td>
                <td>${escapeHtml(b.tickets)}</td>
                <td>${escapeHtml(b.totalAmount)}</td>
                <td>${fmtDateTime(b.bookingDate)}</td>
                <td>
                    <button type="button" class="admin-btn admin-btn-sm admin-btn-danger" data-action="cancel-booking" data-id="${b.id}">Cancel</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    async function refreshAll() {
        if (!requireSession()) return;
        populateProfile();
        loadedTabs.clear();
        loadedTabs.add('events');
        await loadEvents();
    }

    async function ensureTabData(tab) {
        if (!requireSession() || loadedTabs.has(tab)) return;
        loadedTabs.add(tab);
        if (tab === 'users') await loadUsers();
        if (tab === 'bookings') await loadBookings();
    }

    function parseTokenFromQrText(rawText) {
        if (!rawText) return null;
        const t = String(rawText).trim();
        const directMatch = t.match(/(?:^|[?&])token=([^&\s]+)/);
        if (directMatch) {
            try {
                return decodeURIComponent(directMatch[1]);
            } catch {
                return directMatch[1];
            }
        }
        return t;
    }

    async function submitAttendanceToken(rawToken) {
        const s = requireSession();
        if (!s) return;
        if (scanInProgress) return;

        const eventId = el('scanner-event-select')?.value;
        if (!eventId) {
            toast('Select an event first', 'error');
            return;
        }

        const now = Date.now();
        if (rawToken === lastScannedToken && now - lastScannedAtMs < 5000) {
            return;
        }
        lastScannedToken = rawToken;
        lastScannedAtMs = now;

        scanInProgress = true;
        try {
            await submitAttendanceTokenCore(rawToken, s, eventId);
        } finally {
            scanInProgress = false;
        }
    }

    async function submitAttendanceTokenCore(rawToken, s, eventId) {

        const out = el('scanner-last-result');

        // Extract token payload (handles URL wrapper if present)
        const token = parseTokenFromQrText(rawToken) || rawToken;

        // Check if this is a ticket QR code (starts with TICKET:)
        if (token.startsWith('TICKET:')) {
            try {
                const body = {
                    qrToken: token,
                    eventId: eventId ? Number(eventId) : null,
                    adminUsername: s.name || s.email || 'Admin'
                };
                const res = await fetch(`${apiBase()}/api/events/bookings/scan`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(body)
                });
                
                const data = await res.json().catch(() => ({}));
                if (!res.ok) {
                    const errMsg = `❌ Scan Fail: ${data.message || 'Invalid ticket code'}`;
                    if (out) {
                        out.className = 'error';
                        out.style.backgroundColor = 'rgba(239, 68, 68, 0.15)';
                        out.style.border = '1px solid #ef4444';
                        out.style.color = '#ef4444';
                        out.style.padding = '15px';
                        out.style.borderRadius = '8px';
                        out.style.marginTop = '15px';
                        out.textContent = errMsg;
                        out.hidden = false;
                    }
                    throw new Error(errMsg);
                }
                
                const successMsg = `✅ Ticket Verified: ${data.studentName || 'Student'} (${data.studentEmail || ''}) - Status: USED (Scanned successfully)`;
                if (out) {
                    out.className = 'success';
                    out.style.backgroundColor = 'rgba(16, 185, 129, 0.15)';
                    out.style.border = '1px solid #10b981';
                    out.style.color = '#10b981';
                    out.style.padding = '15px';
                    out.style.borderRadius = '8px';
                    out.style.marginTop = '15px';
                    out.textContent = successMsg;
                    out.hidden = false;
                }
                toast('Ticket checked in successfully!');
                
                // Refresh bookings table
                loadBookings();
            } catch (err) {
                // Offline fallback
                const ticketId = token.substring(7);
                const localBookings = JSON.parse(localStorage.getItem('bookings') || '[]');
                const bIndex = localBookings.findIndex(b => String(b.id) === String(ticketId));
                
                if (bIndex !== -1) {
                    const b = localBookings[bIndex];
                    if (b.ticketStatus === 'USED') {
                        const errMsg = `❌ Scan Fail: Ticket already used (Offline)`;
                        if (out) {
                            out.className = 'error';
                            out.style.backgroundColor = 'rgba(239, 68, 68, 0.15)';
                            out.style.border = '1px solid #ef4444';
                            out.style.color = '#ef4444';
                            out.style.padding = '15px';
                            out.style.borderRadius = '8px';
                            out.style.marginTop = '15px';
                            out.textContent = errMsg;
                            out.hidden = false;
                        }
                        return;
                    }
                    
                    // Mark as used
                    localBookings[bIndex].ticketStatus = 'USED';
                    localStorage.setItem('bookings', JSON.stringify(localBookings));
                    
                    const successMsg = `✅ Ticket Verified: ${b.studentName || 'Student'} (${b.studentEmail || ''}) - Status: USED (Scanned offline)`;
                    if (out) {
                        out.className = 'success';
                        out.style.backgroundColor = 'rgba(16, 185, 129, 0.15)';
                        out.style.border = '1px solid #10b981';
                        out.style.color = '#10b981';
                        out.style.padding = '15px';
                        out.style.borderRadius = '8px';
                        out.style.marginTop = '15px';
                        out.textContent = successMsg;
                        out.hidden = false;
                    }
                    toast('Ticket checked in successfully (Offline)!');
                    
                    // Refresh bookings table
                    loadBookings();
                } else {
                    toast(err.message || 'Invalid ticket code', 'error');
                }
            }
            return;
        }

        // Default to attendance token flow
        if (!token) {
            toast('No token found in QR content', 'error');
            return;
        }
        
        try {
            const res = await fetch(
                `${apiBase()}/api/attendance/scan?token=${encodeURIComponent(token)}&eventId=${encodeURIComponent(eventId)}`,
                { method: 'POST' }
            );
            const data = await res.json().catch(() => ({}));
            
            if (!res.ok) {
                const errMsg = `Scan Again: ${data.message || 'Invalid QR code'}`;
                if (out) {
                    out.className = 'error';
                    out.style.backgroundColor = 'rgba(239, 68, 68, 0.15)';
                    out.style.border = '1px solid #ef4444';
                    out.style.color = '#ef4444';
                    out.style.padding = '15px';
                    out.style.borderRadius = '8px';
                    out.style.marginTop = '15px';
                    out.textContent = errMsg;
                    out.hidden = false;
                }
                throw new Error(errMsg);
            }

            if (data.alreadyRegistered) {
                const errMsg = `Already Scanned Before: ${data.message || 'Student is already registered'}`;
                if (out) {
                    out.className = 'error';
                    out.style.backgroundColor = 'rgba(239, 68, 68, 0.15)';
                    out.style.border = '1px solid #ef4444';
                    out.style.color = '#ef4444';
                    out.style.padding = '15px';
                    out.style.borderRadius = '8px';
                    out.style.marginTop = '15px';
                    out.textContent = errMsg;
                    out.hidden = false;
                }
                throw new Error(errMsg);
            }

            const successMsg = `✅ Attendance Registered: ${data.studentName || 'Student'} (${data.studentEmail || ''})`;
            if (out) {
                out.className = 'success';
                out.style.backgroundColor = 'rgba(16, 185, 129, 0.15)';
                out.style.border = '1px solid #10b981';
                out.style.color = '#10b981';
                out.style.padding = '15px';
                out.style.borderRadius = '8px';
                out.style.marginTop = '15px';
                out.textContent = successMsg;
                out.hidden = false;
            }
            toast(successMsg);
        } catch (err) {
            toast(err.message, 'error');
        }
    }

    async function startQrScanner() {
        if (scannerActive) return;
        const status = el('scanner-status');
        const reader = el('scanner-reader');
        if (!reader) return;

        reader.hidden = false;
        scannerActive = true;
        if (status) status.textContent = 'Initializing camera...';

        try {
            html5QrCode = new Html5Qrcode("scanner-reader");
            const config = { 
                fps: 5, 
                qrbox: (width, height) => {
                    const min = Math.min(width, height);
                    const size = Math.floor(min * 0.7);
                    return { width: size, height: size };
                }
            };
            
            try {
                await html5QrCode.start(
                    { facingMode: "environment" },
                    config,
                    async (decodedText, decodedResult) => {
                        await submitAttendanceToken(decodedText);
                    },
                    (errorMessage) => {}
                );
            } catch (envErr) {
                console.warn("Environment camera not found, trying user facing camera...", envErr);
                await html5QrCode.start(
                    { facingMode: "user" },
                    config,
                    async (decodedText, decodedResult) => {
                        await submitAttendanceToken(decodedText);
                    },
                    (errorMessage) => {}
                );
            }
            
            if (status) status.textContent = 'Scanner running. Point camera at attendee QR code.';
        } catch (err) {
            console.error('Camera startup error', err);
            toast('Could not access camera: ' + err.message, 'error');
            if (status) status.textContent = 'Failed to access camera: ' + err.message;
            scannerActive = false;
            reader.hidden = true;
        }
    }

    function stopQrScanner() {
        scannerActive = false;
        const status = el('scanner-status');
        if (status) status.textContent = 'Stopping camera...';

        if (html5QrCode) {
            html5QrCode.stop().then(() => {
                html5QrCode = null;
                const reader = el('scanner-reader');
                if (reader) reader.hidden = true;
                if (status) status.textContent = 'Scanner is idle.';
            }).catch(err => {
                console.error('Failed to stop camera', err);
                html5QrCode = null;
                const reader = el('scanner-reader');
                if (reader) reader.hidden = true;
                if (status) status.textContent = 'Scanner is idle.';
            });
        } else {
            const reader = el('scanner-reader');
            if (reader) reader.hidden = true;
            if (status) status.textContent = 'Scanner is idle.';
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        const session = getSession();
        if (session && session.role === 'admin') {
            showApp(session);
            refreshAll();
        } else {
            showLogin();
        }

        el('admin-login-form').addEventListener('submit', async e => {
            e.preventDefault();
            el('login-error').textContent = '';
            const email = el('admin-email').value.trim();
            const password = el('admin-password').value;
            try {
                const data = await loginRequest(email, password);
                setSession({ ...data, role: 'admin' });
                showApp(data);
                el('admin-password').value = '';
                await refreshAll();
                toast('Signed in');
            } catch (err) {
                el('login-error').textContent = err.message || 'Sign in failed';
            }
        });

        el('admin-logout').addEventListener('click', () => {
            stopQrScanner();
            clearSession();
            showLogin();
            toast('Signed out');
        });

        document.querySelectorAll('.admin-tabs button').forEach(btn => {
            btn.addEventListener('click', () => {
                const tab = btn.dataset.tab;
                if (tab !== 'scanner' && scannerActive) {
                    stopQrScanner();
                }
                document.querySelectorAll('.admin-tabs button').forEach(b => {
                    b.classList.toggle('active', b === btn);
                    b.setAttribute('aria-selected', b === btn ? 'true' : 'false');
                });
                document.querySelectorAll('.admin-tab-panel').forEach(p => {
                    p.hidden = p.id !== `tab-${tab}`;
                });
                ensureTabData(tab);
            });
        });

        el('btn-add-event').addEventListener('click', () => openEventModal(false));
        el('btn-refresh-users').addEventListener('click', loadUsers);
        el('btn-refresh-bookings').addEventListener('click', loadBookings);
        el('btn-start-scanner')?.addEventListener('click', startQrScanner);
        el('btn-stop-scanner')?.addEventListener('click', stopQrScanner);
        el('btn-manual-scan')?.addEventListener('click', async () => {
            const raw = el('scanner-manual-token').value.trim();
            if (!raw) {
                toast('Enter token first', 'error');
                return;
            }
            await submitAttendanceToken(raw);
        });

        el('scanner-file-input')?.addEventListener('change', async (e) => {
            if (e.target.files.length === 0) return;
            const file = e.target.files[0];
            
            let scannerInstance = html5QrCode;
            let destroyAfter = false;
            if (!scannerInstance) {
                scannerInstance = new Html5Qrcode("scanner-reader");
                destroyAfter = true;
            }
            
            const status = el('scanner-status');
            if (status) status.textContent = 'Scanning file...';
            
            try {
                const decodedText = await scannerInstance.scanFile(file, true);
                if (status) status.textContent = 'File scan success!';
                await submitAttendanceToken(decodedText);
            } catch (err) {
                console.error("File scanning error", err);
                toast("Failed to scan QR from image file. Check image clarity.", "error");
                if (status) status.textContent = 'File scan failed.';
            } finally {
                if (destroyAfter) {
                    try {
                        scannerInstance.clear();
                    } catch (clearErr) {
                        console.error("Error clearing scanner instance", clearErr);
                    }
                }
            }
        });

        el('event-form').addEventListener('submit', saveEvent);

        document.querySelectorAll('[data-close-modal]').forEach(b => {
            b.addEventListener('click', closeEventModal);
        });
        el('event-modal').addEventListener('click', e => {
            if (e.target === el('event-modal')) closeEventModal();
        });

        document.querySelectorAll('[data-close-user-modal]').forEach(b => {
            b.addEventListener('click', closeUserModal);
        });
        el('user-modal').addEventListener('click', e => {
            if (e.target === el('user-modal')) closeUserModal();
        });

        el('user-form').addEventListener('submit', saveUser);

        el('events-table-body').addEventListener('click', e => {
            const editBtn = e.target.closest('[data-action="edit-event"]');
            const delBtn = e.target.closest('[data-action="delete-event"]');
            if (editBtn) {
                const id = editBtn.getAttribute('data-id');
                const ev = (window.__adminEvents || []).find(x => String(eventId(x)) === String(id));
                if (ev) openEventModal(true, ev);
            }
            if (delBtn) deleteEventById(delBtn.getAttribute('data-id'));
        });

        el('users-table-body').addEventListener('click', e => {
            const editBtn = e.target.closest('[data-action="edit-user"]');
            const delBtn = e.target.closest('[data-action="delete-user"]');
            const copyBtn = e.target.closest('[data-action="copy-profile-link"]');
            if (editBtn) {
                const id = editBtn.getAttribute('data-id');
                const u = (window.__adminUsers || []).find(x => String(x.id) === String(id));
                if (u) openUserModal(u);
            }
            if (delBtn) deleteUserById(delBtn.getAttribute('data-id'));
            if (copyBtn) {
                const id = copyBtn.getAttribute('data-id');
                const url = profileUrlForUserId(id);
                navigator.clipboard
                    ?.writeText(url)
                    .then(() => toast('Profile link copied'))
                    .catch(() => toast('Could not copy link (browser blocked)', 'error'));
            }
        });

        el('bookings-table-body').addEventListener('click', async e => {
            const cancelBtn = e.target.closest('[data-action="cancel-booking"]');
            if (cancelBtn) {
                const id = cancelBtn.getAttribute('data-id');
                if (!confirm('Cancel this booking? The student will be notified via WhatsApp.')) return;
                try {
                    const res = await fetch(`${apiBase()}/api/events/bookings/${id}`, { method: 'DELETE' });
                    if (!res.ok) throw new Error('Cancellation failed');
                    toast('Booking cancelled');
                    await loadBookings();
                } catch {
                    toast('Could not cancel booking', 'error');
                }
            }
        });
    });
})();
