/* The prototype keeps entered values in the current page only. */
(() => {
    'use strict';
    const form = document.getElementById('cake-booking-form');
    // Details-page cancellation shows a message only; confirmation sends no request.
    const cancelButton = document.getElementById('cancel-preview');
    if (cancelButton) {
        cancelButton.disabled = false;
        cancelButton.addEventListener('click', () => {
            if (!window.confirm('Demonstrate cancellation? This will only show a message; nothing will be saved.')) return;
            const message = document.getElementById('cancel-message');
            message.textContent = 'Cancellation preview only — no booking has been cancelled and no changes have been saved.';
            message.hidden = false;
        });
    }

    // Other preview pages share this script but do not contain the request form.
    if (!form) return;
    const button = document.getElementById('preview-request');
    const design = document.getElementById('design-description');
    const details = document.getElementById('preview-details');

    // Block implicit submission as well as clicks; there is no POST or network call.
    form.addEventListener('submit', event => event.preventDefault());
    document.getElementById('booking-fields').disabled = false;

    // Reject descriptions containing only whitespace while retaining browser validation.
    design.addEventListener('input', () => design.setCustomValidity(''));
    button.addEventListener('click', () => {
        design.setCustomValidity(design.value.trim() ? '' : 'Please describe your cake design.');
        if (!form.reportValidity()) return;

        const fields = [
            ['Cake type', 'cake-type'],
            ['Cake size', 'cake-size'],
            ['Flavour', 'flavour'],
            ['Design description', 'design-description'],
            ['Cake message', 'cake-message'],
            ['Required date', 'required-date'],
            ['Reference image URL', 'reference-image-url']
        ];

        // textContent prevents entered markup from becoming executable HTML.
        details.replaceChildren();
        fields.forEach(([label, id]) => {
            const term = document.createElement('dt');
            const value = document.createElement('dd');
            term.textContent = label;
            value.textContent = document.getElementById(id).value.trim() || 'Not provided';
            details.append(term, value);
        });
        document.getElementById('preview-placeholder').hidden = true;
        document.getElementById('preview-content').hidden = false;
        document.getElementById('preview-title').focus();
    });

    // Changes invalidate the previous preview so it cannot be mistaken for current input.
    form.addEventListener('input', () => {
        document.getElementById('preview-placeholder').hidden = false;
        document.getElementById('preview-content').hidden = true;
    });
})();
