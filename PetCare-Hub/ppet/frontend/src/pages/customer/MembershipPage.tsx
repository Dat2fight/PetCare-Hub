import React from 'react';
import { Card } from '../../components/common/Card';
import { FiCheckCircle } from 'react-icons/fi';

export const MembershipPage: React.FC = () => {
  const levels = [
    { name: 'Bronze', points: '0 - 499 điểm', color: 'bg-orange-100 text-orange-700', active: false },
    { name: 'Silver', points: '500 - 999 điểm', color: 'bg-gray-200 text-gray-700', active: false },
    { name: 'Gold', points: '1000 - 4999 điểm', color: 'bg-yellow-100 text-yellow-700', active: true },
    { name: 'Platinum', points: '5000+ điểm', color: 'bg-blue-100 text-blue-700', active: false },
  ];

  return (
    <div className="bg-background min-h-screen py-10">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Hạng thành viên</h1>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
          {levels.map((level, idx) => (
            <Card
              key={idx}
              className={`relative overflow-hidden ${
                level.active ? 'border-2 border-primary-500 shadow-md' : ''
              }`}
            >
              {level.active && <div className="absolute top-0 inset-x-0 h-1 bg-primary-500"></div>}
              <div className={`inline-block px-3 py-1 rounded-full text-sm font-bold mb-4 ${level.color}`}>
                {level.name}
              </div>
              <h3 className="font-bold text-gray-900 mb-1">{level.points}</h3>
              {level.active && (
                <p className="text-primary-600 text-sm font-medium flex items-center">
                  <FiCheckCircle className="mr-1" /> Hạng hiện tại của bạn
                </p>
              )}
            </Card>
          ))}
        </div>

        <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-8">
          <h2 className="text-xl font-bold text-gray-900 mb-6">Ưu đãi đặc quyền hạng Gold</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="flex items-center p-4 border border-gray-100 rounded-2xl">
              <span className="text-2xl mr-4">🛍️</span>
              <div>
                <h4 className="font-bold text-gray-900">Giảm 10% khi mua hàng</h4>
                <p className="text-sm text-gray-500">Áp dụng cho mọi đơn hàng phụ kiện, thức ăn.</p>
              </div>
            </div>
            <div className="flex items-center p-4 border border-gray-100 rounded-2xl">
              <span className="text-2xl mr-4">✂️</span>
              <div>
                <h4 className="font-bold text-gray-900">Giảm 20% Grooming</h4>
                <p className="text-sm text-gray-500">Áp dụng mỗi tháng 1 lần.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};