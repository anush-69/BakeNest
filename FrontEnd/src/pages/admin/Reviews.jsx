import { useState } from "react";
import { Eye, Trash2, MessageSquare } from "lucide-react";
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
} from "../../components/common/UI";
export default function ReviewsAdmin() {
  const reviews = useResource(() => reviewApi.all());
  const { notify } = useApp();
  const [filter, setFilter] = useState("ALL");
  const [search, setSearch] = useState("");
  const [detail, setDetail] = useState(null);
  const [remove, setRemove] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const rows =
    reviews.data?.filter(
      (review) =>
        (filter === "ALL" || review.status === filter) &&
        `${review.comment} ${review.id} ${review.productId}`
          .toLowerCase()
          .includes(search.toLowerCase()),
    ) || [];
  async function deleteReview() {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      await reviewApi.delete(remove.id);
      notify("Review deleted.");
      setRemove(null);
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
        title="Little notes of love"
        eyebrow="FEEDBACK & MODERATION"
        text="Give every customer’s words the care they deserve."
      />
      <div className="table-toolbar">
        <input
          aria-label="Search reviews"
          value={search}
          placeholder="Search feedback or product number…"
          onChange={(e) => setSearch(e.target.value)}
        />
        <select
          aria-label="Filter review status"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        >
          {["ALL", "PENDING", "APPROVED", "REJECTED"].map((status) => (
            <option key={status}>{status}</option>
          ))}
        </select>
      </div>
      {reviews.loading ? (
        <Loading />
      ) : reviews.error ? (
        <ErrorState error={reviews.error} retry={reviews.reload} />
      ) : rows.length ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Feedback</th>
                <th>Rating</th>
                <th>Customer / Product</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((review) => (
                <tr key={review.id}>
                  <td>
                    <p className="table-comment">{review.comment}</p>
                    <small>{date(review.createdAt)}</small>
                  </td>
                  <td>
                    <Stars rating={review.rating} />
                  </td>
                  <td>
                    Customer #{review.customerId}
                    <small>Product #{review.productId}</small>
                  </td>
                  <td>
                    <Badge status={review.status} />
                  </td>
                  <td>
                    <div className="row-actions">
                      <button
                        className="icon-button"
                        aria-label={`View and moderate review ${review.id}`}
                        onClick={() => setDetail(review.id)}
                      >
                        <Eye size={17} />
                      </button>
                      <button
                        className="icon-button"
                        aria-label={`Delete review ${review.id}`}
                        onClick={() => {
                          setError(null);
                          setRemove(review);
                        }}
                      >
                        <Trash2 size={17} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <Empty
          icon={MessageSquare}
          title="All quiet on the feedback front"
          text="Customer reviews will appear here for moderation."
        />
      )}
      {detail && (
        <Moderation
          id={detail}
          onClose={() => setDetail(null)}
          onSaved={() => {
            setDetail(null);
            reviews.reload();
          }}
        />
      )}
      {remove && (
        <Confirm
          title="Permanently delete this review?"
          text="The customer’s feedback will be removed. Use moderation to hide it without deleting it."
          action="Delete review"
          onClose={() => setRemove(null)}
          onConfirm={deleteReview}
          busy={busy}
          error={error}
        />
      )}
    </>
  );
}
function Moderation({ id, onClose, onSaved }) {
  const review = useResource(() => reviewApi.get(id), [id]);
  const { notify } = useApp();
  const [status, setStatus] = useState("APPROVED");
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  async function submit(e) {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      await reviewApi.moderate(id, { status, note });
      notify("Review moderation saved.");
      onSaved();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal title={`Review #${id}`} onClose={onClose} busy={busy}>
      {review.loading ? (
        <Loading />
      ) : review.error ? (
        <ErrorState error={review.error} retry={review.reload} />
      ) : (
        <>
          <div className="review-top">
            <Stars rating={review.data.rating} />
            <Badge status={review.data.status} />
          </div>
          <blockquote className="moderation-quote">
            {review.data.comment}
          </blockquote>
          <p className="small muted">
            Customer #{review.data.customerId} · Product #
            {review.data.productId} · {date(review.data.createdAt)}
          </p>
          <form onSubmit={submit}>
            <fieldset disabled={busy}>
              <Field
                label="Moderation decision"
                as="select"
                value={status}
                onChange={(e) => setStatus(e.target.value)}
              >
                <option value="APPROVED">Approve for public display</option>
                <option value="REJECTED">Reject from public display</option>
              </Field>
              <Field
                label="Internal moderation note"
                as="textarea"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                maxLength={1000}
                rows={3}
              />
              <InlineError error={error} />
              <Button type="submit" busy={busy}>
                Save decision
              </Button>
            </fieldset>
          </form>
        </>
      )}
    </Modal>
  );
}
