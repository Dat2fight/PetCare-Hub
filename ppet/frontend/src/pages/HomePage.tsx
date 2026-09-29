import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '../components/common/Button';
import { Card } from '../components/common/Card';
import { mockImages } from '../data/mockImages';
import { FiHeart, FiStar } from 'react-icons/fi';

export const HomePage: React.FC = () => {
  return (
    <div className="bg-background">
      {/* 1. Hero Section */}
      <section className="relative bg-primary-50 py-20 lg:py-32 overflow-hidden">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
          <div className="lg:w-1/2">
            <span className="inline-block py-1 px-3 rounded-full bg-primary-100 text-primary-700 font-semibold text-sm mb-4">N?n t?ng cham sóc thú cung #1</span>
            <h1 className="text-4xl lg:text-6xl font-bold text-gray-900 leading-tight mb-6">
              Tìm ngu?i b?n b?n chân phù h?p v?i b?n
            </h1>
            <p className="text-lg text-gray-600 mb-8 max-w-lg">
              PetCare Hub cung c?p gi?i pháp toàn di?n t? tìm ki?m thú cung, mua s?m s?n ph?m d?n d?t l?ch d?ch v? cham sóc và y t?.
            </p>
            <div className="flex flex-wrap gap-4">
              <Link to="/pets">
                <Button size="lg">Tìm Thú Cung</Button>
              </Link>
              <Link to="/products">
                <Button variant="outline" size="lg">Khám phá s?n ph?m</Button>
              </Link>
            </div>
          </div>
        </div>
        <div className="hidden lg:block absolute top-0 right-0 w-1/2 h-full">
          <img src={mockImages.dogHero} alt="Happy dog" className="w-full h-full object-cover rounded-l-[100px]" />
        </div>
      </section>

      {/* 2. Featured Pets */}
      <section className="py-16 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-end mb-8">
          <div>
            <h2 className="text-3xl font-bold text-gray-900 mb-2">Thú cung n?i b?t</h2>
            <p className="text-gray-500">Nh?ng ngu?i b?n dáng yêu dang ch? b?n</p>
          </div>
          <Link to="/pets" className="text-primary-600 font-medium hover:text-primary-700">Xem t?t c? ?</Link>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {[
            { id: 1, name: "Corgi", price: "9.500.000d", image: mockImages.dogCorgi, breed: "Chó Corgi", age: "2 tháng" },
            { id: 2, name: "Poodle", price: "6.000.000d", image: mockImages.dogPoodle, breed: "Chó Poodle", age: "3 tháng" },
            { id: 3, name: "Mèo Anh Lông Ng?n", price: "5.500.000d", image: mockImages.catScottish, breed: "Mèo ALN", age: "2.5 tháng" },
            { id: 4, name: "Golden Retriever", price: "12.000.000d", image: mockImages.dogGolden, breed: "Chó Golden", age: "2 tháng" },
          ].map(pet => (
            <Card key={pet.id} noPadding className="group cursor-pointer hover:shadow-lg transition-shadow">
              <div className="relative aspect-square overflow-hidden">
                <img src={pet.image} alt={pet.name} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
                <button className="absolute top-3 right-3 p-2 bg-white rounded-full text-gray-400 hover:text-coral-500 transition-colors shadow-sm">
                  <FiHeart />
                </button>
              </div>
              <div className="p-5">
                <div className="flex justify-between items-start mb-2">
                  <h3 className="font-bold text-lg text-gray-900">{pet.name}</h3>
                </div>
                <p className="text-sm text-gray-500 mb-3">{pet.breed} • {pet.age}</p>
                <div className="font-bold text-primary-600">{pet.price}</div>
              </div>
            </Card>
          ))}
        </div>
      </section>

      {/* 3. Pet Matching CTA */}
      <section className="py-12 bg-primary-600">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row items-center justify-between">
          <div className="text-white mb-6 md:mb-0 md:w-2/3">
            <h2 className="text-3xl font-bold mb-3">B?n chua bi?t ch?n thú cung nào?</h2>
            <p className="text-primary-100 text-lg">Làm bài tr?c nghi?m ng?n d? chúng tôi g?i ý ngu?i b?n b?n chân phù h?p nh?t v?i phong cách s?ng c?a b?n.</p>
          </div>
          <div>
            <Link to="/pet-matching">
              <Button className="bg-white text-primary-600 hover:bg-primary-50" size="lg">Làm bài test ngay</Button>
            </Link>
          </div>
        </div>
      </section>

      {/* 4. Popular Products */}
      <section className="py-16 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-end mb-8">
          <div>
            <h2 className="text-3xl font-bold text-gray-900 mb-2">S?n ph?m bán ch?y</h2>
            <p className="text-gray-500">Ð? dùng, th?c an t?t nh?t cho thú cung</p>
          </div>
          <Link to="/products" className="text-primary-600 font-medium hover:text-primary-700">Xem t?t c? ?</Link>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {[
            { id: 1, name: "Th?c an Royal Canin Adult", price: "1.200.000d", image: mockImages.foodRoyalCanin, rating: 4.8 },
            { id: 2, name: "Vòng c? cao c?p", price: "250.000d", image: mockImages.collar, rating: 4.5 },
            { id: 3, name: "Ð? choi bóng cao su", price: "120.000d", image: mockImages.toyBall, rating: 4.9 },
            { id: 4, name: "L?ng v?n chuy?n", price: "800.000d", image: mockImages.carrier, rating: 4.7 },
          ].map(product => (
            <Card key={product.id} noPadding className="group cursor-pointer hover:shadow-lg transition-shadow">
              <div className="relative aspect-square overflow-hidden bg-gray-50 p-6 flex items-center justify-center">
                <img src={product.image} alt={product.name} className="w-full h-full object-contain mix-blend-multiply group-hover:scale-105 transition-transform duration-300" />
              </div>
              <div className="p-5">
                <div className="flex items-center text-yellow-400 mb-2 text-sm">
                  <FiStar fill="currentColor" />
                  <span className="text-gray-600 ml-1">{product.rating}</span>
                </div>
                <h3 className="font-medium text-gray-900 mb-2 line-clamp-2">{product.name}</h3>
                <div className="flex justify-between items-center">
                  <span className="font-bold text-primary-600">{product.price}</span>
                  <button className="text-sm font-medium text-primary-600 bg-primary-50 px-3 py-1 rounded-full hover:bg-primary-100 transition-colors">Thêm</button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      </section>
      
      <div className="pb-20"></div>
    </div>
  );
};

export default HomePage;
