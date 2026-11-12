import { lazy, Suspense, useEffect, Component } from "react";
import { Routes, Route, useLocation } from "react-router-dom";
import {
  CustomerLayout,
  AccountLayout,
  AdminLayout,
  Protected,
} from "./components/layout/Layouts";
import { Loading } from "./components/common/UI";
import Home from "./pages/public/Home";
import { Shop, ProductDetail } from "./pages/public/Shop";
import Auth from "./pages/public/Auth";
import { CustomCakes, OurStory, NotFound } from "./pages/public/Editorial";
import Profile from "./pages/customer/Profile";
import Cart from "./pages/customer/Cart";
import Checkout from "./pages/customer/Checkout";
import { Orders, OrderDetail } from "./pages/customer/Orders";
import MyReviews from "./pages/customer/Reviews";
const Dashboard = lazy(() => import("./pages/admin/Dashboard"));
const Products = lazy(() => import("./pages/admin/Products"));
const Accounts = lazy(() => import("./pages/admin/Accounts"));
const ReviewsAdmin = lazy(() => import("./pages/admin/Reviews"));
function RouteEffects() {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo(0, 0);
    const names = {
      "/": "Baked with love",
      "/shop": "Our bakery",
      "/login": "Welcome back",
      "/register": "Join the nest",
      "/custom-cakes": "Custom cakes",
      "/our-story": "Our story",
      "/admin": "Bakery studio",
      "/cart": "Your order",
      "/account": "Your account",
    };
    document.title = `BakeNest — ${names[pathname] || (pathname.startsWith("/admin") ? "Bakery studio" : "Your sweet moments")}`;
  }, [pathname]);
  return null;
}
class ErrorBoundary extends Component {
  state = { error: false };
  static getDerivedStateFromError() {
    return { error: true };
  }
  render() {
    return this.state.error ? (
      <div className="container page state">
        <h1>A little hiccup.</h1>
        <p>Something didn’t load as expected. Please refresh to try again.</p>
        <button className="button" onClick={() => window.location.reload()}>
          Refresh BakeNest
        </button>
      </div>
    ) : (
      this.props.children
    );
  }
}
export default function App() {
  return (
    <ErrorBoundary>
      <RouteEffects />
      <Suspense fallback={<Loading />}>
        <Routes>
          <Route element={<CustomerLayout />}>
            <Route index element={<Home />} />
            <Route path="shop" element={<Shop />} />
            <Route path="shop/:id" element={<ProductDetail />} />
            <Route path="login" element={<Auth key="login" />} />
            <Route path="register" element={<Auth key="register" register />} />
            <Route path="admin/login" element={<Auth key="admin" admin />} />
            <Route path="cart" element={<Cart />} />
            <Route path="checkout" element={<Checkout />} />
            <Route path="custom-cakes" element={<CustomCakes />} />
            <Route path="our-story" element={<OurStory />} />
            <Route element={<Protected />}>
              <Route path="account" element={<AccountLayout />}>
                <Route index element={<Profile />} />
                <Route element={<Protected role="CUSTOMER" />}>
                  <Route path="orders" element={<Orders />} />
                  <Route path="orders/:id" element={<OrderDetail />} />
                  <Route path="reviews" element={<MyReviews />} />
                  <Route path="bookings" element={<CustomCakes account />} />
                  <Route
                    path="bookings/:id"
                    element={<CustomCakes account />}
                  />
                </Route>
              </Route>
            </Route>
            <Route path="*" element={<NotFound />} />
          </Route>
          <Route element={<Protected role="ADMIN" />}>
            <Route path="admin" element={<AdminLayout />}>
              <Route index element={<Dashboard />} />
              <Route path="products" element={<Products />} />
              <Route path="customers" element={<Accounts key="customers" />} />
              <Route path="admins" element={<Accounts key="admins" admins />} />
              <Route path="orders" element={<Orders admin />} />
              <Route path="orders/:id" element={<OrderDetail admin />} />
              <Route path="reviews" element={<ReviewsAdmin />} />
              <Route path="bookings" element={<CustomCakes admin />} />
              <Route path="*" element={<NotFound />} />
            </Route>
          </Route>
        </Routes>
      </Suspense>
    </ErrorBoundary>
  );
}
