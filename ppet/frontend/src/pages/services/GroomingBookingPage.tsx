import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../../components/common/Button';
import { mockPets } from '../../data/mockPets';
import { FiCalendar, FiClock, FiCheck } from 'react-icons/fi';

export const GroomingBookingPage: React.FC = () => {
  const [selectedPet, setSelectedPet] = useState(mockPets[0]);
  const [selectedDate, setSelectedDate] = useState('2026-05-12');
  const [selectedTime, setSelectedTime] = useState('09:00');
  const [selectedServices, setSelectedServices] = useState<string[]>(['bath', 'cut']);
  const navigate = useNavigate();

  const timeSlots = ['08:00', '09:00', '10:00', '11:00', '14:00', '15:00', '16:00', '17:00'];

  const toggleService = (id: string) => {
    if (selectedServices.includes(id)) {
      setSelectedServices(selectedServices.filter(s => s !== id));
    } else {
      setSelectedServices([...selectedServices, id]);
    }
  };

  const handleBooking = () => {
    navigate('/services');
  };

  return (
    <div className="bg-background min-h-screen py-10">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">

        <h1 className="text-3xl font-bold text-gray-900 mb-8">Đặt lịch Tắm &amp; Grooming</h1>

        <div className="bg-white rounded-3xl p-6 md:p-8 shadow-sm border border-gray-100 flex flex-col md:flex-row gap-10">

          <div className="flex-1 space-y-8">
            {/* Pet Selection */}
            <div>
              <h3 className="font-semibold text-gray-900 mb-4 flex items-center">Chọn thú cưng</h3>
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
                      <p className="text-xs text-gray-500">{pet.breed}</p>
                    </div>
                  </button>
                ))}
              </div>
            </div>

            {/* DateTime Selection */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <h3 className="font-semibold text-gray-900 mb-4">Chọn ngày</h3>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
                    <FiCalendar />
                  </div>
                  <input
                    type="date"
                    className="block w-full rounded-xl border border-gray-300 py-3 pl-12 pr-4 text-gray-900 focus:ring-2 focus:ring-primary-500 bg-white"
                    value={selectedDate}
                    onChange={(e) => setSelectedDate(e.target.value)}
                  />
                </div>
              </div>
            </div>

            <div>
              <h3 className="font-semibold text-gray-900 mb-4">Khung giờ</h3>
              <div className="grid grid-cols-3 sm:grid-cols-4 gap-3">
                {timeSlots.map(time => (
                  <button
                    key={time}
                    onClick={() => setSelectedTime(time)}
                    className={`py-2 rounded-xl text-sm font-medium transition-colors border ${
                      selectedTime === time
                        ? 'bg-primary-600 text-white border-primary-600'
                          : 'bg-white text-gray-600 border-gray-200 hover:border-primary-300'
                    }`}
                  >
                    {time}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <div className="md:w-1/3 flex flex-col">
            <h3 className="font-semibold text-gray-900 mb-4">Dịch vụ thêm</h3>
            <div className="space-y-3 mb-8 flex-1">
              {[
                { id: 'bath', name: 'Tắm cơ bản' },
                { id: 'cut', name: 'Cắt tỉa lông' },
                { id: 'clean', name: 'Vệ sinh tai' },
                { id: 'nail', name: 'Cắt móng' }
              ].map(srv => (
                <label
                  key={srv.id}
                  onClick={() => toggleService(srv.id)}
                  className={`flex items-center justify-between p-3 border rounded-xl cursor-pointer transition-colors ${
                    selectedServices.includes(srv.id)
                      ? 'border-primary-600 bg-primary-50'
                      : 'border-gray-200'
                  }`}
                >
                  <div className="flex items-center">
                    <div
                      className={`w-5 h-5 rounded flex items-center justify-center mr-3 border ${
                        selectedServices.includes(srv.id)
                          ? 'bg-primary-600 border-primary-600 text-white'
                          : 'border-gray-300 bg-white'
                      }`}
                    >
                      {selectedServices.includes(srv.id) && <FiCheck size={14} />}
                    </div>
                    <span className="text-gray-700 font-medium">{srv.name}</span>
                  </div>
                </label>
              ))}
            </div>

            <div className="border-t border-gray-100 pt-4 mt-auto">
              <div className="flex justify-between items-center mb-4">
                <span className="text-gray-600 font-medium">Tổng cộng:</span>
                <span className="text-2xl font-bold text-primary-600">250.000đ</span>
              </div>
              <Button fullWidth size="lg" onClick={handleBooking}>
                Xác nhận đặt lịch
              </Button>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
};