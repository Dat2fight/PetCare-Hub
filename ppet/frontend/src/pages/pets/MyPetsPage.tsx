import React from 'react';
import { Link } from 'react-router-dom';
import { mockPets } from '../../data/mockPets';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';

export const MyPetsPage: React.FC = () => {
  const myPet = mockPets[0]; // Bubi (Corgi)

  return (
    <div className="bg-background min-h-screen py-10">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Thú cưng của tôi</h1>
          <Button>+ Thêm thú cưng</Button>
        </div>

        <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-8 flex flex-col md:flex-row gap-8">
          <div className="flex flex-col items-center justify-center w-full md:w-1/4">
            <div className="w-32 h-32 rounded-full overflow-hidden border-4 border-primary-50 mb-4">
              <img src={myPet.image} alt={myPet.name} className="w-full h-full object-cover" />
            </div>
            <h2 className="text-2xl font-bold text-gray-900">{myPet.name}</h2>
            <p className="text-gray-500 mb-2">{myPet.breed} • {myPet.age}</p>
            <Badge variant="success">{myPet.healthStatus}</Badge>
          </div>

          <div className="w-full md:w-3/4">
            <div className="border-b border-gray-100 flex gap-6 mb-6">
              <button className="pb-3 border-b-2 border-primary-600 text-primary-600 font-semibold text-lg">
                Thông tin
              </button>
              <Link
                to={`/my-pets/${myPet.id}/health`}
                className="pb-3 border-b-2 border-transparent text-gray-400 hover:text-gray-600 font-medium text-lg"
              >
                Sức khỏe
              </Link>
              <Link
                to={`/my-pets/${myPet.id}/vaccinations`}
                className="pb-3 border-b-2 border-transparent text-gray-400 hover:text-gray-600 font-medium text-lg"
              >
                Lịch sử tiêm
              </Link>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-y-4 gap-x-12">
              <div className="flex justify-between border-b border-gray-50 pb-2">
                <span className="text-gray-500">Cân nặng:</span>
                <span className="font-semibold text-gray-900">{myPet.weight}kg</span>
              </div>
              <div className="flex justify-between border-b border-gray-50 pb-2">
                <span className="text-gray-500">Giới tính:</span>
                <span className="font-semibold text-gray-900">{myPet.gender}</span>
              </div>
              <div className="flex justify-between border-b border-gray-50 pb-2">
                <span className="text-gray-500">Ngày sinh:</span>
                <span className="font-semibold text-gray-900">12/03/2026</span>
              </div>
              <div className="flex justify-between border-b border-gray-50 pb-2">
                <span className="text-gray-500">Màu sắc:</span>
                <span className="font-semibold text-gray-900">Vàng trắng</span>
              </div>
            </div>

            <div className="mt-8 flex justify-end">
              <Button variant="outline">Chỉnh sửa thông tin</Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};