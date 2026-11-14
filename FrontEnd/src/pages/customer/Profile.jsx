import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import {
  LogOut,
  LockKeyhole,
  ShoppingBag,
  MessageSquare,
  ShieldCheck,
} from "lucide-react";
import { useApp } from "../../context/AppContext";
import { userApi } from "../../api/userApi";
import { hasRole, validPassword } from "../../lib/format";
import {
  Button,
  PasswordField,
  InlineError,
  Modal,
  Badge,
} from "../../components/common/UI";
import {
  ProfileFields,
  profilePayload,
} from "../../components/forms/AccountFields";
export default function Profile() {
  const { user, setUser, logout, notify } = useApp();
  const navigate = useNavigate();
  const [values, setValues] = useState(user);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [passwordOpen, setPasswordOpen] = useState(false);
  async function save(event) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      const result = await userApi.update(user.id, profilePayload(values));
      setUser(result);
      notify("Your profile has been updated.");
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  async function signout() {
    setBusy(true);
    setError(null);
    try {
      await logout();
      navigate("/");
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="profile-layout">
      <aside className="profile-summary">
        <span className="avatar large">
          {user.firstName?.[0]}
          {user.lastName?.[0]}
        </span>
        <h2>
          {user.firstName} {user.lastName}
        </h2>
        <p>@{user.username}</p>
        <Badge status={user.status} />
        <div className="profile-shortcuts">
          {hasRole(user, "CUSTOMER") && (
            <>
              <Link to="/account/orders">
                <ShoppingBag size={18} />
                My orders
              </Link>
              <Link to="/account/reviews">
                <MessageSquare size={18} />
                My reviews
              </Link>
            </>
          )}
          {hasRole(user, "ADMIN") && (
            <Link to="/admin">
              <ShieldCheck size={18} />
              Admin studio
            </Link>
          )}
          <button onClick={() => setPasswordOpen(true)}>
            <LockKeyhole size={18} />
            Change password
          </button>
          <button disabled={busy} onClick={signout}>
            <LogOut size={18} />
            Sign out
          </button>
        </div>
      </aside>
      <form className="panel profile-form" onSubmit={save}>
        <h2>The personal details</h2>
        <p className="muted">Keep your information feeling up to date.</p>
        <fieldset disabled={busy}>
          <ProfileFields values={values} setValues={setValues} />
          <InlineError error={error} />
          <Button busy={busy} type="submit">
            Save changes
          </Button>
        </fieldset>
      </form>
      {passwordOpen && <PasswordModal onClose={() => setPasswordOpen(false)} />}
    </div>
  );
}
function PasswordModal({ onClose }) {
  const { setUser, notify } = useApp();
  const navigate = useNavigate();
  const [values, setValues] = useState({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const change = (e) =>
    setValues((previous) => ({ ...previous, [e.target.name]: e.target.value }));
  async function submit(e) {
    e.preventDefault();
    if (busy) return;
    setError(null);
    if (values.newPassword !== values.confirmation) {
      setError("The new passwords don’t match.");
      return;
    }
    if (!validPassword(values.newPassword)) {
      setError("Use at least 8 characters, within 72 UTF-8 bytes.");
      return;
    }
    setBusy(true);
    try {
      await userApi.password({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      setUser(null);
      notify("Password changed. Please sign in with your new password.");
      navigate("/login", { replace: true });
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal title="A fresh password" onClose={onClose} busy={busy}>
      <p className="muted">
        Changing your password signs out your existing sessions.
      </p>
      <form onSubmit={submit}>
        <fieldset disabled={busy}>
          <PasswordField
            label="Current password"
            name="currentPassword"
            value={values.currentPassword || ""}
            onChange={change}
            autoComplete="current-password"
          />
          <PasswordField
            label="New password"
            name="newPassword"
            value={values.newPassword || ""}
            onChange={change}
            minLength={8}
            maxLength={72}
            autoComplete="new-password"
          />
          <PasswordField
            label="Confirm new password"
            name="confirmation"
            value={values.confirmation || ""}
            onChange={change}
            minLength={8}
            maxLength={72}
            autoComplete="new-password"
          />
          <InlineError error={error} />
          <Button busy={busy} type="submit">
            Update password
          </Button>
        </fieldset>
      </form>
    </Modal>
  );
}
