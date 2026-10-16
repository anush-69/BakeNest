// Show card demo fields only when the customer selects card payment.
const paymentOptions = document.querySelectorAll('input[name="paymentMethod"]');
const cardDetails = document.getElementById('card-details');

function showPaymentFields() {
    const selected = document.querySelector('input[name="paymentMethod"]:checked');
    const useCard = selected && selected.value === 'CARD_DEMO';
    cardDetails.hidden = !useCard;
    cardDetails.disabled = !useCard;
}

paymentOptions.forEach(function (option) {
    option.addEventListener('change', showPaymentFields);
});
showPaymentFields();

// Use cents for the screen preview. Java calculates the authoritative total.
const quantities = document.querySelectorAll('.quantity');
function updateTotal() {
    let totalCents = 0;
    quantities.forEach(function (input) {
        const quantity = Math.max(0, Math.min(99, Math.floor(Number(input.value) || 0)));
        const priceCents = Math.round(Number(input.dataset.price) * 100);
        const subtotalCents = quantity * priceCents;
        totalCents += subtotalCents;
        input.closest('.product').querySelector('.line-total').textContent =
            'LKR ' + (subtotalCents / 100).toFixed(2);
    });
    document.getElementById('order-total').textContent = 'LKR ' + (totalCents / 100).toFixed(2);
}
quantities.forEach(function (input) {
    input.addEventListener('input', updateTotal);
});
updateTotal();
