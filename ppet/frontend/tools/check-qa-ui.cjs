/* global localStorage, document */
const { chromium } = require(process.env.QA_PLAYWRIGHT);
const assert = require('node:assert/strict');
const base = 'http://127.0.0.1:5175/PetCare-Hub';
(async () => {
  const browser = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  try {
    const context = await browser.newContext({ viewport: { width: 360, height: 900 } });
    let signedIn = false; let pets = []; let failSlots = false; let petRequests = 0;
    await context.route('**/*', route => {
      const url = new URL(route.request().url());
      if (url.origin !== new URL(base).origin) return route.abort();
      if (!url.pathname.startsWith('/api/')) return route.continue();
      const path = url.pathname.replace('/api/v1', ''); let data = []; let status = 200;
      if (path === '/auth/me') { status = signedIn ? 200 : 401; data = { id: 1, firstName: 'qa_Khách', lastName: 'qa_Test', roles: ['CUSTOMER'] }; }
      else if (path === '/auth/login') { status = 401; data = { message: 'Thông tin đăng nhập không chính xác' }; }
      else if (path === '/pets/my-pets') {
        if (route.request().method() === 'POST') { const body = route.request().postDataJSON(); data = { ...body, id: 5, speciesName: 'Chó', availabilityStatus: 'UNAVAILABLE' }; pets = [data]; status = 201; }
        else data = pets;
      } else if (path.startsWith('/pets/')) { petRequests++; status = 404; data = { message: 'Không tìm thấy thú cưng' }; }
      else if (path === '/species') data = [{ id: 42, name: 'Chó' }, { id: 75, name: 'Mèo' }];
      else if (path === '/services/1') data = { id: 1, name: 'qa_Khám', price: 100000, durationMinutes: 30 };
      else if (path === '/appointments/slots') { if (failSlots) return route.abort(); data = ['2026-10-28T10:00:00']; }
      else if (path === '/products' || path === '/pets' || path === '/orders') data = { content: [], totalPages: 0 };
      else if (path === '/notifications/unread-count') data = { count: 0 };
      return route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(data) });
    });
    const page = await context.newPage();
    await page.goto(base + '/', { waitUntil: 'networkidle' });
    const menu = page.getByRole('button', { name: 'Mở menu', exact: true });
    await menu.click(); assert.equal(await page.getByRole('button', { name: 'Đóng menu' }).getAttribute('aria-expanded'), 'true');
    assert.equal(await page.locator('#mobile-navigation').evaluate(el => el.contains(document.activeElement)), true);
    for (const name of ['Đăng nhập', 'Đăng ký']) { const box = await page.locator('#mobile-navigation').getByRole('link', { name, exact: true }).boundingBox(); assert.ok(box.height >= 44 && box.width >= 44); }
    await page.keyboard.press('Escape'); assert.equal(await menu.getAttribute('aria-expanded'), 'false'); assert.equal(await menu.evaluate(el => el === document.activeElement), true);
    await menu.click(); await page.locator('#mobile-navigation').getByRole('link', { name: 'Sản phẩm', exact: true }).click();
    assert.equal(await page.locator('#mobile-navigation').count(), 0);
    await page.goto(base + '/pets/not-a-number', { waitUntil: 'networkidle' });
    assert.ok((await page.locator('main').innerText()).includes('Không tìm thấy thú cưng')); assert.equal(petRequests, 0);
    await page.goto(base + '/pets/999999', { waitUntil: 'networkidle' });
    assert.ok((await page.locator('main').innerText()).includes('Không tìm thấy thú cưng')); assert.equal(petRequests, 1);
    await page.goto(base + '/my-pets', { waitUntil: 'networkidle' }); assert.ok(page.url().endsWith('/login'));
    await page.getByLabel('Email hoặc số điện thoại').fill('qa_invalid'); await page.getByLabel('Mật khẩu', { exact: true }).fill('qa_invalid');
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).last().click();
    await page.getByText('Thông tin đăng nhập không chính xác', { exact: true }).waitFor();
    assert.equal(await page.getByLabel('Email hoặc số điện thoại').inputValue(), 'qa_invalid');
    assert.equal(await page.locator('a[href="#"]').count(), 0);
    signedIn = true;
    await page.evaluate(() => localStorage.setItem('accessToken', 'qa_browser_fixture'));
    await page.goto(base + '/services/booking?serviceId=1', { waitUntil: 'networkidle' });
    await page.getByRole('button', { name: '10:00', exact: true }).click();
    await page.getByRole('button', { name: 'Thêm nhanh thú cưng' }).click();
    const dialog = page.getByRole('dialog'); await dialog.waitFor();
    await dialog.getByLabel('Tên thú cưng').fill('qa_Bông'); await dialog.getByLabel('Loài', { exact: true }).selectOption('42'); await dialog.getByLabel('Tuổi (năm)').fill('2');
    await dialog.getByRole('button', { name: 'Lưu thú cưng' }).click(); await dialog.waitFor({ state: 'detached' });
    const confirm = page.getByRole('button', { name: 'Xác nhận đặt lịch' }); await confirm.waitFor(); assert.equal(await confirm.isEnabled(), true);
    assert.ok((await page.locator('main').innerText()).includes('qa_Bông')); assert.ok((await page.locator('main').innerText()).includes('10:00'));
    failSlots = true;
    await page.locator('input[type="date"]').fill('2026-10-29');
    await page.getByText('Không tải được giờ trống.', { exact: false }).waitFor();
    assert.equal(await page.getByRole('button', { name: '10:00', exact: true }).count(), 0);
    console.log('PASS: mobile menu/focus/Esc/navigation/touch targets; invalid pet IDs and 404; protected redirect; login 401 retains form; quick pet auto-selection retains slot; slot failure has no mock.');
  } finally { await browser.close(); }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
