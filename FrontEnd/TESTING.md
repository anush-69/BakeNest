# Verification report

All commands were executed in the current **FrontEnd** folder. Tests use isolated synthetic responses only; they do not create demonstration records in the application or contact MySQL.

| Check | Result |
|---|---|
| `npm.cmd install` | Completed; lockfile generated |
| `npm.cmd run build` | Passed; production `dist/` generated, admin chunks split |
| `npm.cmd run lint` | Passed, no errors or warnings |
| `npm.cmd test` | 18 focused tests passed |
| `npm.cmd audit --omit=dev` | No runtime dependency vulnerabilities reported |
| Full `npm.cmd audit` | 5 moderate findings in the ESLint/minimatch development dependency chain; no high/critical findings. Latest compatible ESLint was installed. No forced downgrade or incompatible transitive override was applied |
| Frontend HTTP | HTTP 200 at `http://127.0.0.1:5173/` |
| Route smoke checks | 23 paths returned HTTP 200 with the Vite app shell; confirms SPA fallbacks, not authenticated page behavior |
| Backend HTTP | `http://localhost:8080/api/products` unavailable; no live API/database tests claimed |
| Backend integrity | All 166 files in the backend directory matched their initial SHA-256 snapshot after implementation |
| Browser visual / mobile interaction QA | Attempted but blocked by computer-use safety checks: Chrome’s current URL could not be reliably determined. No screenshots, automated browser interactions or measured mobile layout results are claimed |

## Executed contract and behavior tests

- Backend product search parameter names and credential inclusion.
- CSRF retrieval before mutations, returned header name, refreshed tokens between writes.
- Order payload excludes authoritative prices/customer identity; response total is used.
- Failed state-changing requests are not silently replayed.
- Form-encoded session login; identity checked with `/users/me`.
- Spring `/login?error` handling, and rejection of a redirect without a valid session.
- CSRF-protected POST logout; failed logout cannot report success.
- Session-expiration event on protected 401 responses.
- Network errors, non-JSON API responses, and forbidden CSRF/auth responses.
- Review moderation uses `status` and `note`; review edit uses `rating` and `comment`.
- Account deletion handles HTTP 204 correctly.
- Order state progression and cancellation restrictions.
- Persisted basket validation, quantity limits and exclusion of extra fields.
- Password UTF-8 byte limit and actual backend role names.
- Account HTML patterns compile in the browser-standard Unicode `v` regex mode.
- Vite API/form path proxy and rewriting backend-origin login redirects.

## HTTP paths checked

`/`, `/shop`, `/shop/1`, `/login`, `/register`, `/admin/login`, `/admin`, `/admin/products`, `/admin/customers`, `/admin/admins`, `/admin/orders`, `/admin/reviews`, `/admin/bookings`, `/account`, `/account/orders`, `/account/orders/1`, `/account/reviews`, `/account/bookings`, `/account/bookings/1`, `/cart`, `/custom-cakes`, `/our-story`, and `/missing-page`.

## Verification still requiring the backend/browser

When the backend owner runs the application with suitable existing accounts and products:

1. Confirm username/password session login for CUSTOMER and ADMIN, cookie persistence, logout, invalid-password and expired-session behavior.
2. Register a customer; edit their profile and change password; confirm revoked sessions require sign-in.
3. Check database-backed categories, search, images and product detail reviews.
4. Place an order and compare its returned total against the server price snapshots. Change quantity only while pending, and cancel only while pending/confirmed.
5. Verify customer ownership and ADMIN access restrictions using separate accounts.
6. Exercise product/category/admin management and customer deactivation with designated test records.
7. Submit/edit a review, approve/reject it, and verify public visibility.
8. Visually inspect desktop, tablet and phone widths; keyboard focus, dialog focus trapping, mobile menus and table overflow. Responsive breakpoints and reduced-motion behavior are implemented but were not browser-verified in this session.

No backend source/build files were written and no MySQL schema/data changes were performed. Backend startup was deliberately left to its owner because the existing configuration can update the database on startup.
