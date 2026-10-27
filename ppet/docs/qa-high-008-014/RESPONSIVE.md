# Responsive Chrome — 2026-10-27

API fixture intercepted in browser; no dev database access. Values are documentElement.scrollWidth. Guest protected pages redirect to login. Customer pages render actual page components with fixture data. All 208 cases passed; page errors and empty main content fail the runner.

| Role | Page | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
|---|---|---|---|---|---|---|---|---|---|
| ADMIN | / | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| CUSTOMER | / | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| CUSTOMER | /cart | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| CUSTOMER | /checkout | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| CUSTOMER | /login | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| CUSTOMER | /orders | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| CUSTOMER | /pets | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| CUSTOMER | /pets/1 | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| CUSTOMER | /products | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| CUSTOMER | /products/1 | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| CUSTOMER | /register | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| CUSTOMER | /services/booking?serviceId=1 | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| GUEST | / | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| GUEST | /cart | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| GUEST | /checkout | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| GUEST | /login | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| GUEST | /orders | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| GUEST | /pets | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| GUEST | /pets/1 | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| GUEST | /products | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| GUEST | /products/1 | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| GUEST | /register | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| GUEST | /services/booking?serviceId=1 | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| SALES_STAFF | / | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| VETERINARIAN | / | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |
| WAREHOUSE_STAFF | / | 320 | 360 | 375 | 390 | 412 | 768 | 1366 | 1920 |

Before: 134/208 cases overflowed. At 360px: guest 411px, authenticated long-name fixture 528px; authenticated tablet 768px reached 852px. After Header fix, checkout at 320px remained 334px; constraining its payment select removed the final overflow.
