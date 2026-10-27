// Regression probe: ESLint must reject a real unused variable under src, then clean up.
const { writeFileSync, unlinkSync } = require('node:fs');
const { spawnSync } = require('node:child_process');
const { resolve } = require('node:path');
const file = resolve(__dirname, '../src/qa-lint-probe.ts');
try {
  writeFileSync(file, 'const qaUnusedVariable = 1;\nexport {};\n', { flag: 'wx' });
  const result = spawnSync(process.execPath, [resolve(__dirname, '../node_modules/eslint/bin/eslint.js'), file], { encoding: 'utf8' });
  if (result.status !== 1 || !result.stdout.includes('@typescript-eslint/no-unused-vars')) {
    throw new Error('ESLint did not reject the deliberate unused variable');
  }
  console.log('PASS: ESLint rejected the probe with no-unused-vars (exit 1).');
} finally {
  unlinkSync(file);
}
