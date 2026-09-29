import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../../components/common/Button';
import { mockPets } from '../../data/mockPets';
import { FiCalendar } from 'react-icons/fi';

export const VetBookingPage: React.FC = () => {
  const [selectedPet, setSelectedPet] = useState(mockPets[0]);
  const [reason, setReason] = useState('KhamTongQuat');
  const navigate = useNavigate();

  return (
    <div className="bg-background min-h-screen py-10">
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Đặt lịch Khám Thú Y</h1>

        <div className="bg-white rounded-3xl p-6 md:p-8 shadow-sm border border-gray-100">
          <div className="space-y-8">

            {/* Pet Selection */}
            <div>
              <h3 className="font-semibold text-gray-900 mb-4 flex items-center">Chọn thú cưng cần khám</h3>
              <div className="flex gap-4 overflow-x-auto pb-2">
                {mockPets.slice(0, 2).map((pet) => (
                  <button
                    key={pet.id}
                    onClick={() => setSelectedPet(pet)}
                    className={`flex items-center p-3 rounded-2xl border-2 transition-colors min-w-[200px] text-left ${
                      selectedPet.id === pet.id
                        ? 'border-primary-600 bg-primary-50'
                        : 'border-gray-200 hover:border-gray-300'
                    }`}
                  >
                    <img src={pet.image} alt={pet.name} className="w-12 h-12 rounded-full object-cover mr-3" />
                    <div>
                      <p className="font-bold text-gray-900">{pet.name}</p>
                      <p className="text-xs text-gray-500">Nặng: {pet.weight}kg</p>
                    </div>
                  </button>
                ))}
              </div>
            </div>

            <div>
              <h3 className="font-semibold text-gray-900 mb-4">Lý do khám</h3>
              <div className="flex flex-wrap gap-4">
                {[
                  { id: 'KhamTongQuat', label: 'Khám tổng quát' },
                  { id: 'TiemPhong', label: 'Tiêm phòng' },
                  { id: 'DieuTri', label: 'Điều trị bệnh' },
                  { id: 'Khac', label: 'Khác' }
                ].map(item => (
                  <label key={item.id} className="flex items-center cursor-pointer">
                    <input
                      type="radio"
                      checked={reason === item.id}
                      onChange={() => setReason(item.id)}
                      className="w-5 h-5 text-primary-600 focus:ring-primary-500 border-gray-300 mr-2"
                    />
                    <span className="text-gray-700">{item.label}</span>
                  </label>
                ))}
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 border-t border-gray-100 pt-6">
              <div>
                <h3 className="font-semibold text-gray-900 mb-4">Chọn ngày</h3>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
                    <FiCalendar />
                  </div>
                  <input
                    type="date"
                    className="block w-full rounded-xl border border-gray-300 py-3 pl-12 pr-4 text-gray-900 focus:ring-2 focus:ring-primary-500 bg-white"
                  />
                </div>
              </div>
              <div>
                <h3 className="font-semibold text-gray-900 mb-4">Khung giờ</h3>
                <select className="block w-full rounded-xl border border-gray-300 py-3 px-4 text-gray-900 focus:ring-2 focus:ring-primary-500 bg-white">
                  <option>09:00 - 10:00</option>
                  <option>10:00 - 11:00</option>
                  <option>14:00 - 15:00</option>
                </select>
              </div>
            </div>

            <div>
              <h3 className="font-semibold text-gray-900 mb-4">Ghi chú triệu chứng (nếu có)</h3>
              <textarea
                className="w-full border border-gray-300 rounded-xl p-4 text-gray-900 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                rows={3}
                placeholder="VD: Bé bỏ ăn 2 ngày nay..."
              ></textarea>
            </div>

            <div className="pt-4 flex justify-end">
              <Button size="lg" onClick={() => navigate('/services')} className="w-full sm:w-auto px-10">
                Đặt lịch khám
              </Button>
            </div>

          </div>
        </div>
      </div>
    </div>
  );
};