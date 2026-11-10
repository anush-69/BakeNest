import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import { ArrowLeft, ArrowRight, ShoppingBag, CreditCard, Truck } from "lucide-react";
import { useApp } from "../../context/AppContext";
import { useResource } from "../../hooks/useResource";
import { productApi } from "../../api/productApi";
import { orderApi } from "../../api/orderApi";
import { money } from "../../lib/format";
import {
  Button,
  ButtonLink,
  PageHeading,
  Loading,
  ErrorState,
  Empty,
  Field,
  InlineError,
  Modal,
} from "../../components/common/UI";

// Delivery cost by city keyword (case-insensitive match)
const DELIVERY_RATES = [
  { keywords: ["colombo"], cost: 250 },
  { keywords: ["gampaha", "kalutara", "negombo", "kelaniya"], cost: 350 },
  { keywords: ["kandy", "galle", "matara", "kurunegala"], cost: 500 },
];
const DEFAULT_DELIVERY_COST = 650;

function getDeliveryCost(city) {
  if (!city.trim()) return 0;
  const lower = city.toLowerCase();
  for (const rate of DELIVERY_RATES) {
    if (rate.keywords.some((k) => lower.includes(k))) return rate.cost;
  }
  return DEFAULT_DELIVERY_COST;
}

export default function Checkout() {
  const { basket, setBasket, user, notify } = useApp();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    customerName: user ? `${user.firstName || ""} ${user.lastName || ""}`.trim() : "",
    customerPhone: user?.phone || "",
    deliveryAddress: "",
    deliveryCity: "",
    paymentMethod: "CASH_ON_DELIVERY",
    customerNote: "",
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [confirmation, setConfirmation] = useState(null);

  // Redirect away if basket is empty
  useEffect(() => {
    if (!basket.length) navigate("/cart", { replace: true });
  }, [basket.length, navigate]);

  const ids = basket.map((item) => item.productId).join(",");
  const products = useResource(async () => {
    return Promise.all(
      basket.map(async (item) => {
        try {
          return { productId: item.productId, product: await productApi.get(item.productId) };
        } catch (e) {
          if (e.status === 404) return { productId: item.productId, missing: true };
          throw e;
        }
      }),
    );
  }, [ids]);

  const items = basket.map((item) => ({
    ...item,
    ...products.data?.find((p) => p.productId === item.productId),
  }));

  const subtotal = items.reduce(
    (sum, item) => sum + Number(item.product?.price || 0) * item.quantity,
    0,
  );
  const deliveryCost = getDeliveryCost(form.deliveryCity);
  const total = subtotal + deliveryCost;

  function set(field) {
    return (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }));
  }

  async function placeOrder(e) {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      const order = await orderApi.create({
        items: basket.map(({ productId, quantity }) => ({ productId, quantity })),
        customerName: form.customerName,
        customerPhone: form.customerPhone,
        deliveryAddress: form.deliveryAddress,
        deliveryCity: form.deliveryCity,
        paymentMethod: form.paymentMethod,
        deliveryCost: deliveryCost,
        customerNote: form.customerNote || null,
      });
      setBasket([]);
      setConfirmation(order);
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  if (products.loading) return <Loading />;
  if (products.error) return <ErrorState error={products.error} retry={products.reload} />;

  return (
    <div className="container page">
      <Link className="back-link" to="/cart">
        <ArrowLeft size={16} />
        Back to cart
      </Link>
      <PageHeading
        eyebrow="ALMOST THERE"
        title={<>Your delivery <em>details.</em></>}
        text="Let us know where to bring your order."
      />

      <div className="cart-layout">
        {/* LEFT — form */}
        <form className="checkout-form" onSubmit={placeOrder}>
          <div className="panel">
            <p className="eyebrow">CONTACT DETAILS</p>
            <h3 style={{ marginBottom: "1.25rem" }}>Who's ordering?</h3>
            <Field
              label="Full name"
              required
              value={form.customerName}
              onChange={set("customerName")}
              maxLength={200}
              disabled={busy}
              placeholder="e.g. Sahanya Perera"
            />
            <Field
              label="Phone number"
              required
              type="tel"
              value={form.customerPhone}
              onChange={set("customerPhone")}
              maxLength={20}
              disabled={busy}
              placeholder="e.g. 077 123 4567"
            />
          </div>

          <div className="panel" style={{ marginTop: "1.5rem" }}>
            <p className="eyebrow">DELIVERY</p>
            <h3 style={{ marginBottom: "1.25rem" }}>Where should we deliver?</h3>
            <Field
              label="Street address"
              required
              value={form.deliveryAddress}
              onChange={set("deliveryAddress")}
              maxLength={500}
              disabled={busy}
              placeholder="e.g. 123 Galle Road"
            />
            <Field
              label="City"
              required
              value={form.deliveryCity}
              onChange={set("deliveryCity")}
              maxLength={100}
              disabled={busy}
              placeholder="e.g. Colombo"
              hint={
                form.deliveryCity.trim()
                  ? `Delivery cost: ${money(deliveryCost)}`
                  : "Enter your city to see delivery cost"
              }
            />
          </div>

          <div className="panel" style={{ marginTop: "1.5rem" }}>
            <p className="eyebrow">PAYMENT</p>
            <h3 style={{ marginBottom: "1.25rem" }}>How will you pay?</h3>
            <div className="payment-options">
              <label className={`payment-option ${form.paymentMethod === "CASH_ON_DELIVERY" ? "selected" : ""}`}>
                <input
                  type="radio"
                  name="paymentMethod"
                  value="CASH_ON_DELIVERY"
                  checked={form.paymentMethod === "CASH_ON_DELIVERY"}
                  onChange={set("paymentMethod")}
                  disabled={busy}
                />
                <Truck size={22} />
                <span>
                  <strong>Cash on delivery</strong>
                  <small>Pay when your order arrives</small>
                </span>
              </label>
              <label className={`payment-option ${form.paymentMethod === "CARD" ? "selected" : ""}`}>
                <input
                  type="radio"
                  name="paymentMethod"
                  value="CARD"
                  checked={form.paymentMethod === "CARD"}
                  onChange={set("paymentMethod")}
                  disabled={busy}
                />
                <CreditCard size={22} />
                <span>
                  <strong>Card payment</strong>
                  <small>Pay by card at collection</small>
                </span>
              </label>
            </div>
          </div>

          <div className="panel" style={{ marginTop: "1.5rem" }}>
            <Field
              label="A note for the bakery"
              as="textarea"
              value={form.customerNote}
              onChange={set("customerNote")}
              maxLength={1000}
              rows={3}
              disabled={busy}
              placeholder="Allergies, special requests, delivery instructions…"
            />
          </div>

          <InlineError error={error} />

          <Button type="submit" className="full-width" busy={busy} style={{ marginTop: "1rem" }}>
            Place my order
            <ArrowRight size={17} />
          </Button>
        </form>

        {/* RIGHT — order summary */}
        <div className="order-summary panel">
          <p className="eyebrow">YOUR ORDER</p>
          <h2>Summary</h2>
          {items.map((item) => (
            <div key={item.productId} className="summary-row">
              <span>
                {item.product?.name || `Product #${item.productId}`}
                <small className="muted"> ×{item.quantity}</small>
              </span>
              <span>
                {item.product
                  ? money(Number(item.product.price) * item.quantity)
                  : "—"}
              </span>
            </div>
          ))}
          <hr style={{ border: "none", borderTop: "1px solid var(--line)", margin: "0.75rem 0" }} />
          <div className="summary-row">
            <span>Subtotal</span>
            <span>{money(subtotal)}</span>
          </div>
          <div className="summary-row">
            <span>Delivery</span>
            <span>
              {form.deliveryCity.trim()
                ? money(deliveryCost)
                : <small className="muted">Enter city</small>}
            </span>
          </div>
          <div className="summary-row total">
            <strong>Total</strong>
            <strong>{form.deliveryCity.trim() ? money(total) : "—"}</strong>
          </div>
          <ButtonLink variant="text secondary" to="/cart" style={{ marginTop: "0.5rem" }}>
            <ArrowLeft size={15} />
            Edit cart
          </ButtonLink>
        </div>
      </div>

      {confirmation && (
        <Modal
          title="Your order is placed!"
          onClose={() => navigate(`/account/orders/${confirmation.id}`)}
        >
          <div className="confirmation-icon">
            <ShoppingBag size={32} />
          </div>
          <p>
            Order <strong>#{String(confirmation.id).padStart(4, "0")}</strong> is confirmed.
          </p>
          <div className="summary-row">
            <span>Subtotal</span>
            <span>{money(confirmation.total - (confirmation.deliveryCost || 0))}</span>
          </div>
          <div className="summary-row">
            <span>Delivery</span>
            <span>{money(confirmation.deliveryCost || 0)}</span>
          </div>
          <div className="summary-row total">
            <strong>Total paid</strong>
            <strong>{money(confirmation.total)}</strong>
          </div>
          <p className="muted">
            Payment: {confirmation.paymentMethod === "CARD" ? "Card at collection" : "Cash on delivery"}.
          </p>
          <ButtonLink to={`/account/orders/${confirmation.id}`}>
            View my order
            <ArrowRight size={17} />
          </ButtonLink>
        </Modal>
      )}
    </div>
  );
}

