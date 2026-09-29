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
              N?n t?ng toàn di?n cham sóc và cung c?p d?ch v? t?t nh?t cho thú cung c?a b?n. Better Care, Happier Pets.
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
            <h3 className="font-bold text-gray-900 mb-6">Danh m?c</h3>
            <ul className="space-y-4">
              <li><Link to="/pets" className="text-gray-500 hover:text-primary-600 transition-colors">Tìm thú cung</Link></li>
              <li><Link to="/products" className="text-gray-500 hover:text-primary-600 transition-colors">S?n ph?m</Link></li>
              <li><Link to="/services/grooming" className="text-gray-500 hover:text-primary-600 transition-colors">D?ch v? Grooming</Link></li>
              <li><Link to="/services/vet" className="text-gray-500 hover:text-primary-600 transition-colors">Khám Thú Y</Link></li>
            </ul>
          </div>

          {/* Links 2 */}
          <div>
            <h3 className="font-bold text-gray-900 mb-6">H? tr? khách hàng</h3>
            <ul className="space-y-4">
              <li><Link to="#" className="text-gray-500 hover:text-primary-600 transition-colors">Chính sách b?o hành</Link></li>
              <li><Link to="#" className="text-gray-500 hover:text-primary-600 transition-colors">Chính sách d?i tr?</Link></li>
              <li><Link to="#" className="text-gray-500 hover:text-primary-600 transition-colors">Hu?ng d?n mua hàng</Link></li>
              <li><Link to="#" className="text-gray-500 hover:text-primary-600 transition-colors">Câu h?i thu?ng g?p (FAQ)</Link></li>
            </ul>
          </div>

          {/* Newsletter */}
          <div>
            <h3 className="font-bold text-gray-900 mb-6">Ðang ký nh?n tin</h3>
            <p className="text-gray-500 mb-4">Nh?n thông tin uu dãi m?i nh?t t? PetCare Hub.</p>
            <div className="flex">
              <input type="email" placeholder="Email c?a b?n" className="bg-gray-50 border border-gray-200 text-gray-900 text-sm rounded-l-xl focus:ring-primary-500 focus:border-primary-500 block w-full p-3" />
              <button className="bg-primary-600 text-white px-4 rounded-r-xl font-medium hover:bg-primary-700 transition-colors">
                G?i
              </button>
            </div>
          </div>
        </div>

        <div className="border-t border-gray-100 pt-8 flex flex-col md:flex-row justify-between items-center">
          <p className="text-gray-400 text-sm mb-4 md:mb-0">© 2026 PetCare Hub. All rights reserved.</p>
          <div className="flex space-x-6 text-sm text-gray-400">
            <a href="#" className="hover:text-primary-600">Ði?u kho?n s? d?ng</a>
            <a href="#" className="hover:text-primary-600">Chính sách b?o m?t</a>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
