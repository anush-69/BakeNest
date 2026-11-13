import { useState } from "react";
import { Link } from "react-router-dom";
import { Pencil, Trash2, MessageSquare } from "lucide-react";
import { reviewApi } from "../../api/reviewApi";
import { useResource } from "../../hooks/useResource";
import { useApp } from "../../context/AppContext";
import { date } from "../../lib/format";
import {
  PageHeading,
  Loading,
  ErrorState,
  Empty,
  Stars,
  Badge,
  Button,
  Field,
  Modal,
  InlineError,
  Confirm,
  ButtonLink,
} from "../../components/common/UI";
export function ReviewForm({ productId, review, onClose, onSaved }) {
  const { notify } = useApp();
  const [rating, setRating] = useState(review?.rating || 5);
  const [comment, setComment] = useState(review?.comment || "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    if (!comment.trim()) {
      setError("Please write a few words about your experience.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const body = { rating: Number(rating), comment: comment.trim() };
      if (review) await reviewApi.update(review.id, body);
      else await reviewApi.create({ ...body, productId: Number(productId) });
      notify("Your review is saved and awaiting moderation.");
      onSaved();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title={review ? "A little update to your review" : "Share a little love"}
      onClose={onClose}
      busy={busy}
    >
      <p className="muted">
        Tell us about your experience. Reviews are checked before appearing in
        the bakery.
      </p>
      <form onSubmit={submit}>
        <fieldset disabled={busy}>
          <Field
            label="Your rating"
            as="select"
            value={rating}
            onChange={(e) => setRating(e.target.value)}
          >
            {[5, 4, 3, 2, 1].map((value) => (
              <option key={value} value={value}>
                {value} {value === 1 ? "star" : "stars"}
              </option>
            ))}
          </Field>
          <Field
            label="Your review"
            as="textarea"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            required
            maxLength={2000}
            rows={5}
            placeholder="What made your moment sweet?"
          />
          <InlineError error={error} />
          <Button type="submit" busy={busy}>
            {review ? "Save review" : "Submit review"}
          </Button>
        </fieldset>
      </form>
    </Modal>
  );
}
export default function MyReviews() {
  const { user, notify } = useApp();
  const reviews = useResource(() => reviewApi.customer(user.id), [user.id]);
  const [edit, setEdit] = useState(null);
  const [remove, setRemove] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  async function deleteReview() {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      await reviewApi.delete(remove.id);
      setRemove(null);
      notify("Your review has been deleted.");
      reviews.reload();
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <PageHeading
        eyebrow="YOUR WORDS MATTER"
        title="Your little notes of love"
        text="A history of the feedback you’ve shared with the bakery."
      />
      {reviews.loading ? (
        <Loading />
      ) : reviews.error ? (
        <ErrorState error={reviews.error} retry={reviews.reload} />
      ) : reviews.data.length ? (
        <div className="review-grid">
          {reviews.data.map((review) => (
            <article className="review-card" key={review.id}>
              <div className="review-top">
                <Stars rating={review.rating} />
                <Badge status={review.status} />
              </div>
              <Link className="text-link" to={`/shop/${review.productId}`}>
                Product #{review.productId}
              </Link>
              <p>{review.comment}</p>
              <small className="muted">{date(review.createdAt)}</small>
              <div className="review-actions">
                <Button
                  variant="small secondary"
                  onClick={() => setEdit(review)}
                >
                  <Pencil size={14} />
                  Edit
                </Button>
                <Button
                  variant="small danger-outline"
                  onClick={() => {
                    setError(null);
                    setRemove(review);
                  }}
                >
                  <Trash2 size={14} />
                  Delete
                </Button>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <Empty
          icon={MessageSquare}
          title="Your first sweet word awaits"
          text="Visit a product to tell us what you think."
        >
          <ButtonLink to="/shop">Explore our bakery</ButtonLink>
        </Empty>
      )}
      {edit && (
        <ReviewForm
          review={edit}
          onClose={() => setEdit(null)}
          onSaved={() => {
            setEdit(null);
            reviews.reload();
          }}
        />
      )}
      {remove && (
        <Confirm
          title="Delete your review?"
          text="Your feedback will be permanently removed."
          onClose={() => setRemove(null)}
          onConfirm={deleteReview}
          busy={busy}
          error={error}
          action="Delete review"
        />
      )}
    </>
  );
}
