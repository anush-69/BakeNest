import { useId, useState, useEffect, useRef } from "react";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  CakeSlice,
  Eye,
  EyeOff,
  LoaderCircle,
  Minus,
  Plus,
  RefreshCw,
  X,
  AlertCircle,
  Star,
} from "lucide-react";

export function Button({
  children,
  variant = "",
  busy,
  className = "",
  ...props
}) {
  return (
    <button
      className={`button ${variant} ${className}`}
      {...props}
      disabled={busy || props.disabled}
    >
      {busy && <LoaderCircle size={17} className="spin" />}
      {children}
    </button>
  );
}
export function ButtonLink({ children, to, variant = "", ...props }) {
  return (
    <Link className={`button ${variant}`} to={to} {...props}>
      {children}
    </Link>
  );
}
export function SectionHeading({ eyebrow, title, text, children }) {
  return (
    <div className="section-heading">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h2>{title}</h2>
        {text && <p className="muted">{text}</p>}
      </div>
      {children}
    </div>
  );
}
export function PageHeading({ eyebrow, title, text, children }) {
  return (
    <div className="page-heading">
      <div>
        <p className="eyebrow">{eyebrow || "THE BAKENEST EXPERIENCE"}</p>
        <h1>{title}</h1>
        {text && <p className="muted">{text}</p>}
      </div>
      {children}
    </div>
  );
}
export function Field({
  label,
  hint,
  error,
  as: Tag = "input",
  className = "",
  ...props
}) {
  const id = useId();
  return (
    <div className={`field ${className}`}>
      <label htmlFor={id}>
        {label}
        {props.required && <span aria-hidden="true"> *</span>}
      </label>
      <Tag
        id={id}
        aria-invalid={!!error}
        aria-describedby={hint || error ? `${id}-hint` : undefined}
        {...props}
      />
      {(hint || error) && (
        <small id={`${id}-hint`} className={error ? "error-text" : "muted"}>
          {error || hint}
        </small>
      )}
    </div>
  );
}
export function PasswordField({ label = "Password", ...props }) {
  const [show, setShow] = useState(false);
  const id = useId();
  return (
    <div className="field">
      <label htmlFor={id}>{label} *</label>
      <div className="password-wrap">
        <input id={id} type={show ? "text" : "password"} required {...props} />
        <button
          type="button"
          className="icon-button"
          aria-label={show ? "Hide password" : "Show password"}
          aria-pressed={show}
          onClick={() => setShow(!show)}
        >
          {show ? <EyeOff size={18} /> : <Eye size={18} />}
        </button>
      </div>
    </div>
  );
}
export function Loading({ label = "Preparing something lovely…" }) {
  return (
    <div className="state" role="status">
      <LoaderCircle className="spin" size={28} />
      <p>{label}</p>
      <div className="skeleton-row">
        <i />
        <i />
        <i />
      </div>
    </div>
  );
}
export function Empty({
  title = "A little quiet here",
  text,
  children,
  icon: Icon = CakeSlice,
}) {
  return (
    <div className="state empty">
      <span className="state-icon">
        <Icon size={30} strokeWidth={1.3} />
      </span>
      <h3>{title}</h3>
      <p className="muted">{text}</p>
      {children}
    </div>
  );
}
export function ErrorState({ error, retry }) {
  return (
    <div className="state error-state" role="alert">
      <AlertCircle size={26} />
      <h3>We couldn’t load this just yet.</h3>
      <p>{error?.message || "Please try again shortly."}</p>
      {retry && (
        <Button variant="secondary" onClick={retry}>
          <RefreshCw size={16} />
          Try again
        </Button>
      )}
    </div>
  );
}
export function InlineError({ error }) {
  return error ? (
    <p className="inline-error" role="alert">
      <AlertCircle size={18} />
      {typeof error === "string" ? error : error.message}
    </p>
  ) : null;
}
export function Badge({ status }) {
  return (
    <span className={`badge ${String(status).toLowerCase()}`}>
      {String(status).replaceAll("_", " ")}
    </span>
  );
}
export function Stars({ rating }) {
  return (
    <span className="stars" aria-label={`${rating} out of 5 stars`}>
      {Array.from({ length: 5 }, (_, i) => (
        <Star
          key={i}
          size={15}
          fill={i < Math.round(rating) ? "currentColor" : "none"}
        />
      ))}
    </span>
  );
}
export function Quantity({ value, onChange, disabled }) {
  return (
    <div className="quantity">
      <button
        type="button"
        disabled={disabled || value <= 1}
        aria-label="Decrease quantity"
        onClick={() => onChange(value - 1)}
      >
        <Minus size={15} />
      </button>
      <input
        aria-label="Quantity"
        type="number"
        min="1"
        max="10000"
        value={value}
        disabled={disabled}
        onChange={(e) => {
          const n = Number(e.target.value);
          if (Number.isInteger(n) && n > 0 && n <= 10000) onChange(n);
        }}
      />
      <button
        type="button"
        disabled={disabled || value >= 10000}
        aria-label="Increase quantity"
        onClick={() => onChange(value + 1)}
      >
        <Plus size={15} />
      </button>
    </div>
  );
}
export function ProductImage({ product, className = "" }) {
  const [broken, setBroken] = useState(false);
  useEffect(() => setBroken(false), [product.imageUrl]);
  const safe =
    product.imageUrl && /^(https?:\/\/|\/[^/])/.test(product.imageUrl);
  return safe && !broken ? (
    <img
      className={className}
      src={product.imageUrl}
      alt={product.name}
      loading="lazy"
      onError={() => setBroken(true)}
    />
  ) : (
    <div className={`product-placeholder ${className}`}>
      <CakeSlice size={48} strokeWidth={1} />
      <span>Product photo coming soon</span>
    </div>
  );
}
export function Modal({ title, children, onClose, busy }) {
  const ref = useRef(null);
  const titleId = useId();
  useEffect(() => {
    const dialog = ref.current;
    dialog.showModal();
    return () => dialog.close();
  }, []);
  return (
    <dialog
      ref={ref}
      className="modal"
      aria-labelledby={titleId}
      onCancel={(e) => {
        e.preventDefault();
        if (!busy) onClose();
      }}
    >
      <div className="modal-heading">
        <h2 id={titleId}>{title}</h2>
        <button
          className="icon-button"
          aria-label="Close dialog"
          disabled={busy}
          onClick={onClose}
        >
          <X />
        </button>
      </div>
      {children}
    </dialog>
  );
}
export function Confirm({
  title,
  text,
  onConfirm,
  onClose,
  busy,
  error,
  action = "Confirm",
}) {
  return (
    <Modal title={title} onClose={onClose} busy={busy}>
      <p className="muted">{text}</p>
      <InlineError error={error} />
      <div className="form-actions">
        <Button variant="secondary" disabled={busy} onClick={onClose}>
          Keep it
        </Button>
        <Button variant="danger" busy={busy} onClick={onConfirm}>
          {action}
        </Button>
      </div>
    </Modal>
  );
}
export function TextLink({ to, children }) {
  return (
    <Link className="text-link" to={to}>
      {children}
      <ArrowRight size={17} />
    </Link>
  );
}
