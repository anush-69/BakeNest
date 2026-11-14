import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { ArrowUpRight, ShieldCheck, Heart } from "lucide-react";
import { authApi } from "../../api/authApi";
import { useApp } from "../../context/AppContext";
import { hasRole, validPassword } from "../../lib/format";
import {
  Button,
  Field,
  PasswordField,
  InlineError,
  ButtonLink,
} from "../../components/common/UI";
import {
  RegistrationFields,
  profilePayload,
} from "../../components/forms/AccountFields";
export default function Auth({ register = false, admin = false }) {
  const { setUser, notify } = useApp();
  const navigate = useNavigate();
  const location = useLocation();
  const [values, setValues] = useState({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [complete, setComplete] = useState(false);
  const change = (event) =>
    setValues((previous) => ({
      ...previous,
      [event.target.name]: event.target.value,
    }));
  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    setError(null);
    if (register && values.password !== values.confirmation) {
      setError("Your passwords don’t match. Please check them again.");
      return;
    }
    if (register && !validPassword(values.password)) {
      setError("Use at least 8 characters, within 72 UTF-8 bytes.");
      return;
    }
    setBusy(true);
    try {
      if (register) {
        await authApi.register({
          ...profilePayload(values),
          username: values.username,
          password: values.password,
        });
        setValues({});
        setComplete(true);
      } else {
        const account = await authApi.login(values.username, values.password);
        setUser(account);
        if (admin && !hasRole(account, "ADMIN")) {
          await authApi.logout();
          setUser(null);
          throw new Error(
            "This account does not have administrator access. Please use customer sign in.",
          );
        }
        notify(`Welcome back, ${account.firstName}.`);
        const from = location.state?.from;
        navigate(
          from?.startsWith("/") &&
            !from.startsWith("//") &&
            !from.includes("login")
            ? from
            : hasRole(account, "ADMIN")
              ? "/admin"
              : "/account",
          { replace: true },
        );
      }
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className={`auth-page ${register ? "registration" : ""}`}>
      <div className="auth-photo">
        <img src="/images/pastries.jpg" alt="Golden bakery pastries" />
        <div>
          <p className="eyebrow">A LITTLE JOY AWAITS</p>
          <h2>
            Good to have
            <br />
            you <em>here.</em>
          </h2>
          <p>
            Your favourites. Your moments.
            <br />
            Your little corner of BakeNest.
          </p>
          <Heart strokeWidth={1} />
        </div>
      </div>
      <div className="auth-content">
        {complete ? (
          <div className="auth-form">
            <span className="state-icon">
              <Heart />
            </span>
            <h1>
              You’re part of
              <br />
              the <em>nest.</em>
            </h1>
            <p>
              Your account has been created. Sign in to start your next sweet
              moment.
            </p>
            <ButtonLink to="/login">
              Sign in
              <ArrowUpRight size={18} />
            </ButtonLink>
          </div>
        ) : (
          <form className="auth-form" onSubmit={submit}>
            <p className="eyebrow">
              {admin ? (
                <>
                  <ShieldCheck size={15} /> THE BAKERY STUDIO
                </>
              ) : (
                "WELCOME TO BAKENEST"
              )}
            </p>
            <h1>
              {register ? (
                <>
                  Join our <em>little nest.</em>
                </>
              ) : admin ? (
                <>
                  Welcome to
                  <br />
                  the <em>studio.</em>
                </>
              ) : (
                <>
                  A sweeter
                  <br />
                  <em>welcome back.</em>
                </>
              )}
            </h1>
            <p className="muted">
              {register
                ? "Create an account. Make room for a little joy."
                : admin
                  ? "Sign in with your administrator account."
                  : "Sign in to your favourites, orders, and more."}
            </p>
            <fieldset disabled={busy}>
              {register ? (
                <RegistrationFields values={values} setValues={setValues} />
              ) : (
                <>
                  <Field
                    label="Username"
                    name="username"
                    required
                    value={values.username || ""}
                    onChange={change}
                    autoComplete="username"
                    maxLength={50}
                    placeholder="Your BakeNest username"
                  />
                  <PasswordField
                    name="password"
                    value={values.password || ""}
                    onChange={change}
                    autoComplete="current-password"
                  />
                </>
              )}
              <InlineError error={error} />
              <Button type="submit" busy={busy} className="full-width">
                {register
                  ? "Create my account"
                  : admin
                    ? "Enter the studio"
                    : "Sign in"}
                <ArrowUpRight size={18} />
              </Button>
            </fieldset>
            <p className="auth-switch">
              {register ? (
                <>
                  Already part of the nest? <Link to="/login">Sign in</Link>
                </>
              ) : admin ? (
                <Link to="/login">Looking for customer sign in?</Link>
              ) : (
                <>
                  New around here? <Link to="/register">Create an account</Link>
                </>
              )}
            </p>
          </form>
        )}
      </div>
    </div>
  );
}
