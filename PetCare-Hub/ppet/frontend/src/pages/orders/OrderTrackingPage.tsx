import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '../../components/common/Button';
import { FiBox, FiCheckCircle, FiTruck, FiHome } from 'react-icons/fi';
import { mockProducts } from '../../data/mockProducts';

export const OrderTrackingPage: React.FC = () => {
  const steps = [
    { label: 'Đã đặt', date: '10/05/2026', icon: <FiBox />, active: true, completed: true },
    { label: 'Đang xử lý', date: '11/05/2026', icon: <FiCheckCircle />, active: true, completed: true },
    { label: 'Đang giao', date: '12/05/2026', icon: <FiTruck />, active: true, completed: false },
    { label: 'Đã giao', date: '', icon: <FiHome />, active: false, completed: false },
  ];

  return (
    <div className="bg-background min-h-screen py-10">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">

        <div className="flex items-center justify-between mb-8">
          <h1 className="text-2xl font-bold text-gray-900">Đơn hàng #DH123456</h1>
          <Badge variant="primary">Đang giao hàng</Badge>
        </div>

        <div className="bg-white rounded-3xl p-8 shadow-sm border border-gray-100 mb-8">
          {/* Tracking Pipeline */}
          <div className="flex flex-col md:flex-row items-center justify-between relative mb-12">
            <div className="hidden md:block absolute left-10 right-10 top-1/2 -translate-y-1/2 h-1 bg-gray-100 rounded-full z-0"></div>
            <div className="hidden md:block absolute left-10 right-10 top-1/2 -translate-y-1/2 h-1 bg-primary-600 rounded-full z-0 w-[66%]"></div>

            {steps.map((step, idx) => (
              <div key={idx} className="relative z-10 flex flex-col items-center mb-6 md:mb-0 w-full md:w-auto">
                <div
                  className={`w-12 h-12 rounded-full flex items-center justify-center text-xl mb-3 ${
                    step.completed
                      ? 'bg-primary-600 text-white'
                      : step.active
                      ? 'bg-white text-primary-600 border-2 border-primary-600'
                      : 'bg-gray-100 text-gray-400'
                  }`}
                >
                  {step.icon}
                </div>
                <span
                  className={`font-semibold text-sm ${
                    step.active ? 'text-gray-900' : 'text-gray-400'
                  }`}
                >
                  {step.label}
                </span>
                {step.date && <span className="text-xs text-gray-500 mt-1">{step.date}</span>}
              </div>
            ))}
          </div>

          <div className="bg-primary-50 rounded-2xl p-5 mb-8 flex items-center">
            <FiTruck className="text-primary-600 text-2xl mr-4" />
            <div>
              <p className="font-semibold text-gray-900">Thời gian dự kiến giao: 12/05/2026</p>
              <p className="text-sm text-gray-600">Đơn hàng của bạn đang được shipper vận chuyển đến địa chỉ nhận.</p>
            </div>
          </div>

          {/* Order Details */}
          <h3 className="font-bold text-gray-900 mb-4 border-b border-gray-100 pb-2">Chi tiết sản phẩm</h3>
          <div className="space-y-4 mb-6">
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 bg-gray-50 rounded-lg p-1">
                <img
                  src={mockProducts[0].image}
                  alt={mockProducts[0].name}
                  className="w-full h-full object-contain mix-blend-multiply"
                />
              </div>
              <div className="flex-1">
                <p className="font-medium text-gray-900">{mockProducts[0].name}</p>
                <p className="text-sm text-gray-500">Số lượng: 1</p>
              </div>
              <div className="font-bold text-gray-900">1.200.000đ</div>
            </div>
          </div>

          <div className="flex justify-between items-center pt-6 border-t border-gray-100">
            <span className="font-medium text-gray-600">Tổng cộng (đã thanh toán):</span>
            <span className="font-bold text-xl text-primary-600">1.230.000đ</span>
          </div>
        </div>

        <div className="flex justify-center">
          <Link to="/">
            <Button variant="outline">Về trang chủ</Button>
          </Link>
        </div>

      </div>
    </div>
  );
};

// Temp badge since it's not exported in the context of this single file script execution environment easily without full imports
const Badge: React.FC<{ children: React.ReactNode; variant?: string }> = ({ children }) => (
  <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800 border border-primary-200">
    {children}
  </span>
);