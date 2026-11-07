const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');

// Exercise the real provider and interceptors without a browser or added dependencies.
const jsx = { jsx: (type, props) => ({ type, props }), jsxs: (type, props) => ({ type, props }) };
function load(file, imports, globals) {
  const source = fs.readFileSync(path.join(__dirname, '..', file), 'utf8').replaceAll('import.meta.env', 'testEnv');
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, target: ts.ScriptTarget.ES2020 } }).outputText;
  const exports = {};
  vm.runInNewContext(code, { exports, ...globals, require: name => {
    if (name === 'react/jsx-runtime') return jsx;
    if (!(name in imports)) throw new Error(`Unexpected import: ${name}`);
    return imports[name];
  } });
  return exports;
}
function harness(token = 'existing-token', pathname = '/PetCare-Hub/checkout') {
  const stored = new Map(token ? [['accessToken', token], ['token', token], ['refreshToken', 'legacy-refresh']] : []);
  const removed = [], redirects = [], requests = [], hooks = [], effects = [];
  let cursor = 0, onRequest, onFailure;
  const storage = { getItem: key => stored.get(key) ?? null, setItem: (key, value) => stored.set(key, value), removeItem: key => { removed.push(key); stored.delete(key); } };
  const client = {
    interceptors: { request: { use: callback => { onRequest = callback; } }, response: { use: (_, callback) => { onFailure = callback; } } },
    get: (url, options) => new Promise((resolve, reject) => {
      const config = onRequest({ url, headers: {}, ...options });
      requests.push({ config, succeed: data => resolve({ data }), fail: async status => {
        const error = { isAxiosError: true, config, ...(status ? { response: { status } } : {}) };
        try { await onFailure(error); } catch (failure) { reject(failure); }
      } });
    })
  };
  const axios = { default: { create: () => client }, isAxiosError: error => error?.isAxiosError === true };
  const globals = { localStorage: storage, sessionStorage: { setItem: () => {} }, AbortController,
    testEnv: { BASE_URL: '/PetCare-Hub/' }, window: { location: { pathname, search: '', replace: url => redirects.push(url) } } };
  const actualClient = load('src/api/client.ts', { axios }, globals).default;
  const react = {
    createContext: () => ({ Provider: 'provider' }),
    useState: initial => {
      const index = cursor++;
      if (!(index in hooks)) hooks[index] = initial;
      return [hooks[index], value => { hooks[index] = typeof value === 'function' ? value(hooks[index]) : value; }];
    },
    useRef: initial => { const index = cursor++; return hooks[index] ??= { current: initial }; },
    useEffect: (callback, deps) => {
      const index = cursor++, previous = hooks[index];
      if (!previous || deps.some((value, i) => value !== previous.deps[i])) effects.push(() => {
        previous?.cleanup?.(); hooks[index] = { deps, cleanup: callback() };
      });
    }
  };
  const { AuthProvider } = load('src/contexts/AuthContext.tsx', { react, axios, '../api/client': { default: actualClient } }, globals);
  const render = () => { cursor = 0; const tree = AuthProvider({ children: 'page' }); while (effects.length) effects.shift()(); return tree.props.value; };
  const settle = async () => { await new Promise(setImmediate); return render(); };
  const guard = () => load('src/components/common/ProtectedRoute.tsx', {
    'react-router-dom': { Navigate: 'navigate', useLocation: () => ({ pathname: '/checkout' }) },
    '../../contexts/AuthContext': { useAuth: render }
  }, {}).default({ children: 'private-content' });
  render();
  return { stored, removed, redirects, requests, render, settle, guard };
}

(async () => {
  const noToken = harness(null); assert.equal(noToken.render().isLoading, false); assert.equal(noToken.requests.length, 0);
  for (const status of [undefined, 500, 503, 403]) {
    const h = harness(); await h.requests[0].fail(status); const failed = await h.settle();
    assert.equal(h.stored.get('accessToken'), 'existing-token'); assert.equal(h.removed.length, 0); assert.equal(h.redirects.length, 0);
    assert.equal(failed.isAuthenticated, false); assert.equal(failed.isLoading, false); assert.ok(failed.sessionError);
    assert.notEqual(h.guard().type, 'navigate'); assert.equal(h.requests.length, 1);
    failed.retrySession(); h.render(); assert.equal(h.requests.length, 2);
    h.requests[1].succeed({ id: 1, roles: ['CUSTOMER'] }); const restored = await h.settle();
    assert.equal(restored.isAuthenticated, true); assert.equal(restored.sessionError, null); assert.equal(restored.isLoading, false);
  }
  for (const pathname of ['/PetCare-Hub/checkout', '/PetCare-Hub/login']) {
    const h = harness('existing-token', pathname); await h.requests[0].fail(401); const failed = await h.settle();
    assert.equal(h.stored.size, 0); assert.equal(h.removed.length, 3); assert.equal(failed.sessionError, null);
    assert.equal(failed.isAuthenticated, false); assert.equal(failed.isLoading, false);
    assert.equal(h.redirects.length, pathname.endsWith('/login') ? 0 : 1); assert.equal(h.requests.length, 1);
  }
  const logout = harness(); logout.render().logout(); logout.requests[0].succeed({ id: 1 });
  assert.equal((await logout.settle()).user, null); assert.equal(logout.stored.size, 0);
  const login = harness(); login.render().login('new-token', { id: 2 }); login.requests[0].succeed({ id: 1 });
  assert.equal((await login.settle()).user.id, 2); assert.equal(login.stored.get('accessToken'), 'new-token');
  console.log('PASS: network/5xx/403 preserve tokens; explicit retry restores session; 401 cleanup occurs once; protected route waits; stale restore cannot overwrite login/logout.');
})().catch(error => { console.error(error); process.exitCode = 1; });
