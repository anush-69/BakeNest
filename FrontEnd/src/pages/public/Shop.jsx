import { useState, useEffect } from "react";
import { useSearchParams, useParams, Link } from "react-router-dom";
import {
  Search,
  SlidersHorizontal,
  ShoppingBag,
  ArrowLeft,
  Heart,
} from "lucide-react";
import { productApi } from "../../api/productApi";
import { reviewApi } from "../../api/reviewApi";
import { useResource } from "../../hooks/useResource";
import { useApp } from "../../context/AppContext";
import { money, date, hasRole } from "../../lib/format";
import ProductCard from "../../components/products/ProductCard";
import {
  PageHeading,
  Loading,
  ErrorState,
  Empty,
  Button,
  ButtonLink,
  Quantity,
  ProductImage,
  Badge,
  Stars,
  SectionHeading,
} from "../../components/common/UI";
import { ReviewForm } from "../customer/Reviews";

export function Shop() {
  const [params, setParams] = useSearchParams();
  const [query, setQuery] = useState(params.get("name") || "");
  const [sort, setSort] = useState("default");
  const categoryId = params.get("categoryId") || "";
  const name = params.get("name") || "";
  useEffect(() => {
    setQuery(name);
  }, [name]);
  const products = useResource(
    () => productApi.list({ name, categoryId }),
    [name, categoryId],
  );
  const categories = useResource(() => productApi.categories());
  const changeFilter = (key, value) => {
    const next = new URLSearchParams(params);
    value ? next.set(key, value) : next.delete(key);
    setParams(next);
  };
  const sorted = [...(products.data || [])].sort((a, b) =>
    sort === "low"
      ? a.price - b.price
      : sort === "high"
        ? b.price - a.price
        : sort === "name"
          ? a.name.localeCompare(b.name)
          : 0,
  );
  return (
    <div className="container page">
      <PageHeading
        eyebrow="WELCOME TO OUR BAKERY"
        title={
          <>
            A little something <em>lovely.</em>
          </>
        }
        text="Find your favourites. Make a moment of it."
      />
      <div className="shop-toolbar">
        <form
          className="search-box"
          onSubmit={(e) => {
            e.preventDefault();
            changeFilter("name", query.trim());
          }}
        >
          <Search size={19} />
          <input
            aria-label="Search bakery products"
            placeholder="What are you craving?"
            value={query}
            maxLength={255}
            onChange={(e) => setQuery(e.target.value)}
          />
          <button type="submit">Search</button>
        </form>
        <label className="sort-label">
          <SlidersHorizontal size={16} />
          <span className="sr-only">Sort products</span>
          <select value={sort} onChange={(e) => setSort(e.target.value)}>
            <option value="default">Bakery selection</option>
            <option value="low">Price: low to high</option>
            <option value="high">Price: high to low</option>
            <option value="name">Name: A to Z</option>
          </select>
        </label>
      </div>
      <div className="filter-tabs" aria-label="Filter by category">
        <button
          className={!categoryId ? "active" : ""}
          onClick={() => changeFilter("categoryId", "")}
        >
          All the good things
        </button>
        {categories.data?.map((category) => (
          <button
            key={category.id}
            className={categoryId === String(category.id) ? "active" : ""}
            onClick={() => changeFilter("categoryId", String(category.id))}
          >
            {category.name}
          </button>
        ))}
      </div>
      {categories.error && (
        <p className="muted small">
          Categories are temporarily unavailable. You can still browse or search
          products.
        </p>
      )}
      {products.loading ? (
        <Loading />
      ) : products.error ? (
        <ErrorState error={products.error} retry={products.reload} />
      ) : sorted.length ? (
        <>
          <p className="result-count">
            {sorted.length} lovely {sorted.length === 1 ? "find" : "finds"}
            {name && ` for “${name}”`}
          </p>
          <div className="product-grid">
            {sorted.map((product) => (
              <ProductCard product={product} key={product.id} />
            ))}
          </div>
        </>
      ) : (
        <Empty
          title={
            name || categoryId
              ? "No treats match just yet"
              : "Our display is being prepared"
          }
          text={
            name || categoryId
              ? "Try another search or browse all our bakery favourites."
              : "Available products will appear here when the bakery adds them."
          }
        >
          {(name || categoryId) && (
            <Button variant="secondary" onClick={() => setParams({})}>
              Clear filters
            </Button>
          )}
        </Empty>
      )}
      <div className="shop-bottom-note">
        <Heart size={18} />
        <span>Every good day deserves a little sweetness.</span>
      </div>
    </div>
  );
}
export function ProductDetail() {
  const { id } = useParams();
  const { user, addToBasket } = useApp();
  const [quantity, setQuantity] = useState(1);
  const [reviewing, setReviewing] = useState(false);
  const product = useResource(() => productApi.get(id), [id]);
  const reviews = useResource(
    () => Promise.all([reviewApi.product(id), reviewApi.average(id)]),
    [id],
  );
  if (product.loading) return <Loading />;
  if (product.error)
    return (
      <div className="container page">
        <ErrorState error={product.error} retry={product.reload} />
      </div>
    );
  const p = product.data;
  return (
    <div className="container page">
      <Link className="back-link" to="/shop">
        <ArrowLeft size={16} />
        Back to our bakery
      </Link>
      <div className="product-detail">
        <div className="detail-image">
          <ProductImage product={p} />
        </div>
        <div className="detail-copy">
          <p className="eyebrow">{p.category?.name}</p>
          <h1>{p.name}</h1>
          {reviews.data?.[0].length > 0 && (
            <a href="#product-reviews" className="rating-row">
              <Stars rating={reviews.data[1].averageRating} />
              <span>
                {reviews.data[1].averageRating.toFixed(1)} ·{" "}
                {reviews.data[0].length} reviews
              </span>
            </a>
          )}
          <p className="detail-price">{money(p.price)}</p>
          <Badge status={p.available ? "AVAILABLE" : "UNAVAILABLE"} />
          <p className="detail-description">
            {p.description ||
              "A little something from our bakery. Product description coming soon."}
          </p>
          <div className="detail-order">
            <Quantity
              value={quantity}
              onChange={setQuantity}
              disabled={!p.available}
            />
            <Button
              disabled={!p.available || (user && !hasRole(user, "CUSTOMER"))}
              onClick={() => addToBasket(p.id, quantity)}
            >
              <ShoppingBag size={18} />
              Add to order
            </Button>
          </div>
          <p className="small muted">
            Your final order total is confirmed when your order is placed.
          </p>
          <div className="detail-footnote">
            <Heart size={20} strokeWidth={1.2} />
            <span>A little care. A little joy. A little BakeNest.</span>
          </div>
        </div>
      </div>
      <section id="product-reviews" className="section">
        <SectionHeading
          eyebrow="SHARED WITH LOVE"
          title="The sweetest feedback"
        >
          {hasRole(user, "CUSTOMER") ? (
            <Button variant="secondary" onClick={() => setReviewing(true)}>
              Write a review
            </Button>
          ) : (
            !user && (
              <ButtonLink to="/login" variant="secondary">
                Sign in to review
              </ButtonLink>
            )
          )}
        </SectionHeading>
        {reviews.loading ? (
          <Loading />
        ) : reviews.error ? (
          <ErrorState error={reviews.error} retry={reviews.reload} />
        ) : reviews.data[0].length ? (
          <div className="review-grid">
            {reviews.data[0].map((review) => (
              <article className="review-card" key={review.id}>
                <Stars rating={review.rating} />
                <p>{review.comment}</p>
                <small className="muted">
                  BakeNest customer · {date(review.createdAt)}
                </small>
              </article>
            ))}
          </div>
        ) : (
          <Empty
            title="Be the first to share a little love"
            text="Approved customer reviews will appear here."
          />
        )}
      </section>
      {reviewing && (
        <ReviewForm
          productId={p.id}
          onClose={() => setReviewing(false)}
          onSaved={() => {
            setReviewing(false);
            reviews.reload();
          }}
        />
      )}
    </div>
  );
}
