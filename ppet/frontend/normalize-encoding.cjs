const fs = require('fs');
const path = require('path');

const srcDir = path.join(__dirname, 'src');

const replacements = [
  // ProductDetailPage specific
  ['Kh\ufffdng th? t?i th\ufffdng tin s?n ph?m. Vui l\ufffdng th? l?i sau.', 'Không thể tải thông tin sản phẩm. Vui lòng thử lại sau.'],
  ['Kh\ufffdng t\ufffdm th?y s?n ph?m', 'Không tìm thấy sản phẩm'],
  ['Quay l?i c?a h\ufffdng', 'Quay lại cửa hàng'],
  ['Trang ch?', 'Trang chủ'],
  ['S?n ph?m', 'Sản phẩm'],
  ['Chi ti?t', 'Chi tiết'],
  ['(124 d\ufffdnh gi\ufffd)', '(124 đánh giá)'],
  ['\ufffd?c di?m n?i b?t:', 'Đặc điểm nổi bật:'],
  ['Th\ufffdch h?p cho m?i gi?ng ch\ufffd/m\ufffdo', 'Thích hợp cho mọi giống chó/mèo'],
  ['Th\ufffdnh ph?n t? nhi\ufffdn 100%', 'Thành phần tự nhiên 100%'],
  ['\ufffd\ufffdng g\ufffdi c?n th?n, b?o qu?n d? d\ufffdng', 'Đóng gói cẩn thận, bảo quản dễ dàng'],
  ['M\ufffd t?:', 'Mô tả:'],
  ['S? lu?ng:', 'Số lượng:'],
  ['C\ufffdn h\ufffdng', 'Còn hàng'],
  ['H?t h\ufffdng', 'Hết hàng'],
  ['Th\ufffdm v\ufffdo gi? h\ufffdng', 'Thêm vào giỏ hàng'],

];

// Only exact string literals in an explicit file are eligible. Never replace code tokens.
const ts = require('typescript');
const { execFileSync } = require('child_process');
const write = process.argv.includes('--write');
const relative = process.argv.find(arg => arg.startsWith('--file='))?.slice(7);
if (!relative) {
  console.log('Chạy --file=src/path.tsx [--dry-run | --write]. Mặc định chỉ xem thay đổi.');
  process.exit(0);
}
const file = path.resolve(__dirname, relative);
if (!file.startsWith(srcDir + path.sep) || !/\.tsx?$/.test(file)) throw new Error('Chỉ hỗ trợ tệp TS/TSX trong src');
if (write && execFileSync('git', ['status', '--porcelain'], { cwd: __dirname, encoding: 'utf8' }).trim()) {
  throw new Error('Cần Git sạch trước khi ghi');
}
const original = fs.readFileSync(file, 'utf8');
const ast = ts.createSourceFile(file, original, ts.ScriptTarget.Latest, true, file.endsWith('.tsx') ? ts.ScriptKind.TSX : ts.ScriptKind.TS);
const edits = [];
function visit(node) {
  if (ts.isStringLiteral(node)) {
    const replacement = replacements.find(([bad]) => bad === node.text);
    if (replacement) edits.push({ start: node.getStart(ast), end: node.end, text: JSON.stringify(replacement[1]) });
  }
  ts.forEachChild(node, visit);
}
visit(ast);
let changed = original;
for (const edit of edits.sort((a, b) => b.start - a.start)) {
  console.log('- ' + original.slice(edit.start, edit.end)); console.log('+ ' + edit.text);
  changed = changed.slice(0, edit.start) + edit.text + changed.slice(edit.end);
}
changed = changed.replace(/^\uFEFF/, '');
if (write && changed !== original) {
  fs.writeFileSync(file, changed, 'utf8');
  try {
    execFileSync(process.execPath, [require.resolve('typescript/bin/tsc'), '--noEmit', '-p', 'tsconfig.app.json'], { cwd: __dirname, stdio: 'inherit' });
  } catch (error) {
    fs.writeFileSync(file, original, 'utf8');
    throw error;
  }
}
