import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Button } from '../../components/common/Button';
import { FiCalendar, FiClock } from 'react-icons/fi';
import { serviceService, Service } from '../../services/serviceService';
import { bookingService } from '../../services/bookingService';
import toast from 'react-hot-toast';

// TODO: fetch user's pets from API when Pet Service is integrated
import { mockPets } from '../../data/mockPets';

export const BookingPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const serviceId = searchParams.get('serviceId');
  
  const [service, setService] = useState<Service | null>(null);
  const [selectedPet, setSelectedPet] = useState(mockPets[0]);
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [selectedTime, setSelectedTime] = useState('09:00');
  const [availableSlots, setAvailableSlots] = useState<string[]>([]);
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  
  const navigate = useNavigate();

  useEffect(() => {
    if (!serviceId) {
      navigate('/services');
      return;
    }

    const loadData = async () => {
      try {
        const s = await serviceService.getServiceById(Number(serviceId));
        setService(s);
      } catch (error) {
        console.error(error);
        toast.error('Không tải được thông tin dịch vụ');
        navigate('/services');
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, [serviceId, navigate]);

  useEffect(() => {
    if (serviceId && selectedDate) {
      const loadSlots = async () => {
        try {
          const slots = await bookingService.getAvailableSlots(selectedDate, Number(serviceId));
          // map server slots to string like "09:00"
          const formatted = slots.map(isoStr => {
            const date = new Date(isoStr);
            return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
          });
          setAvailableSlots(formatted.length > 0 ? formatted : ['08:00', '09:00', '10:00', '11:00', '14:00', '15:00', '16:00']);
        } catch (error) {
          console.error(error);
          setAvailableSlots(['08:00', '09:00', '10:00', '11:00', '14:00', '15:00', '16:00']);
        }
      };
      loadSlots();
    }
  }, [selectedDate, serviceId]);

  const handleBooking = async () => {
    if (!service) return;
    setSubmitting(true);
    try {
      const dateTimeString = `${selectedDate}T${selectedTime}:00`;
      
      await bookingService.bookAppointment({
        petId: 1, // hardcode to 1 for now until Pet API is integrated
        serviceId: service.id,
        appointmentTime: dateTimeString,
        notes: notes
      });
      toast.success('Đặt lịch thành công!');
      navigate('/services');
    } catch (error) {
      console.error(error);
      toast.error('Có lỗi xảy ra khi đặt lịch. Vui lòng thử lại sau.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading || !service) {
    return <div className="min-h-screen py-20 flex justify-center"><div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div></div>;
  }

  return (
    <div className="bg-background min-h-screen py-10">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Đặt lịch: {service.name}</h1>

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
                      <p className="text-sm text-gray-500">{pet.breed}</p>
                    </div>
                  </button>
                ))}
              </div>
            </div>

            {/* Date Selection */}
            <div>
              <h3 className="font-semibold text-gray-900 mb-4 flex items-center">
                <FiCalendar className="mr-2" />
                Chọn ngày
              </h3>
              <input
                type="date"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
                min={new Date().toISOString().split('T')[0]}
                className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:ring-primary-500 focus:border-primary-500"
              />
            </div>

            {/* Time Selection */}
            <div>
              <h3 className="font-semibold text-gray-900 mb-4 flex items-center">
                <FiClock className="mr-2" />
                Chọn giờ
              </h3>
              <div className="grid grid-cols-4 gap-3">
                {availableSlots.map((time) => (
                  <button
                    key={time}
                    onClick={() => setSelectedTime(time)}
                    className={`py-2 px-1 rounded-xl border font-medium transition-colors ${
                      selectedTime === time
                        ? 'border-primary-600 bg-primary-600 text-white'
                        : 'border-gray-200 text-gray-700 hover:border-primary-300'
                    }`}
                  >
                    {time}
                  </button>
                ))}
              </div>
            </div>

            {/* Notes */}
            <div>
              <h3 className="font-semibold text-gray-900 mb-4 flex items-center">Ghi chú thêm</h3>
              <textarea
                rows={3}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Ví dụ: Bé nhát người lạ..."
                className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:ring-primary-500 focus:border-primary-500"
              />
            </div>
            
          </div>

          {/* Sidebar Summary */}
          <div className="md:w-80 space-y-6">
            <div className="bg-gray-50 rounded-2xl p-6 border border-gray-100">
              <h3 className="font-bold text-gray-900 mb-4 text-lg">Tóm tắt lịch hẹn</h3>
              
              <div className="space-y-4 mb-6">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">Dịch vụ:</span>
                  <span className="font-medium text-gray-900 text-right">{service.name}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">Thú cưng:</span>
                  <span className="font-medium text-gray-900 text-right">{selectedPet.name}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">Thời gian:</span>
                  <span className="font-medium text-gray-900 text-right">{selectedTime}, {selectedDate}</span>
                </div>
              </div>

              <div className="pt-4 border-t border-gray-200">
                <div className="flex justify-between items-center mb-6">
                  <span className="font-semibold text-gray-900">Tổng cộng:</span>
                  <span className="font-bold text-xl text-primary-600">{service.price.toLocaleString('vi-VN')} đ</span>
                </div>
                
                <Button onClick={handleBooking} disabled={submitting} fullWidth>
                  {submitting ? 'Đang xử lý...' : 'Xác nhận đặt lịch'}
                </Button>
                <p className="text-xs text-gray-500 text-center mt-4">
                  Bạn có thể hủy lịch miễn phí trước 24 giờ
                </p>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
};
