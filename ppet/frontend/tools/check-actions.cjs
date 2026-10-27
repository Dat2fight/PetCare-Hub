// Static regression check for placeholder links and buttons with no action.
const ts = require('typescript');
const { readdirSync, readFileSync } = require('node:fs');
const { join } = require('node:path');
const failures = [];
function walk(dir) {
  for (const item of readdirSync(dir, { withFileTypes: true })) {
    const path = join(dir, item.name);
    if (item.isDirectory()) { walk(path); continue; }
    if (!path.endsWith('.tsx') || item.name === 'Button.tsx') continue;
    const source = ts.createSourceFile(path, readFileSync(path, 'utf8'), ts.ScriptTarget.Latest, true, ts.ScriptKind.TSX);
    function visit(node) {
      if (ts.isJsxOpeningElement(node) || ts.isJsxSelfClosingElement(node)) {
        const tag = node.tagName.getText(source);
        const attrs = new Map(node.attributes.properties.filter(ts.isJsxAttribute).map(a => [a.name.getText(source), a.initializer?.getText(source)]));
        if (attrs.get('href') === '"#"' || attrs.get('href') === "'#'") failures.push(`${path}:${source.getLineAndCharacterOfPosition(node.pos).line + 1}: href=#`);
        if (['button', 'Button'].includes(tag) && !attrs.has('onClick') && !attrs.has('disabled')) {
          let ancestor = node.parent; let action = false;
          while (ancestor) {
            if (ts.isJsxElement(ancestor)) {
              const name = ancestor.openingElement.tagName.getText(source);
              if (name === 'Link' || name === 'form' && ancestor.openingElement.attributes.properties.some(a => ts.isJsxAttribute(a) && a.name.getText(source) === 'onSubmit')) action = true;
            }
            ancestor = ancestor.parent;
          }
          if (!action) failures.push(`${path}:${source.getLineAndCharacterOfPosition(node.pos).line + 1}: ${tag} has no action`);
        }
      }
      ts.forEachChild(node, visit);
    }
    visit(source);
  }
}
walk(join(__dirname, '../src'));
if (failures.length) { console.error(failures.join('\n')); process.exitCode = 1; }
else console.log('PASS: no placeholder href or unhandled enabled button. Link wrappers and form submissions are recognized.');
