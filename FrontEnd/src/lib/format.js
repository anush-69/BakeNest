export const money = (value) =>
  new Intl.NumberFormat("en-LK", {
    style: "currency",
    currency: "LKR",
    maximumFractionDigits: 2,
  }).format(Number(value || 0));
export const date = (value) =>
  value
    ? new Intl.DateTimeFormat("en-GB", {
        day: "numeric",
        month: "short",
        year: "numeric",
      }).format(new Date(value))
    : "Date unavailable";
export const hasRole = (user, role) =>
  user?.roles?.some((value) => value.toUpperCase() === role) || false;
export const nextStatus = (status) =>
  ({
    PENDING: "CONFIRMED",
    CONFIRMED: "PREPARING",
    PREPARING: "READY",
    READY: "COMPLETED",
  })[status];
export const canCancel = (status) => ["PENDING", "CONFIRMED"].includes(status);
export function validPassword(password) {
  return (
    password.length >= 8 && new TextEncoder().encode(password).length <= 72
  );
}
export function sanitizeBasket(value) {
  return Array.isArray(value)
    ? value
        .filter(
          (x) =>
            x &&
            typeof x === "object" &&
            Number.isSafeInteger(x.productId) &&
            x.productId > 0 &&
            Number.isInteger(x.quantity) &&
            x.quantity > 0 &&
            x.quantity <= 10000,
        )
        .slice(0, 100)
        .filter(
          (x, i, all) =>
            all.findIndex((y) => y.productId === x.productId) === i,
        )
        .map(({ productId, quantity }) => ({ productId, quantity }))
    : [];
}
