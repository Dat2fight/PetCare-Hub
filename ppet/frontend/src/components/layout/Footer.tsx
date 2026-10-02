import React from 'react';
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
