import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  useRef,
} from "react";
import { authApi } from "../api/authApi";
import { sanitizeBasket } from "../lib/format";

const AppContext = createContext(null);
export function AppProvider({ children }) {
  const [user, setUser] = useState(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [authError, setAuthError] = useState(null);
  const [toasts, setToasts] = useState([]);
  const toastTimers = useRef([]);
  const [basket, setBasket] = useState(() => {
    try {
      return sanitizeBasket(
        JSON.parse(localStorage.getItem("bakenest:basket") || "[]"),
      );
    } catch {
      return [];
    }
  });
  const notify = useCallback((message, tone = "success") => {
    const id = crypto.randomUUID();
    setToasts((items) => [...items, { id, message, tone }]);
    toastTimers.current.push(
      setTimeout(
        () => setToasts((items) => items.filter((item) => item.id !== id)),
        6000,
      ),
    );
  }, []);
  const refreshUser = useCallback(async () => {
    setAuthLoading(true);
    setAuthError(null);
    try {
      const account = await authApi.me();
      setUser(account);
      return account;
    } catch (error) {
      setUser(null);
      if (error.status !== 401) setAuthError(error);
      return null;
    } finally {
      setAuthLoading(false);
    }
  }, []);
  useEffect(() => {
    refreshUser();
  }, [refreshUser]);
  useEffect(() => {
    const expired = () => {
      setUser(null);
      notify("Your session has expired. Please sign in again.", "error");
    };
    window.addEventListener("bakenest:unauthorized", expired);
    return () => window.removeEventListener("bakenest:unauthorized", expired);
  }, [notify]);
  useEffect(() => {
    try {
      localStorage.setItem("bakenest:basket", JSON.stringify(basket));
    } catch {
      /* Basket still works in memory. */
    }
  }, [basket]);
  useEffect(() => () => toastTimers.current.forEach(clearTimeout), []);
  const addToBasket = (productId, quantity = 1) => {
    if (
      !basket.some((x) => x.productId === productId) &&
      basket.length >= 100
    ) {
      notify("An order can contain up to 100 different products.", "error");
      return;
    }
    setBasket((items) => {
      const existing = items.find((item) => item.productId === productId);
      return existing
        ? items.map((item) =>
            item.productId === productId
              ? { ...item, quantity: Math.min(10000, item.quantity + quantity) }
              : item,
          )
        : [...items, { productId, quantity }];
    });
    notify("Added to your order. A little joy is on its way.");
  };
  const updateQuantity = (productId, quantity) =>
    setBasket((items) =>
      quantity === 0
        ? items.filter((item) => item.productId !== productId)
        : items.map((item) =>
            item.productId === productId
              ? { ...item, quantity: Math.max(1, Math.min(10000, quantity)) }
              : item,
          ),
    );
  const logout = async () => {
    await authApi.logout();
    setUser(null);
    setBasket([]);
    notify("You’ve been signed out. See you soon!");
  };
  return (
    <AppContext.Provider
      value={{
        user,
        setUser,
        authLoading,
        authError,
        refreshUser,
        basket,
        setBasket,
        addToBasket,
        updateQuantity,
        notify,
        logout,
      }}
    >
      {children}
      <div className="toast-stack" aria-live="polite">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast ${toast.tone}`}>
            <span>{toast.message}</span>
            <button
              aria-label="Dismiss notification"
              onClick={() =>
                setToasts((items) =>
                  items.filter((item) => item.id !== toast.id),
                )
              }
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </AppContext.Provider>
  );
}
export const useApp = () => useContext(AppContext);
