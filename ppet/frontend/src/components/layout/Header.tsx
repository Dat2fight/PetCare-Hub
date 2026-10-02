import React from 'react';
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
