import codecs

header_content = '''import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { FiSearch, FiBell, FiShoppingCart, FiUser, FiLogOut, FiChevronDown } from 'react-icons/fi';
import { MdPets } from 'react-icons/md';

const Header: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="bg-white shadow-sm sticky top-0 z-50 border-b border-gray-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-20 items-center">
          
          {/* Logo */}
          <div className="flex-shrink-0 flex flex-col items-start cursor-pointer group" onClick={() => navigate('/')}>
            <div className="flex items-center">
              <div className="bg-primary-500 p-1.5 rounded-lg text-white mr-2 shadow-sm group-hover:scale-105 transition-transform">
                <MdPets size={22} />
              </div>
              <span className="font-extrabold text-2xl text-gray-900 tracking-tight">PetCare Hub</span>
            </div>
            <span className="text-[10px] text-gray-400 font-medium ml-10 -mt-1 tracking-wider uppercase">Better Care, Happier Pets</span>
          </div>

          {/* Center Navigation */}
          <nav className="hidden md:flex space-x-8">
            <Link to="/" className="text-gray-900 font-semibold hover:text-primary-600 transition-colors">Trang chủ</Link>
            <Link to="/pets" className="text-gray-600 hover:text-primary-600 font-medium transition-colors">Thú cưng</Link>
            <Link to="/products" className="text-gray-600 hover:text-primary-600 font-medium transition-colors">Sản phẩm</Link>
            <Link to="/services" className="text-gray-600 hover:text-primary-600 font-medium transition-colors">Dịch vụ</Link>
            <Link to="/pet-matching" className="text-gray-600 hover:text-primary-600 font-medium transition-colors">Tìm thú cưng phù hợp</Link>
          </nav>

          {/* Right Icons */}
          <div className="flex items-center space-x-5">
            <button className="text-gray-500 hover:text-primary-600 transition-colors p-2 rounded-full hover:bg-gray-50">
              <FiSearch size={22} />
            </button>
            
            {user ? (
              <>
                <Link to="/notifications" className="text-gray-500 hover:text-primary-600 transition-colors p-2 rounded-full hover:bg-gray-50 relative">
                  <FiBell size={22} />
                  <span className="absolute top-1 right-1 block h-2.5 w-2.5 rounded-full bg-coral-500 ring-2 ring-white" />
                </Link>
                <Link to="/cart" className="text-gray-500 hover:text-primary-600 transition-colors p-2 rounded-full hover:bg-gray-50 relative">
                  <FiShoppingCart size={22} />
                  <span className="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-primary-600 text-[10px] font-bold text-white">3</span>
                </Link>
                
                <div className="relative group ml-2">
                  <button className="flex items-center gap-2 focus:outline-none bg-gray-50 rounded-full pl-1 pr-3 py-1 border border-gray-100 hover:bg-gray-100 transition-colors">
                    <img className="h-8 w-8 rounded-full border border-primary-100 object-cover" src={user.avatarUrl || "https://placehold.co/100x100/ecfdf5/047857?text=U"} alt="User avatar" />
                    <span className="text-sm font-medium text-gray-700">{user.firstName}</span>
                    <FiChevronDown className="text-gray-400" size={16} />
                  </button>
                  {/* Dropdown Menu */}
                  <div className="absolute right-0 mt-2 w-56 bg-white rounded-2xl shadow-xl border border-gray-100 py-2 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 origin-top-right">
                    <div className="px-4 py-3 border-b border-gray-50 mb-2">
                      <p className="text-sm font-bold text-gray-900 truncate">{user.firstName} {user.lastName}</p>
                      <p className="text-xs text-gray-500 truncate mt-0.5">{user.email}</p>
                    </div>
                    <Link to="/my-pets" className="block px-4 py-2.5 text-sm font-medium text-gray-700 hover:bg-primary-50 hover:text-primary-700">Thú cưng của tôi</Link>
                    <Link to="/orders" className="block px-4 py-2.5 text-sm font-medium text-gray-700 hover:bg-primary-50 hover:text-primary-700">Đơn hàng</Link>
                    <Link to="/loyalty" className="block px-4 py-2.5 text-sm font-medium text-gray-700 hover:bg-primary-50 hover:text-primary-700">Điểm thưởng</Link>
                    <div className="border-t border-gray-50 my-2"></div>
                    <button onClick={handleLogout} className="block w-full text-left px-4 py-2.5 text-sm font-medium text-red-600 hover:bg-red-50 transition-colors flex items-center gap-2">
                      <FiLogOut /> Đăng xuất
                    </button>
                  </div>
                </div>
              </>
            ) : (
              <div className="flex items-center space-x-3 ml-2">
                <Link to="/login" className="text-gray-700 hover:text-primary-600 font-medium px-2">Đăng nhập</Link>
                <Link to="/register" className="bg-primary-600 text-white px-5 py-2 rounded-full font-medium hover:bg-primary-700 transition-colors shadow-sm">Đăng ký</Link>
              </div>
            )}
          </div>
          
        </div>
      </div>
    </header>
  );
};

export default Header;
'''

footer_content = '''import React from 'react';
import { Link } from 'react-router-dom';
import { MdPets } from 'react-icons/md';
import { FiFacebook, FiInstagram, FiTwitter, FiYoutube } from 'react-icons/fi';

const Footer: React.FC = () => {
  return (
    <footer className="bg-white pt-16 pb-8 border-t border-gray-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-8 lg:gap-12 mb-12">
          
          {/* Brand Col */}
          <div className="lg:col-span-2">
            <div className="flex flex-col items-start mb-6">
              <div className="flex items-center">
                <div className="bg-primary-500 p-1.5 rounded-lg text-white mr-2 shadow-sm">
                  <MdPets size={22} />
                </div>
                <span className="font-extrabold text-2xl text-gray-900 tracking-tight">PetCare Hub</span>
              </div>
              <span className="text-[10px] text-gray-400 font-medium ml-10 -mt-1 tracking-wider uppercase">Better Care, Happier Pets</span>
            </div>
            <p className="text-gray-500 text-sm leading-relaxed mb-6 max-w-sm">
              Chúng tôi cam kết mang đến những sản phẩm và dịch vụ tốt nhất, để mỗi thú cưng đều được yêu thương và chăm sóc đúng cách.
            </p>
            <div className="flex space-x-4">
              <a href="#" className="w-10 h-10 rounded-full bg-gray-50 flex items-center justify-center text-gray-400 hover:bg-primary-50 hover:text-primary-600 transition-colors">
                <FiFacebook size={18} />
              </a>
              <a href="#" className="w-10 h-10 rounded-full bg-gray-50 flex items-center justify-center text-gray-400 hover:bg-primary-50 hover:text-primary-600 transition-colors">
                <FiInstagram size={18} />
              </a>
              <a href="#" className="w-10 h-10 rounded-full bg-gray-50 flex items-center justify-center text-gray-400 hover:bg-primary-50 hover:text-primary-600 transition-colors">
                <FiYoutube size={18} />
              </a>
              <a href="#" className="w-10 h-10 rounded-full bg-gray-50 flex items-center justify-center text-gray-400 hover:bg-primary-50 hover:text-primary-600 transition-colors">
                <FiTwitter size={18} />
              </a>
            </div>
          </div>

          {/* Links Col */}
          <div>
            <h3 className="font-bold text-gray-900 mb-6">Liên kết nhanh</h3>
            <ul className="space-y-4">
              <li><Link to="/" className="text-gray-500 hover:text-primary-600 transition-colors text-sm">Trang chủ</Link></li>
              <li><Link to="/pets" className="text-gray-500 hover:text-primary-600 transition-colors text-sm">Thú cưng</Link></li>
              <li><Link to="/products" className="text-gray-500 hover:text-primary-600 transition-colors text-sm">Sản phẩm</Link></li>
              <li><Link to="/services" className="text-gray-500 hover:text-primary-600 transition-colors text-sm">Dịch vụ</Link></li>
              <li><Link to="/pet-matching" className="text-gray-500 hover:text-primary-600 transition-colors text-sm">Tìm thú cưng phù hợp</Link></li>
            </ul>
          </div>

          {/* Support Col */}
          <div>
            <h3 className="font-bold text-gray-900 mb-6">Chăm sóc khách hàng</h3>
            <ul className="space-y-4">
              <li><a href="#" className="text-gray-500 hover:text-primary-600 transition-colors text-sm">Hotline: 1900 1234</a></li>
              <li><a href="#" className="text-gray-500 hover:text-primary-600 transition-colors text-sm">Email: support@petcarehub.vn</a></li>
              <li><a href="#" className="text-gray-500 hover:text-primary-600 transition-colors text-sm">Hướng dẫn mua hàng</a></li>
              <li><a href="#" className="text-gray-500 hover:text-primary-600 transition-colors text-sm">Chính sách giao hàng</a></li>
              <li><a href="#" className="text-gray-500 hover:text-primary-600 transition-colors text-sm">Câu hỏi thường gặp</a></li>
            </ul>
          </div>

          {/* App Col */}
          <div>
            <h3 className="font-bold text-gray-900 mb-6">Tải ứng dụng</h3>
            <p className="text-gray-500 text-sm mb-4">Mua sắm và đặt lịch dễ dàng hơn trên ứng dụng di động.</p>
            <div className="space-y-3">
              <button className="w-full bg-gray-900 text-white rounded-xl py-2.5 px-4 flex items-center justify-center hover:bg-gray-800 transition-colors">
                <span className="font-semibold text-sm">App Store</span>
              </button>
              <button className="w-full bg-gray-900 text-white rounded-xl py-2.5 px-4 flex items-center justify-center hover:bg-gray-800 transition-colors">
                <span className="font-semibold text-sm">Google Play</span>
              </button>
            </div>
          </div>

        </div>

        {/* Bottom */}
        <div className="pt-8 border-t border-gray-100 flex flex-col md:flex-row justify-between items-center gap-4">
          <p className="text-gray-400 text-sm text-center md:text-left">
            © 2025 PetCare Hub. Tất cả quyền được bảo lưu.
          </p>
          <div className="flex space-x-6">
            <a href="#" className="text-gray-400 hover:text-gray-600 text-sm transition-colors">Điều khoản sử dụng</a>
            <a href="#" className="text-gray-400 hover:text-gray-600 text-sm transition-colors">Chính sách bảo mật</a>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
'''

with codecs.open('src/components/layout/Header.tsx', 'w', 'utf-8') as f:
    f.write(header_content)

with codecs.open('src/components/layout/Footer.tsx', 'w', 'utf-8') as f:
    f.write(footer_content)

print("Done writing header and footer.")
