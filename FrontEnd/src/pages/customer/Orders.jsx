import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, ArrowUpRight, ShoppingBag, Check, Pencil, Trash2 } from "lucide-react";
import { orderApi } from "../../api/orderApi";
import { useResource } from "../../hooks/useResource";
import { useApp } from "../../context/AppContext";
import { date, money, canCancel, nextStatus } from "../../lib/format";
import {
  PageHeading,
  Loading,
  ErrorState,
  Empty,
  Badge,
  ButtonLink,
  Button,
  Confirm,
  Quantity,
  InlineError,
  Field,
  Modal,
} from "../../components/common/UI";

// Delivery rates (must match Checkout.jsx)
const DELIVERY_RATES = [
  { keywords: ["colombo"], cost: 250 },
  { keywords: ["gampaha", "kalutara", "negombo", "kelaniya"], cost: 350 },
  { keywords: ["kandy", "galle", "matara", "kurunegala"], cost: 500 },
];
const DEFAULT_DELIVERY_COST = 650;
function getDeliveryCost(city) {
  if (!city?.trim()) return 0;
  const lower = city.toLowerCase();
  for (const rate of DELIVERY_RATES) {
    if (rate.keywords.some((k) => lower.includes(k))) return rate.cost;
  }
  return DEFAULT_DELIVERY_COST;
}

export function Orders({ admin = false }) {
  const orders = useResource(
    () => (admin ? orderApi.list() : orderApi.mine()),
    [admin],
  );
  const [filter, setFilter] = useState("ALL");
  const [search, setSearch] = useState("");
  const rows = (orders.data || [])
    .filter(
      (order) =>
        (filter === "ALL" || order.status === filter) &&
        String(order.id).includes(search.trim()),
    )
    .sort((a, b) => b.id - a.id);
  return (
    <>
      <PageHeading
        eyebrow={admin ? "BAKERY STUDIO" : "YOUR SWEET MOMENTS"}
        title={admin ? "Orders" : "Your order history"}
        text={
          admin
            ? "Every order, from the first click to the final crumb."
            : "A little look at the good things you've chosen."
        }
      />
      <div className="table-toolbar">
        <input
          aria-label="Search by order number"
          placeholder="Search order number…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select
          aria-label="Filter by order status"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        >
          {[
            "ALL",
            "PENDING",
            "CONFIRMED",
            "PREPARING",
            "READY",
            "COMPLETED",
            "CANCELLED",
          ].map((status) => (
            <option key={status}>{status}</option>
          ))}
        </select>
      </div>
      {orders.loading ? (
        <Loading />
      ) : orders.error ? (
        <ErrorState error={orders.error} retry={orders.reload} />
      ) : rows.length ? (
        <OrdersTable orders={rows} admin={admin} />
      ) : (
        <Empty
          icon={ShoppingBag}
          title={
            filter !== "ALL" || search
              ? "No matching orders"
              : "Your next sweet moment awaits"
          }
          text="Orders appear here after they are placed."
        >
          {!admin && <ButtonLink to="/shop">Explore the bakery</ButtonLink>}
        </Empty>
      )}
    </>
  );
}

export function OrdersTable({ orders, admin }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Order</th>
            <th>Placed on</th>
            {admin && <th>Customer</th>}
            <th>Items</th>
            <th>Total</th>
            <th>Status</th>
            <th>
              <span className="sr-only">Actions</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => (
            <tr key={order.id}>
              <td>
                <strong>#{String(order.id).padStart(4, "0")}</strong>
              </td>
              <td>{date(order.orderDate)}</td>
              {admin && <td>Customer #{order.customerId}</td>}
              <td>
                {order.items.reduce((sum, item) => sum + item.quantity, 0)}
              </td>
              <td className="nowrap">{money(order.total)}</td>
              <td>
                <Badge status={order.status} />
              </td>
              <td>
                <Link
                  className="table-action"
                  to={`${admin ? "/admin" : "/account"}/orders/${order.id}`}
                >
                  View
                  <ArrowUpRight size={15} />
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function OrderDetail({ admin = false }) {
  const { id } = useParams();
  const { notify } = useApp();
  const order = useResource(() => orderApi.get(id), [id]);
  const [confirm, setConfirm] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  async function action(operation, message) {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      await operation();
      notify(message);
      setConfirm(false);
      setDeleteConfirm(false);
      order.reload();
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }

  if (order.loading) return <Loading />;
  if (order.error)
    return <ErrorState error={order.error} retry={order.reload} />;
  const data = order.data;
  const steps = ["PENDING", "CONFIRMED", "PREPARING", "READY", "COMPLETED"];
  const step = steps.indexOf(data.status);

  // Customer can edit before CONFIRMED; delete before PREPARING
  const canEdit = !admin && data.status === "PENDING";
  const canDelete =
    admin ||
    (data.status === "PENDING" || data.status === "CONFIRMED");

  return (
    <>
      <Link
        className="back-link"
        to={admin ? "/admin/orders" : "/account/orders"}
      >
        <ArrowLeft size={16} />
        All orders
      </Link>
      <PageHeading
        eyebrow="THE DETAILS, ALL TOGETHER"
        title={`Order #${String(data.id).padStart(4, "0")}`}
        text={`Placed on ${date(data.orderDate)}`}
      >
        <Badge status={data.status} />
      </PageHeading>

      {data.status !== "CANCELLED" && (
        <ol className="order-progress">
          {steps.map((status, index) => (
            <li key={status} className={index <= step ? "done" : ""}>
              <span>{index < step ? <Check size={16} /> : index + 1}</span>
              <small>{status.toLowerCase()}</small>
            </li>
          ))}
        </ol>
      )}

      <div className="panel">
        {/* Delivery & contact info */}
        {(data.customerName || data.deliveryAddress) && (
          <div className="order-delivery-info">
            {data.customerName && (
              <div className="summary-row">
                <span className="muted">Name</span>
                <span>{data.customerName}</span>
              </div>
            )}
            {data.customerPhone && (
              <div className="summary-row">
                <span className="muted">Phone</span>
                <span>{data.customerPhone}</span>
              </div>
            )}
            {data.deliveryAddress && (
              <div className="summary-row">
                <span className="muted">Address</span>
                <span>{data.deliveryAddress}, {data.deliveryCity}</span>
              </div>
            )}
            {data.paymentMethod && (
              <div className="summary-row">
                <span className="muted">Payment</span>
                <span>
                  {data.paymentMethod === "CARD"
                    ? "Card at collection"
                    : "Cash on delivery"}
                </span>
              </div>
            )}
          </div>
        )}

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Product</th>
                <th>Unit price</th>
                <th>Quantity</th>
                <th>Subtotal</th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((item) => (
                <OrderItemRow
                  key={item.id}
                  item={item}
                  editable={!admin && data.status === "PENDING"}
                  busy={busy}
                  save={(quantity) =>
                    action(
                      () => orderApi.quantity(data.id, item.id, quantity),
                      "Order quantity updated.",
                    )
                  }
                />
              ))}
            </tbody>
          </table>
        </div>

        {data.customerNote && (
          <div className="order-note">
            <strong>Note</strong>
            <p>{data.customerNote}</p>
          </div>
        )}

        <div className="summary-row">
          <span className="muted">Delivery</span>
          <span>{money(data.deliveryCost || 0)}</span>
        </div>
        <div className="summary-row total">
          <span>Order total</span>
          <strong>{money(data.total)}</strong>
        </div>

        <InlineError error={error} />

        <div className="form-actions">
          {/* Customer: edit order details before CONFIRMED */}
          {canEdit && (
            <Button
              variant="secondary"
              disabled={busy}
              onClick={() => {
                setError(null);
                setEditOpen(true);
              }}
            >
              <Pencil size={15} />
              Edit details
            </Button>
          )}

          {/* Cancel (customer + admin) */}
          {canCancel(data.status) && (
            <Button
              variant="danger-outline"
              disabled={busy}
              onClick={() => {
                setError(null);
                setConfirm(true);
              }}
            >
              Cancel order
            </Button>
          )}

          {/* Admin: advance status */}
          {admin && nextStatus(data.status) && (
            <Button
              busy={busy}
              onClick={() =>
                action(
                  () => orderApi.status(data.id, nextStatus(data.status)),
                  "Order status updated.",
                )
              }
            >
              Mark as {nextStatus(data.status).toLowerCase()}
            </Button>
          )}

          {/* Delete */}
          {canDelete && (
            <Button
              variant="danger"
              disabled={busy}
              onClick={() => {
                setError(null);
                setDeleteConfirm(true);
              }}
            >
              <Trash2 size={15} />
              Delete order
            </Button>
          )}
        </div>
      </div>

      {/* Cancel confirmation */}
      {confirm && (
        <Confirm
          title="Cancel this order?"
          text={`Order #${data.id} will be marked as cancelled.`}
          busy={busy}
          error={error}
          onClose={() => setConfirm(false)}
          onConfirm={() =>
            action(
              () => orderApi.cancel(data.id),
              "Order has been cancelled.",
            )
          }
          action="Cancel order"
        />
      )}

      {/* Delete confirmation */}
      {deleteConfirm && (
        <Confirm
          title="Delete this order?"
          text={`Order #${data.id} will be permanently deleted and cannot be recovered.`}
          busy={busy}
          error={error}
          onClose={() => setDeleteConfirm(false)}
          onConfirm={() =>
            action(
              () => orderApi.remove(data.id),
              "Order has been deleted.",
            )
          }
          action="Delete order"
        />
      )}

      {/* Edit modal (customer, PENDING only) */}
      {editOpen && (
        <EditOrderModal
          order={data}
          onClose={() => setEditOpen(false)}
          onSaved={() => {
            setEditOpen(false);
            notify("Order details updated.");
            order.reload();
          }}
        />
      )}
    </>
  );
}

function OrderItemRow({ item, editable, busy, save }) {
  const [quantity, setQuantity] = useState(item.quantity);
  return (
    <tr>
      <td>
        <Link to={`/shop/${item.productId}`}>{item.productName}</Link>
      </td>
      <td>{money(item.unitPrice)}</td>
      <td>
        {editable ? (
          <div className="inline-quantity">
            <Quantity value={quantity} onChange={setQuantity} disabled={busy} />
            {quantity !== item.quantity && (
              <Button
                variant="small secondary"
                busy={busy}
                onClick={() => save(quantity)}
              >
                Save
              </Button>
            )}
          </div>
        ) : (
          item.quantity
        )}
      </td>
      <td>{money(item.subtotal)}</td>
    </tr>
  );
}

// Delivery cost helper (same rates as Checkout.jsx)
function calcDelivery(city) {
  return getDeliveryCost(city);
}

function EditOrderModal({ order, onClose, onSaved }) {
  const [form, setForm] = useState({
    customerName: order.customerName || "",
    customerPhone: order.customerPhone || "",
    deliveryAddress: order.deliveryAddress || "",
    deliveryCity: order.deliveryCity || "",
    paymentMethod: order.paymentMethod || "CASH_ON_DELIVERY",
    customerNote: order.customerNote || "",
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const deliveryCost = calcDelivery(form.deliveryCity);

  function set(field) {
    return (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }));
  }

  async function save(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await orderApi.update(order.id, { ...form, deliveryCost });
      onSaved();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title="Edit order details" onClose={onClose} busy={busy}>
      <form onSubmit={save}>
        <Field
          label="Full name"
          required
          value={form.customerName}
          onChange={set("customerName")}
          maxLength={200}
          disabled={busy}
        />
        <Field
          label="Phone number"
          required
          type="tel"
          value={form.customerPhone}
          onChange={set("customerPhone")}
          maxLength={20}
          disabled={busy}
        />
        <Field
          label="Street address"
          required
          value={form.deliveryAddress}
          onChange={set("deliveryAddress")}
          maxLength={500}
          disabled={busy}
        />
        <Field
          label="City"
          required
          value={form.deliveryCity}
          onChange={set("deliveryCity")}
          maxLength={100}
          disabled={busy}
          hint={form.deliveryCity.trim() ? `Delivery: ${money(deliveryCost)}` : ""}
        />
        <div className="payment-options" style={{ marginBottom: "1rem" }}>
          <label className={`payment-option ${form.paymentMethod === "CASH_ON_DELIVERY" ? "selected" : ""}`}>
            <input
              type="radio"
              name="pm"
              value="CASH_ON_DELIVERY"
              checked={form.paymentMethod === "CASH_ON_DELIVERY"}
              onChange={set("paymentMethod")}
              disabled={busy}
            />
            <span><strong>Cash on delivery</strong></span>
          </label>
          <label className={`payment-option ${form.paymentMethod === "CARD" ? "selected" : ""}`}>
            <input
              type="radio"
              name="pm"
              value="CARD"
              checked={form.paymentMethod === "CARD"}
              onChange={set("paymentMethod")}
              disabled={busy}
            />
            <span><strong>Card at collection</strong></span>
          </label>
        </div>
        <Field
          label="Note for the bakery"
          as="textarea"
          value={form.customerNote}
          onChange={set("customerNote")}
          maxLength={1000}
          rows={2}
          disabled={busy}
        />
        <InlineError error={error} />
        <div className="form-actions">
          <Button type="button" variant="secondary" disabled={busy} onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" busy={busy}>
            Save changes
          </Button>
        </div>
      </form>
    </Modal>
  );
}
