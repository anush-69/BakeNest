import { useState } from "react";
import { Plus, Eye, Pencil, Trash2, Users } from "lucide-react";
import { userApi } from "../../api/userApi";
import { adminApi } from "../../api/adminApi";
import { useResource } from "../../hooks/useResource";
import { useApp } from "../../context/AppContext";
import { hasRole, validPassword } from "../../lib/format";
import {
  ProfileFields,
  RegistrationFields,
  profilePayload,
} from "../../components/forms/AccountFields";
import {
  PageHeading,
  Button,
  Modal,
  InlineError,
  Confirm,
  Loading,
  ErrorState,
  Empty,
  Badge,
} from "../../components/common/UI";
export default function Accounts({ admins = false }) {
  const api = admins ? adminApi : userApi;
  const { user, notify } = useApp();
  const accounts = useResource(() => api.list(), [admins]);
  const [search, setSearch] = useState("");
  const [edit, setEdit] = useState(null);
  const [detail, setDetail] = useState(null);
  const [remove, setRemove] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const rows = (accounts.data || []).filter(
    (account) =>
      (admins ||
        (hasRole(account, "CUSTOMER") && !hasRole(account, "ADMIN"))) &&
      `${account.firstName} ${account.lastName} ${account.username} ${account.email}`
        .toLowerCase()
        .includes(search.toLowerCase()),
  );
  async function deleteAccount() {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      await api.delete(remove.id);
      notify("Account deactivated.");
      setRemove(null);
      accounts.reload();
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <PageHeading
        eyebrow={admins ? "PEOPLE BEHIND THE BAKERY" : "THE BAKENEST COMMUNITY"}
        title={admins ? "Administrators" : "Our customers"}
        text={
          admins
            ? "The team making every detail a little sweeter."
            : "A little care for everyone in the nest."
        }
      >
        {admins && (
          <Button onClick={() => setEdit({})}>
            <Plus size={17} />
            Add administrator
          </Button>
        )}
      </PageHeading>
      <div className="table-toolbar">
        <input
          aria-label="Search accounts"
          placeholder="Search name, username or email…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <span className="muted small">
          {accounts.data ? `${rows.length} accounts` : ""}
        </span>
      </div>
      {accounts.loading ? (
        <Loading />
      ) : accounts.error ? (
        <ErrorState error={accounts.error} retry={accounts.reload} />
      ) : rows.length ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Contact</th>
                <th>Username</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((account) => (
                <tr key={account.id}>
                  <td>
                    <div className="table-person">
                      <span className="avatar">
                        {account.firstName?.[0]}
                        {account.lastName?.[0]}
                      </span>
                      <span>
                        <strong>
                          {account.firstName} {account.lastName}
                        </strong>
                        <small>#{account.id}</small>
                      </span>
                    </div>
                  </td>
                  <td>
                    {account.email}
                    <small>{account.phone || "No phone provided"}</small>
                  </td>
                  <td>{account.username}</td>
                  <td>
                    <Badge status={account.status} />
                  </td>
                  <td>
                    <div className="row-actions">
                      <button
                        className="icon-button"
                        aria-label={`View ${account.username}`}
                        onClick={() => setDetail(account.id)}
                      >
                        <Eye size={17} />
                      </button>
                      <button
                        className="icon-button"
                        aria-label={`Edit ${account.username}`}
                        onClick={() => setEdit(account)}
                      >
                        <Pencil size={17} />
                      </button>
                      <button
                        className="icon-button"
                        aria-label={`Deactivate ${account.username}`}
                        disabled={
                          account.status === "DELETED" ||
                          account.id === user.id ||
                          (admins &&
                            account.status === "ACTIVE" &&
                            accounts.data.filter((a) => a.status === "ACTIVE")
                              .length <= 1)
                        }
                        onClick={() => {
                          setError(null);
                          setRemove(account);
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
          icon={Users}
          title="A little quiet in this corner"
          text="No accounts match your search."
        />
      )}
      {edit && (
        <AccountForm
          account={edit}
          api={api}
          onClose={() => setEdit(null)}
          onSaved={() => {
            setEdit(null);
            accounts.reload();
          }}
        />
      )}
      {detail && (
        <AccountDetail id={detail} api={api} onClose={() => setDetail(null)} />
      )}
      {remove && (
        <Confirm
          title="Deactivate this account?"
          text={`${remove.username} will no longer be able to sign in. Their existing order and review records are preserved.`}
          action="Deactivate account"
          onClose={() => setRemove(null)}
          onConfirm={deleteAccount}
          busy={busy}
          error={error}
        />
      )}
    </>
  );
}
function AccountForm({ account, api, onClose, onSaved }) {
  const { notify, user, setUser } = useApp();
  const [values, setValues] = useState(account);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const creating = !account.id;
  async function submit(e) {
    e.preventDefault();
    if (busy) return;
    setError(null);
    if (
      creating &&
      (values.password !== values.confirmation ||
        !validPassword(values.password || ""))
    ) {
      setError(
        "Passwords must match, with at least 8 characters and at most 72 UTF-8 bytes.",
      );
      return;
    }
    setBusy(true);
    try {
      const result = creating
        ? await api.create({
            ...profilePayload(values),
            username: values.username,
            password: values.password,
          })
        : await api.update(account.id, profilePayload(values));
      if (result.id === user.id) setUser(result);
      notify(creating ? "Administrator created." : "Account updated.");
      onSaved();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title={creating ? "Welcome a new administrator" : "Edit account details"}
      onClose={onClose}
      busy={busy}
    >
      <form onSubmit={submit}>
        <fieldset disabled={busy}>
          {creating ? (
            <RegistrationFields values={values} setValues={setValues} />
          ) : (
            <ProfileFields values={values} setValues={setValues} />
          )}
          <InlineError error={error} />
          <Button type="submit" busy={busy}>
            {creating ? "Create administrator" : "Save changes"}
          </Button>
        </fieldset>
      </form>
    </Modal>
  );
}
function AccountDetail({ id, api, onClose }) {
  const account = useResource(() => api.get(id), [id]);
  return (
    <Modal title="Account details" onClose={onClose}>
      {account.loading ? (
        <Loading />
      ) : account.error ? (
        <ErrorState error={account.error} retry={account.reload} />
      ) : (
        <dl className="detail-list">
          {[
            ["Name", `${account.data.firstName} ${account.data.lastName}`],
            ["Username", account.data.username],
            ["Email", account.data.email],
            ["Phone", account.data.phone || "Not provided"],
            ["Status", account.data.status],
            ["Roles", account.data.roles.join(", ")],
          ].map(([label, value]) => (
            <div key={label}>
              <dt>{label}</dt>
              <dd>{value}</dd>
            </div>
          ))}
        </dl>
      )}
    </Modal>
  );
}
