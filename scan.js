/* QR Pass Scanner — talks to /api/qr-pass on the same origin. */
(function () {
    'use strict';

    const API_BASE = window.location.origin;
    const VALIDATE_URL = `${API_BASE}/api/qr-pass/validate`;
    const ISSUE_URL = `${API_BASE}/api/qr-pass/issue`;

    // Per-status presentation for the result screen.
    const STATUS_UI = {
        VALID:        { cls: 'valid',   icon: '✓', title: 'Valid' },
        INVALID:      { cls: 'invalid', icon: '✗', title: 'Invalid' },
        EXPIRED:      { cls: 'expired', icon: '⏰', title: 'Expired' },
        ALREADY_USED: { cls: 'used',    icon: '↻', title: 'Already used' },
        REVOKED:      { cls: 'revoked', icon: '⛔', title: 'Revoked' },
        ERROR:        { cls: 'invalid', icon: '!', title: 'Error' }
    };

    let html5Qr = null;
    let scanning = false;
    let processing = false;
    let lastText = null;
    let lastAt = 0;

    const $ = (id) => document.getElementById(id);

    function showSection(name) {
        ['scanner-section', 'loading-section', 'result-section', 'permission-section']
            .forEach(id => { const el = $(id); if (el) el.hidden = (id !== name); });
    }

    // ---------------------------------------------------------------- camera

    async function startCamera() {
        if (scanning) return;
        showSection('scanner-section');
        $('scan-status').textContent = 'Starting camera…';
        $('btn-start').hidden = true;

        try {
            html5Qr = new Html5Qrcode('reader');
            const config = { fps: 10, qrbox: { width: 250, height: 250 } };
            const onSuccess = (decodedText) => onScan(decodedText);
            const onError = () => {}; // per-frame decode misses are normal; ignore

            try {
                await html5Qr.start({ facingMode: 'environment' }, config, onSuccess, onError);
            } catch (envErr) {
                // Fall back to the front camera (e.g. laptops).
                await html5Qr.start({ facingMode: 'user' }, config, onSuccess, onError);
            }
            scanning = true;
            $('btn-stop').hidden = false;
            $('scan-status').textContent = 'Point the camera at a QR code.';
        } catch (err) {
            console.error('Camera error', err);
            $('btn-start').hidden = false;
            showPermissionScreen(err);
        }
    }

    async function stopCamera() {
        if (html5Qr && scanning) {
            try { await html5Qr.stop(); } catch (e) { /* ignore */ }
        }
        scanning = false;
        $('btn-stop').hidden = true;
        $('btn-start').hidden = false;
    }

    function showPermissionScreen(err) {
        const msg = (err && /permission|denied|notallowed/i.test(String(err.name || err.message || err)))
            ? 'Camera permission was blocked. Allow it in your browser settings, or use a fallback below.'
            : 'No camera was found or it could not be started. Use a fallback below.';
        $('permission-message').textContent = msg;
        showSection('permission-section');
    }

    // ---------------------------------------------------------------- scan -> validate

    async function onScan(decodedText) {
        if (processing) return;
        const now = Date.now();
        if (decodedText === lastText && now - lastAt < 4000) return; // debounce
        lastText = decodedText;
        lastAt = now;

        processing = true;
        await stopCamera();          // stop after a successful read
        await validate(decodedText);
        processing = false;
    }

    async function validate(payload) {
        showSection('loading-section');
        beep();
        try {
            const res = await fetch(VALIDATE_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ payload: payload, scannedBy: scannerName() })
            });
            if (res.status === 429) {
                renderResult({ status: 'ERROR', message: 'Too many scans — slow down and try again.' });
                return;
            }
            const data = await res.json();
            renderResult(data);
        } catch (err) {
            console.error('Validate failed', err);
            renderResult({ status: 'ERROR', message: 'Network error contacting the server.' });
        }
    }

    function renderResult(data) {
        const ui = STATUS_UI[data.status] || STATUS_UI.ERROR;
        const icon = $('result-icon');
        icon.textContent = ui.icon;
        icon.className = 'result-icon ' + ui.cls;
        $('result-title').textContent = ui.title;
        $('result-title').className = 'result-title-' + ui.cls;
        $('result-message').textContent = data.message || '';

        const dl = $('result-details');
        dl.innerHTML = '';
        addDetail(dl, 'Name', data.name);
        addDetail(dl, 'Subject', joinSubject(data.subjectType, data.subjectId));
        addDetail(dl, 'Email', data.email);
        if (data.expiresAt) addDetail(dl, 'Expires', formatTime(data.expiresAt));
        if (data.usedAt && data.status !== 'VALID') addDetail(dl, 'Used at', formatTime(data.usedAt));

        showSection('result-section');
        vibrate(data.status === 'VALID' ? [120] : [80, 60, 80]);
    }

    function addDetail(dl, label, value) {
        if (!value) return;
        const dt = document.createElement('dt'); dt.textContent = label;
        const dd = document.createElement('dd'); dd.textContent = value;
        dl.appendChild(dt); dl.appendChild(dd);
    }

    // ---------------------------------------------------------------- fallbacks

    async function scanFile(file) {
        if (!file) return;
        showSection('loading-section');
        const instance = new Html5Qrcode('reader');
        try {
            const text = await instance.scanFile(file, true);
            await instance.clear();
            await validate(text);
        } catch (err) {
            try { await instance.clear(); } catch (e) {}
            renderResult({ status: 'ERROR', message: 'Could not read a QR code from that image.' });
        }
    }

    async function issueTestPass() {
        const body = {
            name: $('issue-name').value.trim() || null,
            subjectType: $('issue-type').value.trim() || 'ACCESS',
            ttlSeconds: $('issue-ttl').value ? Number($('issue-ttl').value) : null
        };
        const btn = $('btn-issue');
        btn.disabled = true; btn.textContent = 'Issuing…';
        try {
            const res = await fetch(ISSUE_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            const data = await res.json();
            $('issue-qr').src = data.qrImageDataUrl;
            $('issue-result').hidden = false;
        } catch (err) {
            alert('Could not issue a pass (is the backend running?).');
        } finally {
            btn.disabled = false; btn.textContent = 'Issue & show QR';
        }
    }

    // ---------------------------------------------------------------- helpers

    function joinSubject(type, id) {
        if (type && id) return `${type} #${id}`;
        return type || id || '';
    }
    function formatTime(iso) {
        try { return new Date(iso).toLocaleString(); } catch (e) { return iso; }
    }
    function scannerName() {
        try {
            const u = JSON.parse(localStorage.getItem('currentUser') || '{}');
            return u.name || u.email || 'Scanner';
        } catch (e) { return 'Scanner'; }
    }
    function beep() {
        try {
            const ctx = new (window.AudioContext || window.webkitAudioContext)();
            const o = ctx.createOscillator(); const g = ctx.createGain();
            o.connect(g); g.connect(ctx.destination);
            o.frequency.value = 880; g.gain.value = 0.05;
            o.start(); o.stop(ctx.currentTime + 0.12);
        } catch (e) { /* audio not allowed; ignore */ }
    }
    function vibrate(pattern) {
        if (navigator.vibrate) { try { navigator.vibrate(pattern); } catch (e) {} }
    }

    // ---------------------------------------------------------------- wiring

    document.addEventListener('DOMContentLoaded', () => {
        $('btn-start').addEventListener('click', startCamera);
        $('btn-stop').addEventListener('click', stopCamera);
        $('btn-retry-camera').addEventListener('click', startCamera);
        $('btn-scan-next').addEventListener('click', startCamera);

        $('file-input').addEventListener('change', (e) => scanFile(e.target.files[0]));
        $('file-input-2').addEventListener('change', (e) => scanFile(e.target.files[0]));
        $('btn-manual').addEventListener('click', () => {
            const v = $('manual-input').value.trim();
            if (v) validate(v);
        });
        $('btn-manual-2').addEventListener('click', () => {
            const v = $('manual-input-2').value.trim();
            if (v) validate(v);
        });
        $('btn-issue').addEventListener('click', issueTestPass);
    });
})();
