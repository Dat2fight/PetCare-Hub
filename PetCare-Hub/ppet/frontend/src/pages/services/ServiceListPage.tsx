import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { mockImages } from '../../data/mockImages';
import { FiScissors, FiActivity, FiCalendar, FiClock } from 'react-icons/fi';
import { MdPets } from 'react-icons/md';
import { serviceService, Service } from '../../services/serviceService';
import toast from 'react-hot-toast';

export const ServiceListPage: React.FC = () => {
  const [services, setServices] = useState<Service[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchServices = async () => {
      try {
        const data = await serviceService.getAllServices();
        setServices(data.filter(s => s.active));
      } catch (error) {
        console.error('Error fetching services:', error);
        toast.error('Không thể tải danh sách dịch vụ');
      } finally {
        setLoading(false);
      }
    };
    fetchServices();
  }, []);

  const getServiceIcon = (type: string) => {
    if (type === 'VETERINARY') return <FiActivity className="text-blue-500" size={32} />;
    return <FiScissors className="text-primary-600" size={32} />;
  };

  const getServiceImage = (type: string, id: number) => {
    if (type === 'VETERINARY') return mockImages.vet;
    return id % 2 === 0 ? mockImages.grooming : mockImages.placeholder;
  };

  return (
    <div className="bg-background min-h-screen py-10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        <div className="text-center max-w-2xl mx-auto mb-12">
          <h1 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">Dịch vụ chăm sóc thú cưng</h1>
          <p className="text-gray-600">Chúng tôi cung cấp các dịch vụ chất lượng cao nhất để đảm bảo thú cưng của bạn luôn khỏe mạnh và hạnh phúc.</p>
        </div>

        {loading ? (
          <div className="flex justify-center items-center h-64">
             <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {services.map((service, idx) => (
              <Card key={service.id} noPadding className="flex flex-col h-full hover:shadow-lg hover:-translate-y-1 transition-all duration-300">
                <div className="h-48 overflow-hidden relative">
                  <img src={getServiceImage(service.serviceType, service.id)} alt={service.name} className="w-full h-full object-cover" />
                  <div className="absolute -bottom-6 right-6 w-12 h-12 bg-white rounded-2xl shadow-sm flex items-center justify-center">
                    {getServiceIcon(service.serviceType)}
                  </div>
                </div>
                <div className="p-6 pt-8 flex-1 flex flex-col">
                  <div className="flex justify-between items-start mb-2">
                    <h3 className="text-xl font-bold text-gray-900">{service.name}</h3>
                  </div>
                  <p className="text-primary-600 font-bold mb-3">{service.price.toLocaleString('vi-VN')} đ <span className="text-sm text-gray-500 font-normal">/ {service.durationMinutes} phút</span></p>
                  <p className="text-gray-600 mb-6 flex-1">{service.description}</p>
                  
                  <Link to={`/services/booking?serviceId=${service.id}`}>
                    <Button fullWidth variant={idx === 0 ? 'primary' : 'outline'}>
                      Đặt lịch ngay
                    </Button>
                  </Link>
                </div>
              </Card>
            ))}
          </div>
        )}

        {/* Benefits section */}
        <div className="mt-20 bg-white rounded-3xl p-10 border border-gray-100 flex flex-col md:flex-row items-center justify-between gap-10">
          <div className="md:w-1/2">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">Tại sao chọn dịch vụ của PetCare Hub?</h2>
            <div className="space-y-4">
              <div className="flex items-start">
                <div className="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center text-primary-600 mr-4 flex-shrink-0">
                  <FiClock />
                </div>
                <div>
                  <h4 className="font-semibold text-gray-900">Tiết kiệm thời gian</h4>
                  <p className="text-sm text-gray-600">Đặt lịch online nhanh chóng, không cần chờ đợi tại cửa hàng.</p>
                </div>
              </div>
              <div className="flex items-start">
                <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 mr-4 flex-shrink-0">
                  <FiActivity />
                </div>
                <div>
                  <h4 className="font-semibold text-gray-900">Chuyên gia tận tâm</h4>
                  <p className="text-sm text-gray-600">Đội ngũ groomer và bác sĩ thú y có chứng chỉ quốc tế.</p>
                </div>
              </div>
              <div className="flex items-start">
                <div className="w-10 h-10 rounded-full bg-orange-100 flex items-center justify-center text-orange-600 mr-4 flex-shrink-0">
                  <FiCalendar />
                </div>
                <div>
                  <h4 className="font-semibold text-gray-900">Theo dõi lịch sử</h4>
                  <p className="text-sm text-gray-600">Mọi lịch sử khám bệnh và làm đẹp đều được lưu trữ trực tuyến.</p>
                </div>
              </div>
            </div>
          </div>
          <div className="md:w-1/2 rounded-2xl overflow-hidden shadow-sm h-64">
            <img src={mockImages.dogHero} className="w-full h-full object-cover" alt="Happy pet" />
          </div>
        </div>

      </div>
    </div>
  );
};