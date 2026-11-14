import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ShoppingBag, Trash2, ArrowRight } from "lucide-react";
import { useApp } from "../../context/AppContext";
import { useResource } from "../../hooks/useResource";
import { productApi } from "../../api/productApi";
import { money, hasRole } from "../../lib/format";
import {
  Button,
  ButtonLink,
  PageHeading,
  Loading,
  ErrorState,
  Empty,
  ProductImage,
  Quantity,
  InlineError,
} from "../../components/common/UI";

export default function Cart() {
  const { basket, updateQuantity, user } = useApp();
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const ids = basket.map((item) => item.productId).join(",");
  const products = useResource(async () => {
    const results = await Promise.all(
      basket.map(async (item) => {
        try {
          return {
            productId: item.productId,
            product: await productApi.get(item.productId),
          };
        } catch (err) {
          if (err.status === 404)
            return { productId: item.productId, missing: true };
          throw err;
        }
      }),
    );
    return results;
  }, [ids]);

  const items = basket.map((item) => ({
    ...item,
    ...products.data?.find((p) => p.productId === item.productId),
  }));
  const subtotal = items.reduce(
    (sum, item) => sum + Number(item.product?.price || 0) * item.quantity,
    0,
  );
  const unavailable = items.some((item) => !item.product?.available);

  function proceedToCheckout() {
    setError(null);
    if (unavailable) {
      setError("Please remove unavailable items before continuing.");
      return;
    }
    navigate("/checkout");
  }

  return (
    <div className="container page">
      <PageHeading
        eyebrow="A LITTLE JOY, ALL YOURS"
        title={
          <>
            Your lovely little <em>order.</em>
          </>
        }
        text="Review your items, then continue to checkout."
      />
      {!basket.length ? (
        <Empty
          icon={ShoppingBag}
          title="There's room for something sweet"
          text="Explore our bakery and add a little joy to your order."
        >
          <ButtonLink to="/shop">
            Explore the bakery
            <ArrowRight size={17} />
          </ButtonLink>
        </Empty>
      ) : products.loading ? (
        <Loading />
      ) : products.error ? (
        <ErrorState error={products.error} retry={products.reload} />
      ) : (
        <div className="cart-layout">
          <div className="cart-items">
            {items.map((item) => (
              <article className="cart-item" key={item.productId}>
                <div className="cart-image">
                  <ProductImage
                    product={item.product || { name: "Unavailable product" }}
                  />
                </div>
                <div className="cart-item-copy">
                  <p className="eyebrow">
                    {item.product?.category?.name || "BAKERY PRODUCT"}
                  </p>
                  <h3>{item.product?.name || `Product #${item.productId}`}</h3>
                  <p>
                    {item.product
                      ? money(item.product.price)
                      : "No longer available"}
                  </p>
                  {item.product && !item.product.available && (
                    <small className="error-text">
                      Currently unavailable. Remove this item to continue.
                    </small>
                  )}
                  <Quantity
                    value={item.quantity}
                    onChange={(value) => updateQuantity(item.productId, value)}
                  />
                </div>
                <div className="cart-item-total">
                  <button
                    className="icon-button"
                    aria-label={`Remove ${item.product?.name || "product"}`}
                    onClick={() => updateQuantity(item.productId, 0)}
                  >
                    <Trash2 size={18} />
                  </button>
                  <strong>
                    {item.product
                      ? money(Number(item.product.price) * item.quantity)
                      : "—"}
                  </strong>
                </div>
              </article>
            ))}
            <ButtonLink variant="text" to="/shop">
              Keep exploring
              <ArrowRight size={16} />
            </ButtonLink>
          </div>

          <div className="order-summary panel">
            <p className="eyebrow">THE SWEET DETAILS</p>
            <h2>Order summary</h2>
            <div className="summary-row">
              <span>Subtotal</span>
              <span>{money(subtotal)}</span>
            </div>
            <div className="summary-row total">
              <strong>Total (excl. delivery)</strong>
              <strong>{money(subtotal)}</strong>
            </div>
            <p className="small muted">
              Delivery cost is calculated at checkout based on your city.
            </p>
            <InlineError error={error} />
            {!user ? (
              <ButtonLink to="/login" state={{ from: "/cart" }}>
                Sign in to continue
                <ArrowRight size={17} />
              </ButtonLink>
            ) : hasRole(user, "CUSTOMER") ? (
              <Button
                className="full-width"
                onClick={proceedToCheckout}
                disabled={unavailable}
              >
                Proceed to checkout
                <ArrowRight size={17} />
              </Button>
            ) : (
              <p className="inline-error">
                Please sign in with a customer account to place an order.
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
