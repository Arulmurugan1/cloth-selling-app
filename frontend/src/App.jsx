import React, { useEffect, useMemo, useState } from "react";
import { api, uploadCsv } from "./api";

const MENU = {
  CUSTOMER: ["Dashboard", "My Orders", "New Order"],
  ADMIN: [
    "Dashboard",
    "Customers",
    "Orders",
    "Delivery Status",
    "New Order",
    "New Product",
    "New Product Batch",
    "Active Users"
  ],
  SUPER_ADMIN: [
    "Dashboard",
    "Customers",
    "Orders",
    "Delivery Status",
    "New Order",
    "New Product",
    "New Product Batch",
    "Active Users"
  ]
};

function LogoTransition({ show }) {
  return show ? <div className="route-logo">ClothApp</div> : null;
}

function AuthForms({ onLogin }) {
  const [mode, setMode] = useState("login");
  const [login, setLogin] = useState({ login: "", password: "" });
  const [register, setRegister] = useState({ fullName: "", email: "", mobile: "", password: "", dob: "" });
  const [error, setError] = useState("");

  const submitLogin = async (e) => {
    e.preventDefault();
    setError("");
    try {
      const data = await api("/auth/login", "POST", login);
      onLogin(data);
    } catch (err) {
      setError(err.message);
    }
  };

  const submitRegister = async (e) => {
    e.preventDefault();
    setError("");
    try {
      await api("/auth/register", "POST", register);
      setMode("login");
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <h1>Cloth Selling App</h1>
        <p>Stylish commerce for Admin, Super Admin and Customers.</p>
        <div className="auth-switch">
          <button className={mode === "login" ? "active" : ""} onClick={() => setMode("login")}>Login</button>
          <button className={mode === "register" ? "active" : ""} onClick={() => setMode("register")}>Register</button>
        </div>
        {mode === "login" && (
          <form onSubmit={submitLogin}>
            <input placeholder="Email or mobile" value={login.login} onChange={(e) => setLogin({ ...login, login: e.target.value })} required />
            <input type="password" placeholder="Password (4-12)" value={login.password} onChange={(e) => setLogin({ ...login, password: e.target.value })} required minLength={4} maxLength={12} />
            <button type="submit">Sign In</button>
          </form>
        )}
        {mode === "register" && (
          <form onSubmit={submitRegister}>
            <input placeholder="Full name" value={register.fullName} onChange={(e) => setRegister({ ...register, fullName: e.target.value })} required />
            <input placeholder="Email" type="email" value={register.email} onChange={(e) => setRegister({ ...register, email: e.target.value })} required />
            <input placeholder="Mobile" value={register.mobile} onChange={(e) => setRegister({ ...register, mobile: e.target.value })} required />
            <input type="password" placeholder="Password (4-12)" value={register.password} onChange={(e) => setRegister({ ...register, password: e.target.value })} required minLength={4} maxLength={12} />
            <input type="date" value={register.dob} onChange={(e) => setRegister({ ...register, dob: e.target.value })} required />
            <button type="submit">Create Account</button>
          </form>
        )}
        {error && <div className="error">{error}</div>}
      </div>
    </div>
  );
}

function Stat({ label, value, icon }) {
  return (
    <div className="stat-card">
      <span>{icon}</span>
      <strong>{value}</strong>
      <small>{label}</small>
    </div>
  );
}

function DataTable({ title, rows, columns }) {
  const [query, setQuery] = useState("");
  const filtered = rows.filter((r) => JSON.stringify(r).toLowerCase().includes(query.toLowerCase()));
  return (
    <section className="panel">
      <div className="panel-top">
        <h3>{title}</h3>
        <input placeholder="Search" value={query} onChange={(e) => setQuery(e.target.value)} />
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>{columns.map((c) => <th key={c}>{c}</th>)}</tr>
          </thead>
          <tbody>
            {filtered.map((r, i) => (
              <tr key={i}>
                {columns.map((c) => <td key={c}>{String(r[c] ?? "")}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

export default function App() {
  const [auth, setAuth] = useState(null);
  const [page, setPage] = useState("Dashboard");
  const [logoFx, setLogoFx] = useState(false);
  const [dashboard, setDashboard] = useState({ totalOrders: 0, customers: 0, deliveredToday: 0, yetToShip: 0 });
  const [orders, setOrders] = useState([]);
  const [products, setProducts] = useState([]);
  const [users, setUsers] = useState([]);
  const [file, setFile] = useState(null);

  const token = auth?.token;
  const role = auth?.user?.role || "CUSTOMER";
  const menu = useMemo(() => MENU[role] || MENU.CUSTOMER, [role]);

  useEffect(() => {
    if (!token) return;
    api("/api/orders/dashboard/summary", "GET", null, token).then(setDashboard).catch(() => {});
    api("/api/orders", "GET", null, token).then(setOrders).catch(() => {});
    api("/api/products", "GET", null, token).then(setProducts).catch(() => {});
    if (role === "ADMIN" || role === "SUPER_ADMIN") {
      api("/api/users", "GET", null, token).then(setUsers).catch(() => {});
    }
  }, [token, role]);

  const navigate = (p) => {
    setLogoFx(true);
    setTimeout(() => {
      setPage(p);
      setLogoFx(false);
    }, 350);
  };

  if (!auth) {
    return <AuthForms onLogin={setAuth} />;
  }

  return (
    <div className="app-shell">
      <LogoTransition show={logoFx} />
      <header>
        <div className="brand">ClothApp</div>
        <div className="who">Logged in: {auth.user.fullName}</div>
        <button
          onClick={async () => {
            try {
              await api("/auth/logout", "POST", {}, token);
            } catch {
              // ignore logout errors
            }
            setAuth(null);
          }}
        >Sign Out</button>
      </header>
      <div className="layout">
        <aside>
          {menu.map((m) => (
            <button key={m} className={m === page ? "active" : ""} onClick={() => navigate(m)}>{m}</button>
          ))}
        </aside>
        <main>
          {page === "Dashboard" && (
            <section className="dash-grid">
              <Stat label="Total Orders" value={dashboard.totalOrders} icon="🛒" />
              <Stat label="Customers" value={dashboard.customers} icon="👥" />
              <Stat label="Delivered Today" value={dashboard.deliveredToday} icon="✅" />
              <Stat label="Yet To Ship" value={dashboard.yetToShip} icon="🚚" />
            </section>
          )}
          {page === "My Orders" && <DataTable title="My Orders" rows={orders} columns={["id", "status", "orderTotal", "createdAt"]} />}
          {page === "Orders" && <DataTable title="Orders" rows={orders} columns={["id", "customerName", "status", "orderTotal", "updatedAt"]} />}
          {page === "Customers" && <DataTable title="Customers" rows={orders} columns={["customerId", "customerName", "customerEmail"]} />}
          {page === "Delivery Status" && <DataTable title="Delivery Status" rows={orders} columns={["id", "status", "updatedAt"]} />}
          {page === "New Product" && <DataTable title="Products" rows={products} columns={["sku", "name", "price", "stockQty", "defectiveQty", "active"]} />}
          {page === "Active Users" && <DataTable title="Active Users" rows={users} columns={["fullName", "email", "mobile", "role", "active"]} />}
          {page === "New Product Batch" && (
            <section className="panel">
              <div className="panel-top"><h3>Batch Product Upload</h3></div>
              <a href="/api/batch/products/template" target="_blank" rel="noreferrer" className="download-link">Download Sample Template</a>
              <input type="file" accept=".csv" onChange={(e) => setFile(e.target.files?.[0] || null)} />
              <button
                onClick={async () => {
                  if (!file) return;
                  await uploadCsv("/api/batch/products/upload", file, token);
                  const latest = await api("/api/products", "GET", null, token);
                  setProducts(latest);
                }}
              >Upload CSV</button>
            </section>
          )}
          {page === "New Order" && (
            <section className="panel">
              <div className="panel-top"><h3>Create New Order</h3></div>
              <p>Use API payload in this simple starter UI: {"{"}"items":[{"{"}"sku":"SKU-1001","qty":2{"}"}]{"}"}</p>
            </section>
          )}
        </main>
      </div>
    </div>
  );
}
