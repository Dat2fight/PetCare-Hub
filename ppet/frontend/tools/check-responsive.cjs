// Run with QA_PLAYWRIGHT pointing at a temporary Playwright install outside the repository.
// Browser layout fixtures only: all API traffic is intercepted; no database is contacted.
/* global localStorage, document, innerWidth */
const { chromium } = require(process.env.QA_PLAYWRIGHT);
const { writeFileSync } = require('node:fs');
const base = 'http://127.0.0.1:5175/PetCare-Hub';
const widths = [320, 360, 375, 390, 412, 768, 1366, 1920];
const pages = ['/', '/products', '/products/1', '/pets', '/pets/1', '/login', '/register', '/services/booking?serviceId=1', '/checkout', '/cart', '/orders'];
const pet = { id: 1, name: 'qa_Bông', speciesName: 'Chó', speciesId: 1, age: 2, price: 9500000, availabilityStatus: 'AVAILABLE' };
const product = { id: 1, name: 'qa_Thức ăn thú cưng', price: 150000, stockQuantity: 5, active: true, categoryId: 1, categoryName: 'qa_Thức ăn' };
const service = { id: 1, name: 'qa_Khám thú cưng', price: 100000, durationMinutes: 30, active: true };
(async () => {
  const browser = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  const results = [];
  try {
    for (const role of ['GUEST', 'CUSTOMER', 'ADMIN', 'SALES_STAFF', 'WAREHOUSE_STAFF', 'VETERINARIAN']) {
      const context = await browser.newContext();
      await context.addInitScript(authenticated => { if (authenticated) localStorage.setItem('accessToken', 'qa_browser_fixture'); }, role !== 'GUEST');
      await context.route('**/*', route => {
        const url = new URL(route.request().url());
        if (url.origin !== new URL(base).origin) return route.abort();
        if (!url.pathname.startsWith('/api/')) return route.continue();
        const path = url.pathname.replace('/api/v1', '');
        let data = [];
        if (path === '/auth/me') data = { id: 1, firstName: 'qa_Tên khách hàng rất dài', lastName: 'qa_Test', email: 'qa_test@example.test', roles: [role] };
        else if (path === '/pets/my-pets') data = [pet];
        else if (path === '/pets/1') data = pet;
        else if (path === '/pets') data = { content: [pet], totalPages: 1, totalElements: 1 };
        else if (path === '/products/1') data = product;
        else if (path === '/products') data = { content: [product], totalPages: 1, totalElements: 1 };
        else if (path === '/services/1') data = service;
        else if (path === '/services') data = [service];
        else if (path === '/appointments/slots') data = ['2026-10-28T10:00:00'];
        else if (path === '/cart') data = { items: [{ id: 1, productId: 1, productName: product.name, price: product.price, quantity: 1 }] };
        else if (path === '/notifications/unread-count') data = { count: 12345 };
        else if (path === '/orders') data = { content: [], totalPages: 0 };
        return route.fulfill({ contentType: 'application/json', body: JSON.stringify(data) });
      });
      const page = await context.newPage();
      let errors = [];
      page.on('pageerror', error => errors.push(error.message));
      for (const width of widths) {
        await page.setViewportSize({ width, height: 900 });
        for (const path of ['GUEST', 'CUSTOMER'].includes(role) ? pages : ['/']) {
          errors = [];
          await page.goto(base + path, { waitUntil: 'networkidle' });
          if (errors.length || !await page.locator('main').innerText()) throw new Error(`Page failed to render: ${role} ${path}: ${errors.join(', ')}`);
          results.push(await page.evaluate(({ role, path }) => ({ role, path, width: innerWidth, scrollWidth: document.documentElement.scrollWidth,
            overflow: [...document.querySelectorAll('body *')].filter(el => { const r = el.getBoundingClientRect(); return r.width > 0 && (r.right > innerWidth + 1 || r.left < -1); }).slice(0, 8).map(el => ({ tag: el.tagName, cls: el.className })) }), { role, path }));
        }
      }
      await context.close();
    }
    writeFileSync(process.argv[2], JSON.stringify(results, null, 2));
    const failures = results.filter(r => r.scrollWidth > r.width);
    console.log(JSON.stringify({ cases: results.length, failures: failures.map(({ role, path, width, scrollWidth }) => ({ role, path, width, scrollWidth })) }));
    process.exitCode = failures.length ? 1 : 0;
  } finally { await browser.close(); }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
