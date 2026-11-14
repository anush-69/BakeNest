# BakeNest frontend

A React/JavaScript storefront and administrator studio for **BakeNest — Bakery Orders and Custom Cake Booking Platform**, SLIIT SE1020. Built directly in this FrontEnd directory, separate from the existing read-only Spring Boot project.

## Open the application

The development server was started and its HTTP response verified at:

| Page | Local URL |
|---|---|
| Customer website | http://127.0.0.1:5173/ |
| Bakery catalogue | http://127.0.0.1:5173/shop |
| Customer login | http://127.0.0.1:5173/login |
| Admin login | http://127.0.0.1:5173/admin/login |
| Admin dashboard | http://127.0.0.1:5173/admin |
| Configured backend API (offline during implementation) | http://localhost:8080/api |

The admin dashboard requires a real ADMIN session. There are no built-in demonstration credentials or fake records. If the backend is offline, functional data areas display connection errors with retry controls.

## Start the frontend

Run in this folder using PowerShell:

```powershell
npm.cmd install
npm.cmd run dev
```

`npm.cmd` avoids the machine's PowerShell script-execution restriction on `npm.ps1`. Port 5173 is strict: Vite reports a conflict rather than silently changing the advertised address. Use the same hostname consistently so session cookies stay on the same origin.

## Connect your backend

The actual Maven root is `..\BakeNest\bake-nest`. The inspected project requires Java 25 and its configured MySQL database.

When you are ready to run the backend yourself, use another terminal:

```powershell
Set-Location '..\BakeNest\bake-nest'
.\mvnw.cmd spring-boot:run
```

This command is provided for the backend owner; it was not executed during frontend implementation. Starting the existing application may update MySQL because of its current Hibernate and import configuration.

The frontend `.env` contains:

```dotenv
VITE_API_BASE_URL=/api
BACKEND_ORIGIN=http://localhost:8080
```

Vite proxies API requests and Spring form-login/logout requests. Restart Vite after editing `.env`. Keep `VITE_API_BASE_URL=/api` for the supplied setup. See `BACKEND_INTEGRATION_NOTES.md` before deploying to separate origins.

## Included pages and behavior

- Editorial homepage, catalogue with backend search/categories and local price/name sorting, product details with ratings and approved reviews, story page.
- Customer registration and session login; profile edit, password change, logout.
- Persistent basket of IDs/quantities only; checkout with estimated subtotal and backend-confirmed order total.
- Order history/details, pending-order quantity edits, cancellation when permitted, status progression display.
- Review submission, customer history, edit/delete; edits return to moderation.
- Separate ADMIN route guard and responsive studio layout; overview using real lists, order counts and completed-order value.
- Product create/edit/detail/availability/soft delete; category creation.
- Customer search/detail/edit/deactivation; administrator create/search/detail/edit/deactivation.
- All orders, order details, permitted sequential status updates/cancellation; review detail/moderation/delete.
- Explicitly unavailable custom cake booking, customer booking history/details, and admin booking pages. No booking endpoint exists yet.
- Keyboard focus styles, skip links, labeled forms, password visibility, native focus-trapping dialogs, reduced-motion styles, mobile navigation, table scrolling, loading/error/empty states, and toast notifications.

## Technology and structure

React 19, Vite 7, JavaScript/JSX, Tailwind CSS 4 via its Vite plugin, React Router 7, Lucide icons, native Fetch and purpose-built responsive CSS. Admin pages are lazy loaded. No unnecessary animation or state-management library.

```text
src/
  api/                 # Verified endpoint wrappers, session/CSRF client
  components/common/   # Fields, buttons, states, dialogs, image fallback
  components/forms/    # Backend-aligned account fields
  components/layout/   # Storefront, account, admin layouts and route guards
  components/products/ # Product card
  context/             # Session, basket, notifications
  hooks/               # Resource loading and retry
  lib/                 # Currency/date/role/order rules
  pages/public/        # Home, shop, details, auth, story, custom-cake placeholder
  pages/customer/      # Profile, cart, orders, reviews
  pages/admin/         # Dashboard, accounts, products, reviews
  styles/              # Brand system and responsive styles
public/images/         # Downloaded editorial imagery
tests/                 # Contract, security/error-path and proxy tests
```

All endpoint methods, payload fields and permissions are documented in `API_MAPPING.md`. No backend files or MySQL records were modified.

## Checks and production build

```powershell
npm.cmd test
npm.cmd run lint
npm.cmd run build
npm.cmd run preview
```

Production assets are generated in `dist/`. Preview is configured for port 4173 but is not started automatically. Production hosting must provide SPA fallback and same-origin backend proxy routes; copying `dist/` alone to a static host does not create a Spring session proxy.

## Images and verification

See `ASSET_SOURCES.md` for downloaded images, license/source URLs and font/icon details. See `TESTING.md` for executed checks and outstanding backend/browser verification. No stock imagery, fake statistics or demo reviews are presented as database records.
