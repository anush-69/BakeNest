import { useState, useEffect } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Plus, Pencil, Trash2, Eye, Package, FolderPlus } from "lucide-react";
import { productApi } from "../../api/productApi";
import { useResource } from "../../hooks/useResource";
import { useApp } from "../../context/AppContext";
import { money } from "../../lib/format";
import {
  PageHeading,
  Button,
  Field,
  Modal,
  InlineError,
  Confirm,
  Loading,
  ErrorState,
  Empty,
  Badge,
  ProductImage,
} from "../../components/common/UI";
export default function Products() {
  const products = useResource(() => productApi.all());
  const categories = useResource(() => productApi.categories());
  const { notify } = useApp();
  const [params, setParams] = useSearchParams();
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("ALL");
  const [edit, setEdit] = useState(null);
  const [categoryOpen, setCategoryOpen] = useState(false);
  const [remove, setRemove] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  useEffect(() => {
    if (params.get("new") === "1") {
      setEdit({});
      setParams({}, { replace: true });
    }
  }, [params, setParams]);
  async function removeProduct() {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      await productApi.delete(remove.id);
      notify("Product marked unavailable. Order history is preserved.");
      setRemove(null);
      products.reload();
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  const rows =
    products.data?.filter(
      (product) =>
        product.name.toLowerCase().includes(search.toLowerCase()) &&
        (filter === "ALL" || product.available === (filter === "AVAILABLE")),
    ) || [];
  return (
    <>
      <PageHeading
        title="The bakery collection"
        eyebrow="PRODUCT MANAGEMENT"
        text="Keep your display full of good things."
      >
        <div className="button-row">
          <Button variant="secondary" onClick={() => setCategoryOpen(true)}>
            <FolderPlus size={17} />
            Add category
          </Button>
          <Button onClick={() => setEdit({})}>
            <Plus size={17} />
            Add product
          </Button>
        </div>
      </PageHeading>
      <div className="table-toolbar">
        <input
          aria-label="Search products"
          placeholder="Search your collection…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select
          aria-label="Filter availability"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        >
          <option>ALL</option>
          <option>AVAILABLE</option>
          <option>UNAVAILABLE</option>
        </select>
      </div>
      {products.loading ? (
        <Loading />
      ) : products.error ? (
        <ErrorState error={products.error} retry={products.reload} />
      ) : rows.length ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Product</th>
                <th>Category</th>
                <th>Price</th>
                <th>Availability</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((product) => (
                <tr key={product.id}>
                  <td>
                    <div className="table-product">
                      <div>
                        <ProductImage product={product} />
                      </div>
                      <span>
                        <strong>{product.name}</strong>
                        <small>#{product.id}</small>
                      </span>
                    </div>
                  </td>
                  <td>{product.category?.name}</td>
                  <td className="nowrap">{money(product.price)}</td>
                  <td>
                    <Badge
                      status={product.available ? "AVAILABLE" : "UNAVAILABLE"}
                    />
                  </td>
                  <td>
                    <div className="row-actions">
                      <Link
                        className="icon-button"
                        aria-label={`View ${product.name}`}
                        to={`/shop/${product.id}`}
                      >
                        <Eye size={17} />
                      </Link>
                      <button
                        className="icon-button"
                        aria-label={`Edit ${product.name}`}
                        onClick={() => setEdit(product)}
                      >
                        <Pencil size={17} />
                      </button>
                      <button
                        className="icon-button"
                        disabled={!product.available}
                        aria-label={`Remove ${product.name} from sale`}
                        onClick={() => {
                          setError(null);
                          setRemove(product);
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
          icon={Package}
          title="Make room for your next favourite"
          text={
            search || filter !== "ALL"
              ? "No products match your filters."
              : "Add a category, then your first bakery product."
          }
        />
      )}
      {edit && (
        <ProductForm
          product={edit}
          categories={categories}
          onClose={() => setEdit(null)}
          onSaved={() => {
            setEdit(null);
            products.reload();
          }}
        />
      )}
      {categoryOpen && (
        <CategoryForm
          onClose={() => setCategoryOpen(false)}
          onSaved={() => {
            setCategoryOpen(false);
            categories.reload();
          }}
        />
      )}
      {remove && (
        <Confirm
          title="Take this product off the shelf?"
          text={`${remove.name} will be marked unavailable. It stays in existing orders and can be made available again by editing it.`}
          busy={busy}
          error={error}
          onClose={() => setRemove(null)}
          onConfirm={removeProduct}
          action="Mark unavailable"
        />
      )}
    </>
  );
}
function ProductForm({ product, categories, onClose, onSaved }) {
  const { notify } = useApp();
  const [values, setValues] = useState({
    name: product.name || "",
    description: product.description || "",
    price: product.price ?? "",
    categoryId: product.category?.id || "",
    imageUrl: product.imageUrl || "",
    available: product.available ?? true,
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const change = (e) =>
    setValues((previous) => ({ ...previous, [e.target.name]: e.target.value }));
  async function submit(e) {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      if (!values.name.trim())
        throw new Error("Please give this product a name.");
      if (values.imageUrl && !/^(https?:\/\/|\/[^/])/.test(values.imageUrl))
        throw new Error("Use an http(s) image URL or a path beginning with /.");
      const body = {
        ...values,
        name: values.name.trim(),
        price: Number(values.price),
        categoryId: Number(values.categoryId),
      };
      product.id
        ? await productApi.update(product.id, body)
        : await productApi.create(body);
      notify(
        product.id
          ? "Product updated."
          : "A new favourite has joined the bakery.",
      );
      onSaved();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title={product.id ? "A little product refresh" : "Add something lovely"}
      onClose={onClose}
      busy={busy}
    >
      <form onSubmit={submit}>
        <fieldset disabled={busy}>
          <Field
            label="Product name"
            name="name"
            value={values.name}
            onChange={change}
            required
            maxLength={255}
          />
          <Field
            label="Description"
            name="description"
            as="textarea"
            rows={3}
            value={values.description}
            onChange={change}
            maxLength={2000}
          />
          <div className="form-grid">
            <Field
              label="Price (LKR)"
              name="price"
              type="number"
              min="0"
              max="9999999999.99"
              step="0.01"
              value={values.price}
              onChange={change}
              required
            />
            <Field
              label="Category"
              name="categoryId"
              as="select"
              value={values.categoryId}
              onChange={change}
              required
            >
              <option value="">Choose a category</option>
              {categories.data?.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </Field>
          </div>
          {categories.error && (
            <InlineError error="Categories could not be loaded. Close this form and try again." />
          )}
          {categories.data?.length === 0 && (
            <p className="inline-error">
              Create a category before adding a product.
            </p>
          )}
          <Field
            label="Product image URL"
            name="imageUrl"
            value={values.imageUrl}
            onChange={change}
            maxLength={1000}
            placeholder="https://…"
            hint="Use a photograph of the actual product. File upload is not available."
          />
          <label className="checkbox">
            <input
              type="checkbox"
              checked={values.available}
              onChange={(e) =>
                setValues((previous) => ({
                  ...previous,
                  available: e.target.checked,
                }))
              }
            />
            Available to order
          </label>
          <InlineError error={error} />
          <Button type="submit" busy={busy} disabled={!categories.data?.length}>
            Save product
          </Button>
        </fieldset>
      </form>
    </Modal>
  );
}
function CategoryForm({ onClose, onSaved }) {
  const { notify } = useApp();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  async function submit(e) {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      if (!name.trim()) throw new Error("Please enter a category name.");
      await productApi.createCategory({ name: name.trim(), description });
      notify("Category created.");
      onSaved();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal title="A new corner of the bakery" onClose={onClose} busy={busy}>
      <form onSubmit={submit}>
        <fieldset disabled={busy}>
          <Field
            label="Category name"
            required
            maxLength={255}
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <Field
            label="Description"
            as="textarea"
            maxLength={255}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          <InlineError error={error} />
          <Button type="submit" busy={busy}>
            Create category
          </Button>
        </fieldset>
      </form>
    </Modal>
  );
}
