import { Link } from "react-router-dom";
import {
  ArrowRight,
  ArrowUpRight,
  Wheat,
  Heart,
  CakeSlice,
  Sparkles,
  Flower2,
} from "lucide-react";
import { useResource } from "../../hooks/useResource";
import { productApi } from "../../api/productApi";
import { reviewApi } from "../../api/reviewApi";
import ProductCard from "../../components/products/ProductCard";
import {
  ButtonLink,
  SectionHeading,
  Loading,
  ErrorState,
  Empty,
  TextLink,
  Stars,
} from "../../components/common/UI";

export default function Home() {
  const products = useResource(() => productApi.list());
  const categories = useResource(() => productApi.categories());
  const reviews = useResource(() => reviewApi.list());
  return (
    <>
      <section className="hero container">
        <div className="hero-copy">
          <p className="eyebrow">
            <span />A LITTLE JOY, FRESHLY BAKED
          </p>
          <h1>
            Life’s sweeter
            <br />
            with a little
            <br />
            <em>BakeNest.</em>
            <Flower2 className="hero-flower" strokeWidth={1} />
          </h1>
          <p className="hero-description">
            Baked with love, made for every moment.
            <br />
            Discover your everyday favourites and something
            <br className="desktop-break" /> special for the moments that
            matter.
          </p>
          <div className="hero-buttons">
            <ButtonLink to="/shop">
              Explore our bakery
              <ArrowUpRight size={18} />
            </ButtonLink>
            <Link to="/custom-cakes" className="hero-secondary">
              Dream up a cake
              <ArrowRight size={17} />
            </Link>
          </div>
          <div className="hero-signature">
            <span className="signature-line" />
            <span>A little care in every crumb.</span>
            <Heart size={15} strokeWidth={1.3} />
          </div>
        </div>
        <div className="hero-art">
          <div className="hero-image">
            <img
              src="/images/celebration-cake.jpg"
              alt="A chocolate celebration cake topped with fresh fruit"
              fetchPriority="high"
            />
            <span className="image-caption">
              A TASTE OF CELEBRATION · INSPIRATION
            </span>
          </div>
          <div className="hero-stamp">
            <Wheat size={26} strokeWidth={1.2} />
            <span>
              MADE WITH
              <br />
              <strong>love & butter</strong>
            </span>
            <span className="stamp-stars">✦ &nbsp; ✦ &nbsp; ✦</span>
          </div>
          <div className="hero-note">
            <span>
              For the big days.
              <br />
              <em>And the just-because days.</em>
            </span>
            <svg viewBox="0 0 85 45" aria-hidden="true">
              <path
                d="M2 5Q36 52 78 15M62 14L79 14L75 32"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.5"
              />
            </svg>
          </div>
        </div>
      </section>
      <div className="values-strip">
        <div className="container">
          <span>
            <Wheat />
            Thoughtfully crafted
          </span>
          <i>✦</i>
          <span>
            <Heart />A little love in every bite
          </span>
          <i>✦</i>
          <span>
            <CakeSlice />
            Made for your moments
          </span>
          <i>✦</i>
          <span>
            <Sparkles />
            Simply irresistible
          </span>
        </div>
      </div>
      <section className="container section">
        <SectionHeading
          eyebrow="FIND YOUR HAPPY PLACE"
          title={
            <>
              Something for every <em>craving.</em>
            </>
          }
          text="From the first bite to the last crumb."
        >
          <TextLink to="/shop">Explore the bakery</TextLink>
        </SectionHeading>
        <div className="inspiration-grid">
          {[
            {
              image: "celebration-cake",
              name: "A reason to celebrate",
              text: "Make a sweet moment of it.",
              to: "/custom-cakes",
            },
            {
              image: "pastries",
              name: "The little indulgences",
              text: "A pause. A pastry. Pure joy.",
              to: "/shop",
            },
            {
              image: "bread",
              name: "Everyday comforts",
              text: "Simple things, beautifully made.",
              to: "/shop",
            },
          ].map((item) => (
            <Link className="inspiration-card" key={item.name} to={item.to}>
              <img
                src={`/images/${item.image}.jpg`}
                alt={
                  item.name === "Everyday comforts"
                    ? "Artisan bread, editorial inspiration"
                    : item.name === "The little indulgences"
                      ? "Golden pastries, editorial inspiration"
                      : "Fruit-topped cake, celebration inspiration"
                }
                loading="lazy"
              />
              <div>
                <span>
                  <h3>{item.name}</h3>
                  <p>{item.text}</p>
                </span>
                <ArrowUpRight />
              </div>
            </Link>
          ))}
        </div>
        <p className="photo-disclosure">
          Bakery inspiration photography. See individual products for actual
          product details.
        </p>
        {categories.data?.length > 0 && (
          <div className="category-pills">
            {categories.data.map((category) => (
              <Link key={category.id} to={`/shop?categoryId=${category.id}`}>
                {category.name}
                <ArrowRight size={14} />
              </Link>
            ))}
          </div>
        )}
      </section>
      <section className="featured-section">
        <div className="container section">
          <SectionHeading
            eyebrow="FROM OUR OVEN TO YOUR TABLE"
            title={
              <>
                Meet your next <em>favourite.</em>
              </>
            }
            text="Explore what’s available in the BakeNest bakery."
          >
            <TextLink to="/shop">Shop all treats</TextLink>
          </SectionHeading>
          {products.loading ? (
            <Loading />
          ) : products.error ? (
            <ErrorState error={products.error} retry={products.reload} />
          ) : products.data?.length ? (
            <div className="product-grid">
              {products.data.slice(0, 4).map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          ) : (
            <Empty
              title="The next batch is on its way"
              text="Our available products will appear here once added to the bakery."
            />
          )}
        </div>
      </section>
      <section className="container section">
        <div className="cake-promo">
          <div className="cake-promo-image">
            <img
              src="/images/celebration-cake.jpg"
              alt="Celebration cake inspiration"
              loading="lazy"
            />
            <span>YOUR MOMENT. YOUR CAKE.</span>
          </div>
          <div className="cake-promo-copy">
            <p className="eyebrow">A LITTLE MORE PERSONAL</p>
            <h2>
              Big dreams.
              <br />
              Beautiful <em>cakes.</em>
            </h2>
            <p>
              A birthday wish, a new beginning, or a reason all your own.
              There’s always a moment worth celebrating.
            </p>
            <ButtonLink to="/custom-cakes" variant="light">
              Explore custom cakes
              <ArrowUpRight size={18} />
            </ButtonLink>
            <small>Custom booking is coming soon.</small>
          </div>
          <Flower2 className="promo-flower" strokeWidth={0.6} />
        </div>
      </section>
      <section className="container section why-section">
        <div>
          <p className="eyebrow">THE BAKENEST WAY</p>
          <h2>
            Good things take care.
            <br />
            <em>We give them plenty.</em>
          </h2>
        </div>
        <div className="why-grid">
          {[
            [
              Wheat,
              "Thoughtfully selected",
              "Discover bakery favourites with clear product details and availability.",
            ],
            [
              Heart,
              "A more personal touch",
              "A place for your favourites, your feedback, and your next celebration.",
            ],
            [
              CakeSlice,
              "A moment made sweeter",
              "From choosing your treats to following your order, keep every detail close.",
            ],
          ].map(([Icon, title, text]) => (
            <div key={title}>
              <Icon strokeWidth={1.2} />
              <h3>{title}</h3>
              <p>{text}</p>
            </div>
          ))}
        </div>
      </section>
      {reviews.data?.length > 0 && (
        <section className="testimonials">
          <div className="container section">
            <SectionHeading
              eyebrow="LITTLE NOTES OF LOVE"
              title={
                <>
                  Sweet words from <em>our customers.</em>
                </>
              }
            />
            <div className="review-grid">
              {reviews.data.slice(0, 3).map((review) => (
                <article className="review-card" key={review.id}>
                  <Stars rating={review.rating} />
                  <blockquote>“{review.comment}”</blockquote>
                  <span>BakeNest customer · Product #{review.productId}</span>
                </article>
              ))}
            </div>
          </div>
        </section>
      )}
      <section className="closing-note container">
        <Flower2 size={32} strokeWidth={1} />
        <p>
          Some days call for cake.
          <br />
          <em>We think most days do.</em>
        </p>
        <TextLink to="/shop">Find your little joy</TextLink>
      </section>
    </>
  );
}
