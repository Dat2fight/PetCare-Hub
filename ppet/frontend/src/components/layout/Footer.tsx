import React from 'react';
import { Link } from 'react-router-dom';
import { MdPets } from 'react-icons/md';
import { FaFacebook, FaInstagram, FaTwitter, FaYoutube } from 'react-icons/fa';

const Footer: React.FC = () => {
  return (
    <footer className="bg-white border-t border-gray-100 pt-16 pb-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12 mb-12">
          {/* Brand */}
          <div>
            <div className="flex items-center mb-6">
              <div className="bg-primary-500 p-2 rounded-xl text-white mr-2">
                <MdPets size={24} />
              </div>
              <span className="font-bold text-2xl text-gray-900 tracking-tight">PetCare Hub</span>
            </div>
            <p className="text-gray-500 mb-6">
              Nền tảng toàn diện chăm sóc và cung cấp dịch vụ tốt nhất cho thú cưng của bạn. Better Care, Happier Pets.
            </p>
            <div className="flex space-x-4">
              <a href="#" className="text-gray-400 hover:text-primary-600 transition-colors"><FaFacebook size={20} /></a>
              <a href="#" className="text-gray-400 hover:text-primary-600 transition-colors"><FaInstagram size={20} /></a>
              <a href="#" className="text-gray-400 hover:text-primary-600 transition-colors"><FaTwitter size={20} /></a>
              <a href="#" className="text-gray-400 hover:text-primary-600 transition-colors"><FaYoutube size={20} /></a>
            </div>
          </div>

          {/* Links 1 */}
          <div>
            <h3 className="font-bold text-gray-900 mb-6">Danh mục</h3>
            <ul className="space-y-4">
              <li><Link to="/pets" className="text-gray-500 hover:text-primary-600 transition-colors">Tìm thú cưng</Link></li>
              <li><Link to="/products" className="text-gray-500 hover:text-primary-600 transition-colors">Sản phẩm</Link></li>
              <li><Link to="/services/grooming" className="text-gray-500 hover:text-primary-600 transition-colors">Dịch vụ Grooming</Link></li>
              <li><Link to="/services/vet" className="text-gray-500 hover:text-primary-600 transition-colors">Khám Thú Y</Link></li>
            </ul>
          </div>

          {/* Links 2 */}
          <div>
            <h3 className="font-bold text-gray-900 mb-6">Hỗ trợ khách hàng</h3>
            <ul className="space-y-4">
              <li><Link to="#" className="text-gray-500 hover:text-primary-600 transition-colors">Chính sách bảo hành</Link></li>
              <li><Link to="#" className="text-gray-500 hover:text-primary-600 transition-colors">Chính sách đổi trả</Link></li>
              <li><Link to="#" className="text-gray-500 hover:text-primary-600 transition-colors">Hướng dẫn mua hàng</Link></li>
              <li><Link to="#" className="text-gray-500 hover:text-primary-600 transition-colors">Câu hỏi thường gặp (FAQ)</Link></li>
            </ul>
          </div>

          {/* Newsletter */}
          <div>
            <h3 className="font-bold text-gray-900 mb-6">Đăng ký nhận tin</h3>
            <p className="text-gray-500 mb-4">Nhận thông tin ưu đãi mới nhất từ PetCare Hub.</p>
            <div className="flex">
              <input type="email" placeholder="Email của bạn" className="bg-gray-50 border border-gray-200 text-gray-900 text-sm rounded-l-xl focus:ring-primary-500 focus:border-primary-500 block w-full p-3" />
              <button className="bg-primary-600 text-white px-4 rounded-r-xl font-medium hover:bg-primary-700 transition-colors">
                Gửi
              </button>
            </div>
          </div>
        </div>

        <div className="border-t border-gray-100 pt-8 flex flex-col md:flex-row justify-between items-center">
          <p className="text-gray-400 text-sm mb-4 md:mb-0">© 2026 PetCare Hub. All rights reserved.</p>
          <div className="flex space-x-6 text-sm text-gray-400">
            <a href="#" className="hover:text-primary-600">Điều khoản sử dụng</a>
            <a href="#" className="hover:text-primary-600">Chính sách bảo mật</a>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;