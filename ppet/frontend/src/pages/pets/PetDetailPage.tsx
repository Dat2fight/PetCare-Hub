import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { mockPets } from '../../data/mockPets';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { FiHeart, FiMapPin, FiCheckCircle, FiShare2, FiShield } from 'react-icons/fi';
import { MdPets, MdOutlineHealthAndSafety } from 'react-icons/md';

export const PetDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const pet = mockPets.find(p => p.id === id) || mockPets[0]; // fallback for demo

  const [activeImage, setActiveImage] = useState(pet.images[0]);

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  return (
    <div className="bg-background min-h-screen py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

        {/* Breadcrumb */}
        <nav className="flex text-sm text-gray-500 mb-8">
          <Link to="/" className="hover:text-primary-600 transition-colors">Trang chủ</Link>
          <span className="mx-2">/</span>
          <Link to="/pets" className="hover:text-primary-600 transition-colors">Thú cưng</Link>
          <span className="mx-2">/</span>
          <span className="text-gray-900 font-medium">{pet.name}</span>
        </nav>

        <div className="bg-white rounded-3xl shadow-soft border border-gray-100 overflow-hidden">
          <div className="flex flex-col lg:flex-row">

            {/* Left - Images */}
            <div className="w-full lg:w-1/2 p-6 lg:p-10 border-b lg:border-b-0 lg:border-r border-gray-100">
              <div className="relative aspect-square rounded-2xl overflow-hidden bg-gray-50 mb-4">
                <img src={activeImage} alt={pet.name} className="w-full h-full object-cover" />
                <button className="absolute top-4 right-4 p-3 bg-white rounded-full text-gray-400 hover:text-coral-500 shadow-sm transition-colors">
                  <FiHeart size={20} className={pet.isFavorite ? 'fill-coral-500 text-coral-500' : ''} />
                </button>
              </div>
              <div className="flex space-x-4 overflow-x-auto pb-2">
                {pet.images.map((img, idx) => (
                  <button
                    key={idx}
                    onClick={() => setActiveImage(img)}
                    className={`relative w-20 h-20 rounded-xl overflow-hidden flex-shrink-0 border-2 transition-colors ${
                      activeImage === img ? 'border-primary-600' : 'border-transparent'
                    }`}
                  >
                    <img src={img} alt={`${pet.name} ${idx + 1}`} className="w-full h-full object-cover" />
                    {activeImage !== img && (
                      <div className="absolute inset-0 bg-black/10 hover:bg-transparent transition-colors"></div>
                    )}
                  </button>
                ))}
              </div>
            </div>

            {/* Right - Details */}
            <div className="w-full lg:w-1/2 p-6 lg:p-10 flex flex-col">
              <div className="flex justify-between items-start mb-2">
                <h1 className="text-3xl font-bold text-gray-900">{pet.name}</h1>
                <button className="p-2 text-gray-400 hover:text-primary-600 transition-colors bg-gray-50 rounded-full">
                  <FiShare2 size={20} />
                </button>
              </div>

              <div className="flex items-center space-x-3 mb-6">
                <div className="flex items-center text-gray-500 text-sm">
                  <FiMapPin className="mr-1" /> {pet.location}
                </div>
                <span className="text-gray-300">|</span>
                <div className="flex items-center text-primary-600 text-sm font-medium">
                  <FiShield className="mr-1" /> Có bảo hành sức khỏe
                </div>
              </div>

              <div className="text-4xl font-bold text-primary-600 mb-8">
                {formatPrice(pet.price)}
              </div>

              {/* Quick Info Grid */}
              <div className="grid grid-cols-2 gap-4 mb-8">
                <div className="bg-primary-50 p-4 rounded-2xl flex items-center">
                  <div className="w-10 h-10 bg-white rounded-full flex items-center justify-center text-primary-600 mr-3 shadow-sm">
                    <MdPets size={20} />
                  </div>
                  <div>
                    <p className="text-xs text-gray-500">Giống</p>
                    <p className="font-semibold text-gray-900">{pet.breed}</p>
                  </div>
                </div>
                <div className="bg-primary-50 p-4 rounded-2xl flex items-center">
                  <div className="w-10 h-10 bg-white rounded-full flex items-center justify-center text-primary-600 mr-3 shadow-sm">
                    <span className="font-bold">{pet.age.charAt(0)}</span>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500">Tuổi</p>
                    <p className="font-semibold text-gray-900">{pet.age}</p>
                  </div>
                </div>
                <div className="bg-primary-50 p-4 rounded-2xl flex items-center">
                  <div className="w-10 h-10 bg-white rounded-full flex items-center justify-center text-primary-600 mr-3 shadow-sm">
                    <span className="font-bold">{pet.gender === 'Đực' ? '♂' : '♀'}</span>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500">Giới tính</p>
                    <p className="font-semibold text-gray-900">{pet.gender}</p>
                  </div>
                </div>
                <div className="bg-primary-50 p-4 rounded-2xl flex items-center">
                  <div className="w-10 h-10 bg-white rounded-full flex items-center justify-center text-primary-600 mr-3 shadow-sm">
                    <MdOutlineHealthAndSafety size={20} />
                  </div>
                  <div>
                    <p className="text-xs text-gray-500">Sức khỏe</p>
                    <p className="font-semibold text-green-600">{pet.healthStatus}</p>
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex space-x-4 mb-10 mt-auto">
                <Button size="lg" className="flex-1 py-4 text-lg">Đặt mua ngay</Button>
                <Button variant="outline" size="lg" className="flex-1 py-4 text-lg">Liên hệ tư vấn</Button>
              </div>

              {/* Health & Vaccine */}
              <div className="bg-gray-50 rounded-2xl p-5 border border-gray-100">
                <h3 className="font-bold text-gray-900 mb-3 flex items-center">
                  <FiCheckCircle className="text-green-500 mr-2" />
                  Tình trạng tiêm phòng
                </h3>
                <p className="text-sm text-gray-600 mb-2">
                  Trạng thái:{' '}
                  <Badge variant={pet.vaccinationStatus === 'Đã tiêm' ? 'success' : 'warning'}>
                    {pet.vaccinationStatus}
                  </Badge>
                </p>
                <p className="text-sm text-gray-500">
                  Cún đã được xổ lãi và tiêm 1 mũi vaccine 5 bệnh. Có sổ giun sán đầy đủ đi kèm khi đón cún.
                </p>
              </div>

            </div>
          </div>
        </div>

        {/* Detailed Tabs Section */}
        <div className="mt-8 bg-white rounded-3xl shadow-soft border border-gray-100 p-8">
          <h2 className="text-2xl font-bold text-gray-900 mb-6 pb-4 border-b border-gray-100">Thông tin chi tiết</h2>
          <div className="prose max-w-none text-gray-600">
            <p className="mb-6">{pet.description}</p>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mt-8">
              <div>
                <h3 className="text-lg font-bold text-gray-900 mb-4 flex items-center">
                  <span className="w-8 h-8 rounded-full bg-primary-100 text-primary-600 flex items-center justify-center mr-3">🎭</span>
                  Đặc điểm tính cách
                </h3>
                <ul className="space-y-3">
                  {pet.personality.map((p, i) => (
                    <li key={i} className="flex items-start">
                      <FiCheckCircle className="text-primary-500 mt-1 mr-3 flex-shrink-0" />
                      <span>{p}</span>
                    </li>
                  ))}
                </ul>
              </div>
              <div>
                <h3 className="text-lg font-bold text-gray-900 mb-4 flex items-center">
                  <span className="w-8 h-8 rounded-full bg-primary-100 text-primary-600 flex items-center justify-center mr-3">🧼</span>
                  Yêu cầu chăm sóc
                </h3>
                <ul className="space-y-3">
                  {pet.careRequirements.map((c, i) => (
                    <li key={i} className="flex items-start">
                      <FiCheckCircle className="text-primary-500 mt-1 mr-3 flex-shrink-0" />
                      <span>{c}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};