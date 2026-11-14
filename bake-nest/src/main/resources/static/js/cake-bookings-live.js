/* Customer pages only; the prototype never loads or initializes this workflow. */
(() => {
    'use strict';
    const root = document.querySelector('main[data-live-page]');
    if (!root) return;
    const apiBase = root.dataset.api;
    const pageBase = root.dataset.base;
    const mode = root.dataset.livePage;
    const message = document.getElementById('live-message');
    const loginLink = document.getElementById('live-login');
    const retry = document.getElementById('live-retry');
    const fields = ['cakeType', 'cakeSize', 'flavour', 'requiredDate',
        'designDescription', 'cakeMessage', 'referenceImageUrl'];
    let pending = false;

    function feedback(text, error = false) {
        message.textContent = text;
        message.setAttribute('role', error ? 'alert' : 'status');
        retry.hidden = !error;
    }
    function failure(error) {
        feedback(error.message || 'The request could not be completed. Reload and try again.', true);
        loginLink.hidden = !error.login;
    }
    retry.addEventListener('click', () => window.location.reload());
    window.addEventListener('pageshow', event => { if (event.persisted) window.location.reload(); });

    async function api(path, method = 'GET', body) {
        const headers = {Accept: 'application/json'};
        if (method !== 'GET') {
            const token = document.querySelector('meta[name="_csrf"]')?.content;
            const header = document.querySelector('meta[name="_csrf_header"]')?.content;
            if (!token || !header) throw new Error('Security token is unavailable. Reload this page.');
            headers[header] = token;
        }
        if (body !== undefined) headers['Content-Type'] = 'application/json';
        let response;
        try {
            response = await fetch(apiBase + path, {method, headers, credentials: 'same-origin',
                body: body === undefined ? undefined : JSON.stringify(body)});
        } catch (_) {
            throw new Error('Connection lost. Your request may not have completed. Reload to check your bookings before retrying.');
        }
        const loginPath = new URL(loginLink.href, window.location.origin).pathname;
        if (response.redirected || new URL(response.url, window.location.origin).pathname === loginPath || response.status === 401) {
            const error = new Error('Your session has ended. Sign in again to continue.');
            error.login = true;
            throw error;
        }
        // Filter-level CSRF failures may be HTML instead of the API's JSON error DTO.
        if (response.status === 403) {
            throw new Error('Access denied or your security token has expired. Reload this page; sign in again if needed.');
        }
        const contentType = response.headers.get('Content-Type') || '';
        if (!contentType.includes('application/json') && !contentType.includes('+json')) {
            throw new Error('The server returned an unexpected response. Reload this page before retrying.');
        }
        let data;
        try { data = await response.json(); }
        catch (_) { throw new Error('The server response could not be read. Reload this page.'); }
        if (!response.ok) {
            const errors = {
                400: data.message || 'Check the booking details and try again.',
                403: 'Access denied or your security token has expired. Reload this page; sign in again if needed.',
                404: 'This booking is unavailable for your account.',
                409: data.message || 'This booking can no longer be changed. Reload to see its current status.'
            };
            throw new Error(errors[response.status] || 'The server could not complete this request. Try again later.');
        }
        return data;
    }

    function detailUrl(id) { return pageBase + '/' + encodeURIComponent(String(id)); }
    function textElement(tag, text, className) {
        const element = document.createElement(tag);
        element.textContent = text;
        if (className) element.className = className;
        return element;
    }
    function addDetail(list, label, value, reference = false) {
        const group = document.createElement('div');
        const description = textElement('dd', value == null || value === '' ? 'Not provided' : String(value));
        if (reference && value) {
            try {
                const url = new URL(value);
                if (url.protocol === 'https:' || url.protocol === 'http:') {
                    const link = textElement('a', value);
                    link.href = url.href;
                    link.target = '_blank';
                    link.rel = 'noopener noreferrer';
                    link.referrerPolicy = 'no-referrer';
                    description.replaceChildren(link);
                }
            } catch (_) { /* Invalid reference URLs remain plain text. */ }
        }
        group.append(textElement('dt', label), description);
        list.append(group);
    }

    async function listBookings() {
        const previous = document.getElementById('live-previous');
        const next = document.getElementById('live-next');
        const sizeInput = document.getElementById('live-size');
        const params = new URLSearchParams(window.location.search);
        let page = Number(params.get('page') ?? 0);
        let size = Number(params.get('size') ?? 10);
        if (!Number.isSafeInteger(page) || page < 0 || !Number.isInteger(size) || size < 1 || size > 50) {
            throw new Error('Invalid page selection. Open My bookings to start again.');
        }
        if (![10, 25, 50].includes(size)) sizeInput.add(new Option(String(size), String(size)));
        sizeInput.value = String(size);
        async function load(target) {
            if (pending) return;
            pending = true;
            previous.disabled = next.disabled = sizeInput.disabled = true;
            feedback('Loading your bookings?');
            try {
                const data = await api('?page=' + target + '&size=' + size);
                page = data.page;
                const list = document.getElementById('live-list');
                list.replaceChildren();
                document.getElementById('live-empty').hidden = data.items.length !== 0;
                for (const booking of data.items) {
                    const card = document.createElement('article');
                    card.className = 'card booking-card';
                    card.append(textElement('h2', booking.cakeType || 'Cake request'),
                        textElement('span', booking.status, 'status-badge'));
                    const details = document.createElement('dl');
                    addDetail(details, 'Booking', '#' + booking.id);
                    addDetail(details, 'Required date', booking.requiredDate);
                    card.append(details);
                    const view = textElement('a', 'View booking');
                    view.href = detailUrl(booking.id);
                    card.append(view);
                    list.append(card);
                }
                document.getElementById('live-page-summary').textContent = data.totalPages === 0
                    ? '0 bookings' : `Page ${page + 1} of ${data.totalPages} ? ${data.totalElements} bookings`;
                previous.disabled = page === 0;
                next.disabled = page + 1 >= data.totalPages;
                const url = new URL(window.location.href);
                url.searchParams.set('page', String(page));
                url.searchParams.set('size', String(size));
                window.history.replaceState(null, '', url);
                feedback(data.items.length ? 'Your bookings are up to date.' : 'No bookings on this page.');
            } catch (error) { failure(error); }
            finally { pending = false; sizeInput.disabled = false; }
        }
        previous.addEventListener('click', () => load(page - 1));
        next.addEventListener('click', () => load(page + 1));
        sizeInput.addEventListener('change', () => { size = Number(sizeInput.value); load(0); });
        await load(page);
    }

    function renderDetails(booking) {
        const list = document.getElementById('live-details');
        list.replaceChildren();
        for (const [label, value] of [['Booking', '#' + booking.id], ['Status', booking.status],
            ['Cake type', booking.cakeType], ['Cake size', booking.cakeSize], ['Flavour', booking.flavour],
            ['Required date', booking.requiredDate], ['Design description', booking.designDescription],
            ['Cake message', booking.cakeMessage],
            ['Estimated price', booking.estimatedPrice == null ? 'Not yet provided' : booking.estimatedPrice],
            ['Quoted price', booking.quotedPrice == null ? 'Not yet provided' : booking.quotedPrice]]) {
            addDetail(list, label, value);
        }
        addDetail(list, 'Reference image URL', booking.referenceImageUrl, true);
        document.getElementById('live-details-card').hidden = false;
        const edit = document.getElementById('live-edit');
        edit.href = detailUrl(booking.id) + '/edit';
        edit.hidden = booking.status !== 'REQUESTED';
        document.getElementById('live-cancel').hidden = booking.status !== 'REQUESTED';
    }

    async function showDetails() {
        const id = root.dataset.bookingId;
        renderDetails(await api('/' + encodeURIComponent(id)));
        feedback('Booking loaded.');
        const cancel = document.getElementById('live-cancel');
        cancel.addEventListener('click', async () => {
            if (pending || !window.confirm('Cancel this booking? Its details will remain in your booking history.')) return;
            pending = true;
            cancel.disabled = true;
            feedback('Cancelling your booking?');
            try {
                renderDetails(await api('/' + encodeURIComponent(id) + '/cancel', 'PATCH'));
                feedback('Booking cancelled. Your details remain available here.');
            } catch (error) { failure(error); }
            finally { pending = false; cancel.disabled = false; }
        });
    }

    async function bookingForm() {
        const form = document.getElementById('live-form');
        const fieldset = document.getElementById('live-fields');
        const editing = mode === 'edit';
        const id = root.dataset.bookingId;
        let completed = false;
        form.addEventListener('submit', async event => {
            event.preventDefault();
            if (pending || completed || fieldset.disabled || !form.reportValidity()) return;
            const request = {};
            for (const field of fields) request[field] = document.getElementById('live-' + field).value.trim() || null;
            pending = true;
            fieldset.disabled = true;
            feedback(editing ? 'Saving changes?' : 'Creating your booking?');
            try {
                const saved = await api(editing ? '/' + encodeURIComponent(id) : '', editing ? 'PUT' : 'POST', request);
                completed = true;
                feedback(editing ? 'Changes saved.' : 'Booking created.');
                const link = document.getElementById('live-saved-link');
                link.href = detailUrl(saved.id);
                link.hidden = false;
                link.focus();
            } catch (error) { failure(error); }
            finally { pending = false; fieldset.disabled = completed; }
        });
        if (editing) {
            const booking = await api('/' + encodeURIComponent(id));
            if (booking.status !== 'REQUESTED') throw new Error('Only requested bookings can be edited. View this booking for its current details.');
            for (const field of fields) document.getElementById('live-' + field).value = booking[field] ?? '';
        }
        fieldset.disabled = false;
        feedback(editing ? 'Booking loaded. Update your request below.' : 'Ready for your cake request. Required date is mandatory.');
    }

    const start = mode === 'list' ? listBookings : mode === 'details' ? showDetails : bookingForm;
    start().catch(failure);
})();
