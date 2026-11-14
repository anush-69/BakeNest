import { Link } from "react-router-dom";
import { Plus, ArrowUpRight } from "lucide-react";
import { useApp } from "../../context/AppContext";
import { money, hasRole } from "../../lib/format";
import { ProductImage } from "../common/UI";
export default function ProductCard({ product }) {
  const { addToBasket, user } = useApp();
  return (
    <article className="product-card">
      <Link className="product-photo" to={`/shop/${product.id}`}>
        <ProductImage product={product} />
        <span className="photo-arrow">
          <ArrowUpRight size={20} />
        </span>
        {!product.available && (
          <span className="product-label">Currently unavailable</span>
        )}
      </Link>
      <div className="product-copy">
        <p className="eyebrow">{product.category?.name || "FROM OUR BAKERY"}</p>
        <h3>
          <Link to={`/shop/${product.id}`}>{product.name}</Link>
        </h3>
        <div>
          <span className="price">{money(product.price)}</span>
          <button
            className="add-button"
            disabled={
              !product.available || (user && !hasRole(user, "CUSTOMER"))
            }
            aria-label={`Add ${product.name} to your order`}
            onClick={() => addToBasket(product.id)}
          >
            <Plus size={20} />
          </button>
        </div>
        <Link className="details-link" to={`/shop/${product.id}`}>
          View details
        </Link>
      </div>
    </article>
  );
}
