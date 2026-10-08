import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { mockPets } from '../../data/mockPets';
import { FiSearch, FiHeart, FiMapPin } from 'react-icons/fi';
import { MdPets } from 'react-icons/md';
import { FaCat, FaDog } from 'react-icons/fa';

export const PetListPage: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [activeSpecies, setActiveSpecies] = useState<'All' | 'Dog' | 'Cat'>('All');

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const filteredPets = mockPets.filter(pet => {
    const matchesSearch =
      pet.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      pet.breed.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesSpecies = activeSpecies === 'All' || pet.species === activeSpecies;
    return matchesSearch && matchesSpecies;
  });

  return (
    <div className="bg-background min-h-screen py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

        {/* Search and Hero Area */}
        <div className="bg-primary-50 rounded-3xl p-8 md:p-12 mb-10 flex flex-col md:flex-row items-center justify-between relative overflow-hidden">
          <div className="md:w-2/3 relative z-10">
            <h1 className="text-3xl md:text-5xl font-bold text-gray-900 mb-4 leading-tight">
              Tìm người bạn bốn chân <br className="hidden md:block" /> phù hợp với bạn
            </h1>
            <div className="relative max-w-xl mt-6">
              <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
                <FiSearch size={20} />
              </div>
              <input
                type="text"
                className="block w-full rounded-2xl border-none shadow-sm py-4 pl-12 pr-4 text-gray-900 focus:ring-2 focus:ring-primary-500 bg-white"
                placeholder="Tìm kiếm chó Corgi, mèo Anh lông ngắn..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
              <button className="absolute inset-y-2 right-2 bg-primary-600 text-white px-6 rounded-xl font-medium hover:bg-primary-700 transition-colors">
                Tìm
              </button>
            </div>

            {/* Quick Filters */}
            <div className="flex gap-4 mt-8">
              <button
                onClick={() => setActiveSpecies('All')}
                className={`flex items-center px-4 py-2 rounded-full font-medium transition-colors ${
                  activeSpecies === 'All'
                    ? 'bg-primary-600 text-white'
                    : 'bg-white text-gray-600 hover:bg-primary-100'
                }`}
              >
                <MdPets className="mr-2" /> Tất cả
              </button>
              <button
                onClick={() => setActiveSpecies('Dog')}
                className={`flex items-center px-4 py-2 rounded-full font-medium transition-colors ${
                  activeSpecies === 'Dog'
                    ? 'bg-primary-600 text-white'
                    : 'bg-white text-gray-600 hover:bg-primary-100'
                }`}
              >
                <FaDog className="mr-2" /> Chó
              </button>
              <button
                onClick={() => setActiveSpecies('Cat')}
                className={`flex items-center px-4 py-2 rounded-full font-medium transition-colors ${
                  activeSpecies === 'Cat'
                    ? 'bg-primary-600 text-white'
                    : 'bg-white text-gray-600 hover:bg-primary-100'
                }`}
              >
                <FaCat className="mr-2" /> Mèo
              </button>
            </div>
          </div>
        </div>

        <div className="flex flex-col lg:flex-row gap-8">
          {/* Sidebar Filter (Desktop) */}
          <div className="hidden lg:block w-64 flex-shrink-0">
            <div className="bg-white rounded-2xl p-6 shadow-card border border-gray-100 sticky top-28">
              <div className="flex items-center justify-between mb-6">
                <h2 className="font-bold text-lg text-gray-900">Bộ lọc</h2>
                <button className="text-sm text-primary-600 font-medium">Xóa lọc</button>
              </div>

              <div className="space-y-6">
                <div>
                  <h3 className="font-semibold text-gray-900 mb-3">Giới tính</h3>
                  <div className="space-y-2">
                    <label className="flex items-center text-gray-600"><input type="checkbox" className="mr-3 rounded text-primary-600 focus:ring-primary-500" /> Đực</label>
                    <label className="flex items-center text-gray-600"><input type="checkbox" className="mr-3 rounded text-primary-600 focus:ring-primary-500" /> Cái</label>
                  </div>
                </div>

                <div className="border-t border-gray-100 pt-4">
                  <h3 className="font-semibold text-gray-900 mb-3">Mức giá</h3>
                  <div className="space-y-2">
                    <label className="flex items-center text-gray-600"><input type="radio" name="price" className="mr-3 text-primary-600 focus:ring-primary-500" /> Dưới 5 triệu</label>
                    <label className="flex items-center text-gray-600"><input type="radio" name="price" className="mr-3 text-primary-600 focus:ring-primary-500" /> 5 - 10 triệu</label>
                    <label className="flex items-center text-gray-600"><input type="radio" name="price" className="mr-3 text-primary-600 focus:ring-primary-500" /> Trên 10 triệu</label>
                  </div>
                </div>

                <div className="border-t border-gray-100 pt-4">
                  <h3 className="font-semibold text-gray-900 mb-3">Độ tuổi</h3>
                  <div className="space-y-2">
                    <label className="flex items-center text-gray-600"><input type="checkbox" className="mr-3 rounded text-primary-600 focus:ring-primary-500" /> Dưới 3 tháng</label>
                    <label className="flex items-center text-gray-600"><input type="checkbox" className="mr-3 rounded text-primary-600 focus:ring-primary-500" /> 3 - 6 tháng</label>
                    <label className="flex items-center text-gray-600"><input type="checkbox" className="mr-3 rounded text-primary-600 focus:ring-primary-500" /> Trên 6 tháng</label>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Product Grid */}
          <div className="flex-1">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold text-gray-900">
                Tìm thấy <span className="text-primary-600">{filteredPets.length}</span> thú cưng
              </h2>
              <div className="flex items-center space-x-2">
                <span className="text-sm text-gray-500">Sắp xếp:</span>
                <select className="text-sm border-gray-200 rounded-lg focus:ring-primary-500 focus:border-primary-500">
                  <option>Mới nhất</option>
                  <option>Giá thấp đến cao</option>
                  <option>Giá cao đến thấp</option>
                </select>
              </div>
            </div>

            {filteredPets.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
                {filteredPets.map(pet => (
                  <Link to={`/pets/${pet.id}`} key={pet.id}>
                    <Card noPadding className="group cursor-pointer hover:shadow-lg transition-all duration-300 h-full flex flex-col border-transparent hover:border-primary-100">
                      <div className="relative aspect-[4/3] overflow-hidden bg-gray-100">
                        <img
                          src={pet.image}
                          alt={pet.name}
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                        />
                        <button
                          className={`absolute top-3 right-3 p-2 rounded-full shadow-sm transition-colors z-10 ${
                            pet.isFavorite
                              ? 'bg-coral-50 text-coral-500'
                              : 'bg-white text-gray-400 hover:text-coral-500'
                          }`}
                          onClick={(e) => { e.preventDefault(); /* handle favorite */ }}
                        >
                          <FiHeart className={pet.isFavorite ? 'fill-current' : ''} />
                        </button>
                        {pet.healthStatus === 'Tốt' && (
                          <div className="absolute bottom-3 left-3">
                            <Badge variant="success">Khỏe mạnh</Badge>
                          </div>
                        )}
                      </div>
                      <div className="p-5 flex flex-col flex-grow">
                        <div className="flex justify-between items-start mb-1">
                          <h3 className="font-bold text-lg text-gray-900 group-hover:text-primary-600 transition-colors">{pet.name}</h3>
                        </div>
                        <p className="text-sm text-gray-500 mb-3">{pet.breed} • {pet.age} • {pet.gender}</p>

                        <div className="mt-auto pt-4 border-t border-gray-50 flex justify-between items-center">
                          <div className="font-bold text-lg text-primary-600">{formatPrice(pet.price)}</div>
                          <div className="flex items-center text-xs text-gray-400">
                            <FiMapPin className="mr-1" /> {pet.location}
                          </div>
                        </div>
                      </div>
                    </Card>
                  </Link>
                ))}
              </div>
            ) : (
              <div className="text-center py-20 bg-white rounded-2xl shadow-sm border border-gray-100">
                <MdPets size={48} className="mx-auto text-gray-300 mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">Không tìm thấy thú cưng nào</h3>
                <p className="text-gray-500">Vui lòng thử thay đổi tiêu chí tìm kiếm hoặc bộ lọc.</p>
                <Button
                  variant="outline"
                  className="mt-6"
                  onClick={() => { setSearchTerm(''); setActiveSpecies('All'); }}
                >
                  Xóa tất cả bộ lọc
                </Button>
              </div>
            )}

            {/* Pagination */}
            {filteredPets.length > 0 && (
              <div className="mt-10 flex justify-center space-x-2">
                <button className="w-10 h-10 flex items-center justify-center rounded-xl bg-white border border-gray-200 text-gray-500 hover:bg-gray-50 transition-colors">‹</button>
                <button className="w-10 h-10 flex items-center justify-center rounded-xl bg-primary-600 text-white font-medium shadow-sm">1</button>
                <button className="w-10 h-10 flex items-center justify-center rounded-xl bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 transition-colors">2</button>
                <button className="w-10 h-10 flex items-center justify-center rounded-xl bg-white border border-gray-200 text-gray-500 hover:bg-gray-50 transition-colors">›</button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};