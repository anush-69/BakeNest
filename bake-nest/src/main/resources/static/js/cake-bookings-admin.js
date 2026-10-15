/* Admin interactions use rendered fictional rows only; no storage or network access. */
(() => {
    'use strict';
    const search = document.getElementById('booking-search');
    const filter = document.getElementById('status-filter');
    const rows = Array.from(document.querySelectorAll('[data-booking-row]'));
    const form = document.getElementById('admin-review-form');
    const booking = document.getElementById('review-booking');
    const status = document.getElementById('proposed-status');
    const quote = document.getElementById('quote-amount');
    const note = document.getElementById('admin-note');
    const message = document.getElementById('admin-preview-message');

    // Only enable controls once their browser-only handlers are available.
    search.disabled = false;
    filter.disabled = false;
    document.getElementById('admin-review-fields').disabled = false;
    form.addEventListener('submit', event => event.preventDefault());

    // Search and status conditions combine; no-match feedback is announced accessibly.
    const filterRows = () => {
        const query = search.value.trim().toLowerCase().replace(/^#/, '');
        let count = 0;
        rows.forEach(row => {
            const matchesSearch = row.dataset.id.includes(query) || row.dataset.cakeType.toLowerCase().includes(query);
            const matchesStatus = !filter.value || row.dataset.status === filter.value;
            row.hidden = !(matchesSearch && matchesStatus);
            if (!row.hidden) count++;
        });
        document.getElementById('result-count').textContent = `${count} fictional booking${count === 1 ? '' : 's'} shown.`;
        document.getElementById('no-results').hidden = count !== 0;
    };
    search.addEventListener('input', filterRows);
    filter.addEventListener('change', filterRows);
    filterRows();

    // Changing the review target clears unsaved proposals to avoid applying them to another example.
    const selectBooking = () => {
        status.value = booking.selectedOptions[0].dataset.status || '';
        quote.value = '';
        quote.setCustomValidity('');
        note.value = '';
        message.hidden = true;
    };
    booking.addEventListener('change', selectBooking);
    document.querySelectorAll('[data-review-booking]').forEach(button => {
        button.disabled = false;
        button.addEventListener('click', () => {
            booking.value = button.dataset.id;
            selectBooking();
            document.getElementById('review-title').focus();
        });
    });

    // Validate decimal notation directly: reject negatives, exponent notation, and nonnumeric text.
    quote.addEventListener('input', () => quote.setCustomValidity(''));
    form.addEventListener('input', () => { message.hidden = true; });
    document.getElementById('update-preview').addEventListener('click', () => {
        message.hidden = true;
        const amount = quote.value;
        quote.setCustomValidity(amount === '' || /^[0-9]+(?:\.[0-9]+)?$/.test(amount)
            ? '' : 'Enter a nonnegative decimal without currency symbols, spaces, or commas.');
        if (!form.reportValidity()) return;
        // The result is a message only: fixtures, table values, and details stay unchanged.
        message.textContent = 'Preview only — no booking changes have been saved.';
        message.hidden = false;
    });
})();
