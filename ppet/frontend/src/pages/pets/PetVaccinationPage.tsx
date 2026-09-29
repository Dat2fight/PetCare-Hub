import React from 'react';
import { Link, useParams } from 'react-router-dom';
import { mockPets } from '../../data/mockPets';
import { Button } from '../../components/common/Button';

export const PetVaccinationPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const myPet = mockPets.find(p => p.id === id) || mockPets[0];

  return (
    <div className="bg-background min-h-screen py-10">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-8 flex items-center gap-4">
          <img src={myPet.image} alt={myPet.name} className="w-12 h-12 rounded-full object-cover" />
          Lịch tiêm phòng - {myPet.name}
        </h1>

        <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-8 mb-8">
          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 border rounded-2xl bg-green-50 border-green-100">
              <div className="flex items-center gap-4">
                <div className="w-10 h-10 rounded-full bg-green-100 text-green-600 flex items-center justify-center text-xl">✓</div>
                <div>
                  <h4 className="font-bold text-gray-900">Vaccine mũi 1 (5 bệnh)</h4>
                  <p className="text-sm text-gray-500">15/02/2026</p>
                </div>
              </div>
              <span className="font-semibold text-green-600">Đã tiêm</span>
            </div>
            
            <div className="flex items-center justify-between p-4 border rounded-2xl bg-orange-50 border-orange-100">
              <div className="flex items-center gap-4">
                <div className="w-10 h-10 rounded-full bg-orange-100 text-orange-600 flex items-center justify-center text-xl">!</div>
                <div>
                  <h4 className="font-bold text-gray-900">Vaccine mũi 2 (5 bệnh)</h4>
                  <p className="text-sm text-gray-500">Dự kiến: 15/03/2026</p>
                </div>
              </div>
              <span className="font-semibold text-orange-600">Sắp đến hạn</span>
            </div>
            
            <div className="flex items-center justify-between p-4 border rounded-2xl border-gray-200">
              <div className="flex items-center gap-4">
                <div className="w-10 h-10 rounded-full bg-gray-100 text-gray-400 flex items-center justify-center text-xl">-</div>
                <div>
                  <h4 className="font-bold text-gray-900">Vaccine dại</h4>
                  <p className="text-sm text-gray-500">Dự kiến: 15/06/2026</p>
                </div>
              </div>
              <span className="font-semibold text-gray-400">Chưa đến hạn</span>
            </div>
          </div>
          
          <div className="mt-8 text-right">
            <Link to="/services/vet/booking">
              <Button>Đặt lịch tiêm phòng</Button>
            </Link>
          </div>
        </div>
        
        <div className="text-center">
          <Link to="/my-pets" className="text-primary-600 font-medium hover:underline">← Quay lại hồ sơ chung</Link>
        </div>
      </div>
    </div>
  );
};