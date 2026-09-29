import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../../components/common/Card';
import { mockProducts } from '../../data/mockProducts';
import { FiSearch, FiShoppingCart, FiStar } from 'react-icons/fi';
import { FaBone, FaBath, FaTag } from 'react-icons/fa';
import { MdToys } from 'react-icons/md';

export const ProductListPage: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const filteredProducts = mockProducts.filter(p => p.name.toLowerCase().includes(searchTerm.toLowerCase()));

  const categories = [
    { name: 'Thức ăn', icon: <FaBone size={24} />, color: 'bg-orange-100 text-orange-600' },
    { name: 'Đồ chơi', icon: <MdToys size={24} />, color: 'bg-purple-100 text-purple-600' },
    { name: 'Vệ sinh', icon: <FaBath size={24} />, color: 'bg-blue-100 text-blue-600' },
    { name: 'Phụ kiện', icon: <FaTag size={24} />, color: 'bg-green-100 text-green-600' },
  ];

  return (
    <div className="bg-background min-h-screen py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

        {/* Search */}
        <div className="flex flex-col md:flex-row md:items-center justify-between mb-10 gap-4">
          <h1 className="text-3xl font-bold text-gray-900">Sản phẩm nổi bật</h1>
          <div className="relative w-full md:w-96">
            <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
              <FiSearch size={20} />
            </div>
            <input
              type="text"
              className="block w-full rounded-full border border-gray-200 py-3 pl-12 pr-4 text-gray-900 focus:ring-2 focus:ring-primary-500 bg-white shadow-sm"
              placeholder="Tìm kiếm hạt Royal Canin, vòng cổ..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        {/* Categories */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-12">
          {categories.map((cat, idx) => (
            <button
              key={idx}
              className="bg-white rounded-2xl p-6 flex flex-col items-center justify-center border border-gray-100 hover:shadow-card hover:border-primary-100 transition-all cursor-pointer group"
            >
              <div
                className={`w-16 h-16 rounded-full flex items-center justify-center mb-4 transition-transform group-hover:scale-110 ${cat.color}`}
              >
                {cat.icon}
              </div>
              <span className="font-semibold text-gray-800">{cat.name}</span>
            </button>
          ))}
        </div>

        {/* Products Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {filteredProducts.map(product => (
            <Link to={`/products/${product.id}`} key={product.id}>
              <Card noPadding className="group cursor-pointer hover:shadow-lg transition-all duration-300 h-full flex flex-col border-transparent hover:border-primary-100">
                <div className="relative aspect-square overflow-hidden bg-white p-6 flex items-center justify-center border-b border-gray-50">
                  <img
                    src={product.image}
                    alt={product.name}
                    className="w-full h-full object-contain mix-blend-multiply group-hover:scale-105 transition-transform duration-500"
                  />
                  {product.discount && (
                    <div className="absolute top-3 left-3 bg-red-500 text-white text-xs font-bold px-2 py-1 rounded-lg">
                      -GIẢM
                    </div>
                  )}
                  <button
                    className="absolute bottom-3 right-3 p-3 bg-primary-600 text-white rounded-xl shadow-md hover:bg-primary-700 transition-colors z-10 translate-y-2 opacity-0 group-hover:translate-y-0 group-hover:opacity-100"
                    onClick={(e) => { e.preventDefault(); /* add to cart */ }}
                  >
                    <FiShoppingCart size={18} />
                  </button>
                </div>
                <div className="p-5 flex flex-col flex-grow bg-white">
                  <div className="flex items-center text-yellow-400 mb-2 text-xs">
                    <FiStar fill="currentColor" />
                    <span className="text-gray-600 ml-1 font-medium">{product.rating}</span>
                  </div>
                  <h3 className="font-medium text-gray-900 mb-2 line-clamp-2 group-hover:text-primary-600 transition-colors">
                    {product.name}
                  </h3>
                  <div className="mt-auto pt-2 flex flex-col">
                    <div className="font-bold text-lg text-primary-600">
                      {formatPrice(product.price - (product.discount || 0))}
                    </div>
                    {product.discount && (
                      <div className="text-sm text-gray-400 line-through">{formatPrice(product.price)}</div>
                    )}
                  </div>
                </div>
              </Card>
            </Link>
          ))}
        </div>

      </div>
    </div>
  );
};