import React from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { mockImages } from '../../data/mockImages';
import { FiScissors, FiActivity, FiCalendar, FiClock } from 'react-icons/fi';
import { MdPets } from 'react-icons/md';

export const ServiceListPage: React.FC = () => {
  const services = [
    {
      id: 'grooming',
      title: 'Tắm & Grooming',
      description: 'Dịch vụ làm đẹp toàn diện, cắt tỉa lông chuẩn form, vệ sinh tai móng, mang lại vẻ ngoài hoàn hảo cho thú cưng.',
      icon: <FiScissors className="text-primary-600" size={32} />,
      image: mockImages.grooming,
      link: '/services/grooming/booking'
    },
    {
      id: 'vet',
      title: 'Khám Thú Y',
      description: 'Đội ngũ bác sĩ thú y giàu kinh nghiệm, trang thiết bị hiện đại, chuẩn đoán và điều trị chính xác.',
      icon: <FiActivity className="text-blue-500" size={32} />,
      image: mockImages.vet,
      link: '/services/vet/booking'
    },
    {
      id: 'hotel',
      title: 'Khách Sạn Thú Cưng',
      description: 'Không gian lưu trú an toàn, sạch sẽ, có chế độ dinh dưỡng và vận động riêng biệt cho từng bé.',
      icon: <MdPets className="text-orange-500" size={32} />,
      image: mockImages.placeholder,
      link: '/services'
    }
  ];

  return (
    <div className="bg-background min-h-screen py-10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        <div className="text-center max-w-2xl mx-auto mb-12">
          <h1 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">Dịch vụ chăm sóc thú cưng</h1>
          <p className="text-gray-600">Chúng tôi cung cấp các dịch vụ chất lượng cao nhất để đảm bảo thú cưng của bạn luôn khỏe mạnh và hạnh phúc.</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {services.map((service, idx) => (
            <Card key={idx} noPadding className="flex flex-col h-full hover:shadow-lg hover:-translate-y-1 transition-all duration-300">
              <div className="h-48 overflow-hidden relative">
                <img src={service.image} alt={service.title} className="w-full h-full object-cover" />
                <div className="absolute -bottom-6 right-6 w-12 h-12 bg-white rounded-2xl shadow-sm flex items-center justify-center">
                  {service.icon}
                </div>
              </div>
              <div className="p-6 pt-8 flex-1 flex flex-col">
                <h3 className="text-xl font-bold text-gray-900 mb-3">{service.title}</h3>
                <p className="text-gray-600 mb-6 flex-1">{service.description}</p>
                
                <Link to={service.link}>
                  <Button fullWidth variant={idx === 0 ? 'primary' : 'outline'}>
                    Đặt lịch ngay
                  </Button>
                </Link>
              </div>
            </Card>
          ))}
        </div>

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