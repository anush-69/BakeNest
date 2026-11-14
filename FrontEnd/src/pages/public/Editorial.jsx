import { CakeSlice, ArrowUpRight, Heart, Wheat } from "lucide-react";
import { ButtonLink, Empty, PageHeading } from "../../components/common/UI";
import { bookingCapabilities } from "../../api/bookingApi";

export function CustomCakes({ account = false, admin = false }) {
  if (account || admin)
    return (
      <>
        <PageHeading
          eyebrow={admin ? "BAKERY STUDIO" : "YOUR CELEBRATIONS"}
          title="Cake bookings"
          text="A place for your one-of-a-kind celebrations."
        />
        <Empty
          icon={CakeSlice}
          title="Something special is in the making"
          text="Custom cake booking is not available yet. Once the booking service is ready, you’ll be able to manage bookings here."
        >
          <ButtonLink to={admin ? "/admin" : "/shop"} variant="secondary">
            {admin ? "Back to overview" : "Explore the bakery"}
          </ButtonLink>
        </Empty>
      </>
    );
  return (
    <div className="container page">
      <div className="editorial-split">
        <div>
          <p className="eyebrow">YOUR STORY, BEAUTIFULLY BAKED</p>
          <h1>
            A cake as special
            <br />
            as <em>your moment.</em>
          </h1>
          <p className="lead">
            The wish before the candles. The smile before the first slice. A
            celebration that feels entirely yours.
          </p>
          <div className="coming-soon">
            <CakeSlice size={25} />
            <div>
              <strong>Something lovely is coming.</strong>
              <p>
                Custom cake bookings are not open yet. We’re preparing a little
                space for your big ideas.
              </p>
            </div>
          </div>
          <button className="button" disabled={!bookingCapabilities.create}>
            Booking coming soon
          </button>
          <ButtonLink to="/shop" variant="text">
            Explore available treats
            <ArrowUpRight size={17} />
          </ButtonLink>
        </div>
        <div className="editorial-image">
          <img
            src="/images/celebration-cake.jpg"
            alt="Chocolate and fresh fruit cake, celebration inspiration"
          />
          <span>CELEBRATION INSPIRATION</span>
        </div>
      </div>
      <div className="editorial-quote">
        <span>✦</span>
        <h2>
          Every celebration begins
          <br />
          with a little <em>imagination.</em>
        </h2>
        <p>Birthday wishes. Beautiful beginnings. Just-because moments.</p>
      </div>
    </div>
  );
}
export function OurStory() {
  return (
    <div className="container page">
      <div className="editorial-split">
        <div>
          <p className="eyebrow">WELCOME TO BAKENEST</p>
          <h1>
            A little nest.
            <br />A lot of <em>heart.</em>
          </h1>
          <p className="lead">
            We believe the sweetest moments are often the simplest. A shared
            slice. A thoughtful treat. Something lovely to look forward to.
          </p>
          <p className="muted">
            BakeNest brings your bakery favourites and celebrations together in
            one warm, welcoming place. Browse, choose, and follow your order —
            with a little more joy along the way.
          </p>
          <ButtonLink to="/shop">
            Find your favourite
            <ArrowUpRight size={18} />
          </ButtonLink>
        </div>
        <div className="editorial-image">
          <img
            src="/images/bread.jpg"
            alt="Rustic artisan loaves, bakery inspiration"
          />
          <span>THE BEAUTY OF SIMPLE THINGS</span>
        </div>
      </div>
      <div className="story-values">
        <div>
          <Wheat />
          <h3>Everyday comforts</h3>
          <p>Good things that make ordinary days feel a little more special.</p>
        </div>
        <div>
          <Heart />
          <h3>Moments together</h3>
          <p>
            A place for the treats you love and the people you share them with.
          </p>
        </div>
        <div>
          <CakeSlice />
          <h3>Sweet possibilities</h3>
          <p>From a small indulgence to your next big celebration.</p>
        </div>
      </div>
      <p className="project-credit">
        BakeNest — Bakery Orders and Custom Cake Booking Platform
        <br />
        SLIIT · SE1020 — Object-Oriented Programming
      </p>
    </div>
  );
}
export function NotFound() {
  return (
    <div className="container page">
      <Empty
        title="This little corner hasn’t been baked yet"
        text="The page you’re looking for may have moved."
      >
        <ButtonLink to="/">Back to BakeNest</ButtonLink>
      </Empty>
    </div>
  );
}
