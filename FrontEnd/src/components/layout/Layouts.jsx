import { useState } from "react";
import {
  Link,
  NavLink,
  Outlet,
  useNavigate,
  useLocation,
  Navigate,
} from "react-router-dom";
import {
  ChefHat,
  ShoppingBag,
  Menu,
  X,
  UserRound,
  ArrowUpRight,
  LayoutDashboard,
  Package,
  Users,
  ShieldCheck,
  ClipboardList,
  MessageSquare,
  CakeSlice,
  LogOut,
  ChevronRight,
  Store,
} from "lucide-react";
import { useApp } from "../../context/AppContext";
import { hasRole } from "../../lib/format";
import { Loading, ErrorState, ButtonLink, Empty } from "../common/UI";

export function Brand({ light = false }) {
  return (
    <Link
      to="/"
      className={`brand ${light ? "light" : ""}`}
      aria-label="BakeNest home"
    >
      <ChefHat strokeWidth={1.3} />
      <span>
        BakeNest<span className="brand-dot">.</span>
        <small>BAKED WITH LOVE</small>
      </span>
    </Link>
  );
}
export function CustomerLayout() {
  const { user, basket } = useApp();
  const [open, setOpen] = useState(false);
  const location = useLocation();
  return (
    <>
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      <div className="announcement">
        A little sweetness. A lot of love.
        <span>Made for your everyday & your extraordinary.</span>
      </div>
      <header className="site-header">
        <div className="header-inner">
          <Brand />
          <nav className="desktop-nav" aria-label="Main navigation">
            <NavLink to="/" end>
              Home
            </NavLink>
            <NavLink to="/shop">Our bakery</NavLink>
            <NavLink to="/custom-cakes">
              Custom cakes <span className="tiny-dot" />
            </NavLink>
            <NavLink to="/our-story">Our story</NavLink>
          </nav>
          <div className="header-actions">
            <Link className="account-link" to={user ? "/account" : "/login"}>
              <UserRound size={19} />
              <span>{user ? user.firstName : "Sign in"}</span>
            </Link>
            <Link
              className="basket-link"
              to="/cart"
              aria-label={`Your order, ${basket.reduce((sum, item) => sum + item.quantity, 0)} items`}
            >
              <ShoppingBag size={20} />
              <span>
                {basket.reduce((sum, item) => sum + item.quantity, 0)}
              </span>
            </Link>
            <button
              className="icon-button mobile-menu"
              aria-label={open ? "Close menu" : "Open menu"}
              aria-expanded={open}
              onClick={() => setOpen(!open)}
            >
              {open ? <X /> : <Menu />}
            </button>
          </div>
        </div>
        {open && (
          <nav className="mobile-nav" aria-label="Mobile navigation">
            {[
              ["/", "Home"],
              ["/shop", "Our bakery"],
              ["/custom-cakes", "Custom cakes"],
              ["/our-story", "Our story"],
              [user ? "/account" : "/login", user ? "My account" : "Sign in"],
            ].map(([to, label]) => (
              <Link key={to} to={to} onClick={() => setOpen(false)}>
                {label}
                <ChevronRight size={18} />
              </Link>
            ))}
          </nav>
        )}
      </header>
      <main id="main" key={location.pathname} tabIndex="-1">
        <Outlet />
      </main>
      <Footer />
    </>
  );
}
function Footer() {
  return (
    <footer className="footer">
      <div className="container footer-grid">
        <div>
          <Brand light />
          <p>
            Little joys, beautifully baked.
            <br />
            Made for every moment worth sharing.
          </p>
          <span className="footer-note">
            BAKERY ORDERS & CUSTOM CAKE BOOKING
          </span>
        </div>
        <div>
          <h3>Find your favourite</h3>
          <Link to="/shop">Explore the bakery</Link>
          <Link to="/custom-cakes">Custom cakes</Link>
          <Link to="/our-story">The BakeNest story</Link>
        </div>
        <div>
          <h3>Your little corner</h3>
          <Link to="/account">My account</Link>
          <Link to="/account/orders">My orders</Link>
          <Link to="/account/reviews">My reviews</Link>
        </div>
        <div className="footer-signoff">
          <span>
            Good things
            <br />
            are made to <em>share.</em>
          </span>
          <Link to="/shop" aria-label="Explore our bakery">
            <ArrowUpRight />
          </Link>
        </div>
      </div>
      <div className="container footer-bottom">
        <span>© {new Date().getFullYear()} BakeNest. Baked with love.</span>
        <span>SLIIT · SE1020 OOP Project</span>
        <Link to="/admin/login">
          Admin portal <ArrowUpRight size={13} />
        </Link>
      </div>
    </footer>
  );
}
export function Protected({ role }) {
  const { user, authLoading, authError, refreshUser } = useApp();
  const location = useLocation();
  if (authLoading) return <Loading label="Checking your secure session…" />;
  if (!user && authError)
    return (
      <div className="container page">
        <ErrorState error={authError} retry={refreshUser} />
        <ButtonLink
          to={role === "ADMIN" ? "/admin/login" : "/login"}
          variant="secondary"
        >
          Go to sign in
        </ButtonLink>
      </div>
    );
  if (!user)
    return (
      <Navigate
        to={role === "ADMIN" ? "/admin/login" : "/login"}
        state={{ from: location.pathname + location.search }}
        replace
      />
    );
  if (role && !hasRole(user, role))
    return (
      <div className="container page">
        <Empty
          title="This area is for a different account"
          text={
            role === "ADMIN"
              ? "An administrator account is required to enter the studio."
              : "Please use a customer account to place and manage bakery orders."
          }
        >
          <ButtonLink to={hasRole(user, "ADMIN") ? "/admin" : "/account"}>
            Go to your dashboard
          </ButtonLink>
        </Empty>
      </div>
    );
  return <Outlet />;
}
export function AccountLayout() {
  return (
    <div className="container page">
      <div className="account-heading">
        <span className="eyebrow">YOUR LITTLE CORNER</span>
        <h1>Make yourself at home.</h1>
      </div>
      <nav className="tabs" aria-label="Account navigation">
        <NavLink to="/account" end>
          My profile
        </NavLink>
        <NavLink to="/account/orders">My orders</NavLink>
        <NavLink to="/account/reviews">My reviews</NavLink>
        <NavLink to="/account/bookings">Cake bookings</NavLink>
      </nav>
      <Outlet />
    </div>
  );
}
export function AdminLayout() {
  const { user, logout, notify } = useApp();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  const links = [
    ["/admin", LayoutDashboard, "Overview"],
    ["/admin/products", Package, "Products"],
    ["/admin/orders", ClipboardList, "Orders"],
    ["/admin/customers", Users, "Customers"],
    ["/admin/reviews", MessageSquare, "Reviews"],
    ["/admin/bookings", CakeSlice, "Cake bookings"],
    ["/admin/admins", ShieldCheck, "Administrators"],
  ];
  async function signout() {
    setBusy(true);
    try {
      await logout();
      navigate("/admin/login");
    } catch (e) {
      notify(e.message, "error");
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="admin-shell">
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      {open && (
        <button
          aria-label="Close sidebar"
          className="sidebar-scrim"
          onClick={() => setOpen(false)}
        />
      )}
      <aside className={`sidebar ${open ? "open" : ""}`}>
        <Brand />
        <span className="sidebar-label">THE BAKERY STUDIO</span>
        <nav aria-label="Administration">
          {links.map(([to, Icon, label]) => (
            <NavLink
              key={to}
              to={to}
              end={to === "/admin"}
              onClick={() => setOpen(false)}
            >
              <Icon size={19} />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <Link to="/">
            <Store size={18} />
            Visit storefront
            <ArrowUpRight size={15} />
          </Link>
          <button disabled={busy} onClick={signout}>
            <LogOut size={18} />
            {busy ? "Signing out…" : "Sign out"}
          </button>
          <p>
            Thoughtfully baked.
            <br />
            Thoughtfully managed.
          </p>
        </div>
      </aside>
      <div className="admin-main">
        <header className="admin-topbar">
          <div>
            <button
              className="icon-button mobile-menu"
              aria-label="Open admin menu"
              onClick={() => setOpen(true)}
            >
              <Menu />
            </button>
            <span>
              Workspace <ChevronRight size={14} /> BakeNest
            </span>
          </div>
          <Link to="/account" className="admin-profile">
            <span className="avatar">{user.firstName?.[0] || "A"}</span>
            <span>
              {user.firstName} {user.lastName}
              <small>Administrator</small>
            </span>
          </Link>
        </header>
        <main id="main" className="admin-content" tabIndex="-1">
          <Outlet />
        </main>
        <div className="admin-bottom">
          BakeNest Studio <span>Made with care, down to the last detail.</span>
        </div>
      </div>
    </div>
  );
}
