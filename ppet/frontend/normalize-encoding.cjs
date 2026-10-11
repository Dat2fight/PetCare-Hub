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

  // Any other typical mojibake patterns
  ['\ufffd?c', 'Đặc'],
  ['\ufffd\ufffdng', 'Đóng'],
  ['Th\ufffdch', 'Thích'],
  ['th\ufffdch', 'thích'],
  ['Th\ufffdnh', 'Thành'],
  ['th\ufffdnh', 'thành'],
  ['nhi\ufffdn', 'nhiên'],
  ['d\ufffdng', 'dàng'],
  ['Kh\ufffdng', 'Không'],
  ['kh\ufffdng', 'không'],
  ['th\ufffdng', 'thông'],
  ['Vui l\ufffdng', 'Vui lòng'],
  ['vui l\ufffdng', 'vui lòng'],
  ['c?a h\ufffdng', 'cửa hàng'],
  ['d\ufffdnh gi\ufffd', 'đánh giá'],
  ['C\ufffdn h\ufffdng', 'Còn hàng'],
  ['H?t h\ufffdng', 'Hết hàng'],
  ['gi? h\ufffdng', 'giỏ hàng'],
  ['s?n ph?m', 'sản phẩm'],
  ['S?n ph?m', 'Sản phẩm'],
  ['th?y', 'thấy'],
  ['l?i', 'lại'],
  ['c?a', 'của'],
  ['h\ufffdng', 'hàng'],
  ['ti?t', 'tiết'],
  ['di?m', 'điểm'],
  ['n?i', 'nổi'],
  ['b?t', 'bật'],
  ['gi?ng', 'giống'],
  ['ch\ufffd', 'chó'],
  ['m\ufffdo', 'mèo'],
  ['ph?n', 'phần'],
  ['c?n', 'cẩn'],
  ['th?n', 'thận'],
  ['b?o', 'bảo'],
  ['qu?n', 'quản'],
  ['d?', 'dễ'],
  ['lu?ng', 'lượng']
];

function cleanContent(content) {
  // Strip BOM if present (\uFEFF)
  let cleaned = content.replace(/^\uFEFF/, '');
  
  // Apply text replacements
  for (const [bad, good] of replacements) {
    if (cleaned.includes(bad)) {
      cleaned = cleaned.replaceAll(bad, good);
    }
  }
  return cleaned;
}

function processDirectory(dir) {
  let modifiedCount = 0;
  let totalFiles = 0;
  const entries = fs.readdirSync(dir, { withFileTypes: true });

  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      const { total, modified } = processDirectory(fullPath);
      totalFiles += total;
      modifiedCount += modified;
    } else if (entry.isFile() && (entry.name.endsWith('.ts') || entry.name.endsWith('.tsx') || entry.name.endsWith('.html') || entry.name.endsWith('.json'))) {
      totalFiles++;
      const original = fs.readFileSync(fullPath, 'utf8');
      const cleaned = cleanContent(original);
      if (cleaned !== original) {
        fs.writeFileSync(fullPath, cleaned, 'utf8');
        console.log(`[FIXED UTF-8] ${path.relative(__dirname, fullPath)}`);
        modifiedCount++;
      }
    }
  }
  return { total: totalFiles, modified: modifiedCount };
}

console.log('--- Starting Frontend UTF-8 Normalization ---');
const result = processDirectory(srcDir);
console.log(`\nProcessed ${result.total} files. Modified: ${result.modified} files.`);

