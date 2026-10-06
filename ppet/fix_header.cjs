const fs = require('fs');
const path = 'frontend/src/components/layout/Header.tsx';
let content = fs.readFileSync(path, 'utf8');

// replace everything from <p className="text-xs text-gray-500 truncate mt-0.5">{user.email}</p> to <Link to="/my-pets"
const regex = /<p className="text-xs text-gray-500 truncate mt-0\.5">\{user\.email\}<\/p>[\s\S]*?<Link to="\/my-pets"/;
content = content.replace(regex, `<p className="text-xs text-gray-500 truncate mt-0.5">{user.email}</p>\n                    </div>\n                    <Link to="/profile" className="block px-4 py-2.5 text-sm font-medium text-gray-700 hover:bg-primary-50 hover:text-primary-700">Hồ sơ cá nhân</Link>\n                    <Link to="/my-pets"`);

fs.writeFileSync(path, content, 'utf8');
