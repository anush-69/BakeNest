import { Link } from "react-router-dom";
import {
  ArrowUpRight,
  Users,
  Package,
  ShoppingBag,
  MessageSquare,
  Clock,
  Banknote,
  Plus,
  Sun,
} from "lucide-react";
import { userApi } from "../../api/userApi";
import { productApi } from "../../api/productApi";
import { orderApi } from "../../api/orderApi";
import { reviewApi } from "../../api/reviewApi";
import { useResource } from "../../hooks/useResource";
import { useApp } from "../../context/AppContext";
import { money, hasRole, date } from "../../lib/format";
import {
  PageHeading,
  Loading,
  Empty,
  Button,
  ButtonLink,
  Badge,
} from "../../components/common/UI";
import { OrdersTable } from "../customer/Orders";

export default function Dashboard() {
  const { user } = useApp();
  const data = useResource(async () => {
    const results = await Promise.allSettled([
      userApi.list(),
      productApi.all(),
      orderApi.list(),
      reviewApi.all(),
    ]);
    return Object.fromEntries(
      ["customers", "products", "orders", "reviews"].map((key, i) => [
        key,
        results[i].status === "fulfilled"
          ? { value: results[i].value }
          : { error: results[i].reason },
      ]),
    );
  });
  if (data.loading) return <Loading label="Gathering your bakery’s latest…" />;
  const results = data.data || {};
  const customers = results.customers?.value?.filter(
    (account) => hasRole(account, "CUSTOMER") && !hasRole(account, "ADMIN"),
  );
  const products = results.products?.value;
  const orders = results.orders?.value;
  const reviews = results.reviews?.value;
  const stats = [
    [Users, "Total customers", customers?.length, "/admin/customers"],
    [Package, "Total products", products?.length, "/admin/products"],
    [ShoppingBag, "Total orders", orders?.length, "/admin/orders"],
    [
      Clock,
      "Pending orders",
      orders?.filter((o) => o.status === "PENDING").length,
      "/admin/orders",
    ],
    [MessageSquare, "Total reviews", reviews?.length, "/admin/reviews"],
    [
      Banknote,
      "Completed order revenue",
      orders
        ? money(
            orders
              .filter((o) => o.status === "COMPLETED")
              .reduce((sum, o) => sum + Number(o.total), 0),
          )
        : undefined,
      "/admin/orders",
    ],
  ];
  const recent = orders
    ? [...orders].sort((a, b) => b.id - a.id).slice(0, 5)
    : null;
  return (
    <>
      <PageHeading
        eyebrow="YOUR BAKERY, AT A GLANCE"
        title={`Hello, ${user.firstName}.`}
        text="Here’s what’s happening in your little corner of the bakery."
      >
        <ButtonLink to="/admin/products?new=1">
          <Plus size={17} />
          Add product
        </ButtonLink>
      </PageHeading>
      <div className="dashboard-welcome">
        <div>
          <Sun size={25} strokeWidth={1.3} />
          <span>
            <strong>A fresh day. Sweet possibilities.</strong>
            <p>Let’s make every order a lovely one.</p>
          </span>
        </div>
        <span>{date(new Date().toISOString())}</span>
      </div>
      <div className="stats-grid">
        {stats.map(([Icon, label, value, to]) => (
          <Link className="stat-card" key={label} to={to}>
            <div>
              <span className="stat-icon">
                <Icon size={20} />
              </span>
              <ArrowUpRight size={16} />
            </div>
            <p>{label}</p>
            <strong>{value ?? "Unavailable"}</strong>
            <small>
              {value === undefined
                ? "Service could not be reached"
                : "From bakery records"}
            </small>
          </Link>
        ))}
      </div>
      {Object.values(results).some((result) => result.error) && (
        <div className="inline-error">
          Some bakery data could not be loaded.
          <Button variant="small secondary" onClick={data.reload}>
            Retry
          </Button>
        </div>
      )}
      <div className="dashboard-grid">
        <section className="dashboard-panel">
          <div className="panel-heading">
            <h2>Recent orders</h2>
            <Link to="/admin/orders">
              View all
              <ArrowUpRight size={15} />
            </Link>
          </div>
          {recent?.length ? (
            <OrdersTable orders={recent} admin />
          ) : (
            <Empty
              title={
                recent ? "Your next order starts here" : "Orders unavailable"
              }
              text={
                recent
                  ? "New orders will appear as customers place them."
                  : results.orders?.error?.message
              }
            />
          )}
        </section>
        <section className="dashboard-panel">
          <div className="panel-heading">
            <h2>Order snapshot</h2>
            <ShoppingBag size={18} />
          </div>
          {orders ? (
            <div className="status-chart">
              {[
                "PENDING",
                "CONFIRMED",
                "PREPARING",
                "READY",
                "COMPLETED",
                "CANCELLED",
              ].map((status) => {
                const count = orders.filter((o) => o.status === status).length;
                return (
                  <div key={status}>
                    <div>
                      <Badge status={status} />
                      <strong>{count}</strong>
                    </div>
                    <div className="chart-track">
                      <span
                        style={{
                          width: `${orders.length ? (count / orders.length) * 100 : 0}%`,
                        }}
                      />
                    </div>
                  </div>
                );
              })}
              <p className="small muted">
                Distribution of all recorded orders.
              </p>
            </div>
          ) : (
            <Empty
              title="No snapshot available"
              text="Reconnect to load order activity."
            />
          )}
        </section>
      </div>
      <div className="studio-note">
        <span>✦</span>
        <p>Behind every lovely bakery is a little thoughtful care.</p>
        <Link to="/">
          Visit your storefront
          <ArrowUpRight size={16} />
        </Link>
      </div>
    </>
  );
}
