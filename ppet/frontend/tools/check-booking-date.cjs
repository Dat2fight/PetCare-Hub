const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');

function load(file, imports, DateImpl = Date) {
  const code = ts.transpileModule(fs.readFileSync(path.join(__dirname, '..', file), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, target: ts.ScriptTarget.ES2020 }
  }).outputText;
  const exports = {};
  vm.runInNewContext(code, { exports, Date: DateImpl, Intl, require: name => {
    if (!(name in imports)) throw new Error(`Unexpected import: ${name}`);
    return imports[name];
  } });
  return exports;
}
const { getBusinessDate } = load('src/utils/businessDate.ts', {});
for (const [instant, expected] of [
  ['2026-11-05T19:00:00Z', '2026-11-06'], // 02:00 in Vietnam, still November 5 in UTC.
  ['2026-11-05T16:59:59Z', '2026-11-05'],
  ['2026-11-05T17:00:00Z', '2026-11-06'],
  ['2026-12-31T17:00:00Z', '2027-01-01']
]) assert.equal(getBusinessDate(new Date(instant)), expected);

const originalZone = process.env.TZ;
try {
  for (const zone of ['UTC', 'America/Los_Angeles', 'Asia/Tokyo']) {
    process.env.TZ = zone;
    class FixedDate extends Date { constructor(...args) { super(...(args.length ? args : ['2026-11-05T19:00:00Z'])); } }
    const utils = load('src/utils/businessDate.ts', {}, FixedDate);
    let stateIndex = 0;
    const jsx = { jsx: (type, props) => ({ type, props }), jsxs: (type, props) => ({ type, props }) };
    const { BookingPage } = load('src/pages/services/BookingPage.tsx', {
      'react/jsx-runtime': jsx,
      react: { useEffect: () => {}, useState: initial => {
        const value = stateIndex++ === 0 ? { id: 1, name: 'Test service', price: 100 } : initial === true ? false : initial;
        return [value, () => {}];
      } },
      'react-router-dom': { useSearchParams: () => [{ get: () => '1' }], useNavigate: () => () => {} },
      '../../components/common/Button': { Button: 'button' },
      'react-icons/fi': { FiCalendar: 'span', FiClock: 'span' },
      '../../services/serviceService': {}, '../../services/bookingService': {},
      '../../contexts/AuthContext': { useAuth: () => ({ isAuthenticated: true, user: { roles: ['CUSTOMER'] } }) },
      'react-hot-toast': {}, '../../services/petManagementService': {},
      '../../components/pets/QuickAddPetModal': {}, '../../utils/businessDate': utils
    }, FixedDate);
    function walk(node) { return node && typeof node === 'object' ? [node, ...[node.props?.children].flat(Infinity).flatMap(walk)] : []; }
    const input = walk(BookingPage()).find(node => node.type === 'input' && node.props.type === 'date');
    assert.ok(input); assert.equal(input.props.value, '2026-11-06'); assert.equal(input.props.min, '2026-11-06');
  }
} finally { if (originalZone === undefined) delete process.env.TZ; else process.env.TZ = originalZone; }
console.log('PASS: Vietnam 02:00 Nov 6 gives Nov 6 for both default date and min, independent of browser timezone; midnight/year boundaries pass.');
