import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { mockProducts } from '../../data/mockProducts';
import { FiCheckCircle, FiCreditCard, FiTruck } from 'react-icons/fi';

export const CheckoutPage: React.FC = () => {
  const navigate = useNavigate();
  const [paymentMethod, setPaymentMethod] = useState('cod');

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const handlePlaceOrder = (e: React.FormEvent) => {
    e.preventDefault();
    navigate('/orders/tracking');
  };

  return (
    <div className="bg-background min-h-screen py-10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

        {/* Progress Steps */}
        <div className="max-w-3xl mx-auto mb-10">
          <div className="flex items-center justify-between relative">
            <div className="absolute left-0 top-1/2 -translate-y-1/2 w-full h-1 bg-gray-200 rounded-full z-0"></div>
            <div className="absolute left-0 top-1/2 -translate-y-1/2 w-1/2 h-1 bg-primary-600 rounded-full z-0"></div>

            <div className="relative z-10 flex flex-col items-center">
              <div className="w-10 h-10 rounded-full bg-primary-600 text-white flex items-center justify-center font-bold shadow-md"><FiCheckCircle /></div>
              <span className="mt-2 text-sm font-semibold text-primary-600">Giỏ hàng</span>
            </div>
            <div className="relative z-10 flex flex-col items-center">
              <div className="w-10 h-10 rounded-full bg-primary-600 text-white flex items-center justify-center font-bold shadow-md">2</div>
              <span className="mt-2 text-sm font-semibold text-primary-600">Thanh toán</span>
            </div>
            <div className="relative z-10 flex flex-col items-center">
              <div className="w-10 h-10 rounded-full bg-white border-2 border-gray-200 text-gray-400 flex items-center justify-center font-bold">3</div>
              <span className="mt-2 text-sm font-medium text-gray-400">Hoàn tất</span>
            </div>
          </div>
        </div>

        <form onSubmit={handlePlaceOrder} className="flex flex-col lg:flex-row gap-8">

          <div className="lg:w-2/3 space-y-6">
            <div className="bg-white rounded-3xl p-8 shadow-sm border border-gray-100">
              <h2 className="text-xl font-bold text-gray-900 mb-6 flex items-center">
                <FiTruck className="mr-3 text-primary-600" /> Thông tin giao hàng
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <Input label="Họ tên người nhận" required placeholder="Nguyễn Văn A" />
                <Input label="Số điện thoại" required placeholder="0901234567" />
                <div className="md:col-span-2">
                  <Input label="Địa chỉ cụ thể" required placeholder="Số nhà, tên đường..." />
                </div>
                <Input label="Tỉnh/Thành phố" required placeholder="Hà Nội" />
                <Input label="Quận/Huyện" required placeholder="Cầu Giấy" />
              </div>
            </div>

            <div className="bg-white rounded-3xl p-8 shadow-sm border border-gray-100">
              <h2 className="text-xl font-bold text-gray-900 mb-6 flex items-center">
                <FiCreditCard className="mr-3 text-primary-600" /> Phương thức thanh toán
              </h2>
              <div className="space-y-4">
                <label
                  className={`flex items-center p-4 border rounded-2xl cursor-pointer transition-colors ${
                    paymentMethod === 'cod'
                      ? 'border-primary-600 bg-primary-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <input
                    type="radio"
                    name="payment"
                    value="cod"
                    checked={paymentMethod === 'cod'}
                    onChange={() => setPaymentMethod('cod')}
                    className="mr-4 w-5 h-5 text-primary-600 focus:ring-primary-500"
                  />
                  <div>
                    <div className="font-semibold text-gray-900">Thanh toán khi nhận hàng (COD)</div>
                    <div className="text-sm text-gray-500">Trả tiền mặt khi giao hàng</div>
                  </div>
                </label>
                <label
                  className={`flex items-center p-4 border rounded-2xl cursor-pointer transition-colors ${
                    paymentMethod === 'bank'
                      ? 'border-primary-600 bg-primary-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <input
                    type="radio"
                    name="payment"
                    value="bank"
                    checked={paymentMethod === 'bank'}
                    onChange={() => setPaymentMethod('bank')}
                    className="mr-4 w-5 h-5 text-primary-600 focus:ring-primary-500"
                  />
                  <div>
                    <div className="font-semibold text-gray-900">Chuyển khoản ngân hàng</div>
                    <div className="text-sm text-gray-500">Chuyển khoản qua mã QR</div>
                  </div>
                </label>
              </div>
            </div>
          </div>

          {/* Summary */}
          <div className="lg:w-1/3">
            <div className="bg-white rounded-3xl p-6 shadow-card border border-gray-100 sticky top-28">
              <h2 className="text-xl font-bold text-gray-900 mb-4">Đơn hàng (3 sản phẩm)</h2>

              <div className="space-y-4 mb-6">
                <div className="flex items-center gap-3 border-b border-gray-50 pb-3">
                  <div className="w-16 h-16 bg-gray-50 rounded-lg p-1">
                    <img
                      src={mockProducts[0].image}
                      alt={mockProducts[0].name}
                      className="w-full h-full object-contain mix-blend-multiply"
                    />
                  </div>
                  <div className="flex-1">
                    <p className="text-sm font-medium text-gray-900 line-clamp-1">{mockProducts[0].name}</p>
                    <p className="text-xs text-gray-500">SL: 1</p>
                    <p className="text-sm font-bold text-primary-600 mt-1">{formatPrice(mockProducts[0].price)}</p>
                  </div>
                </div>
              </div>

              <div className="space-y-3 text-sm text-gray-600 mb-6">
                <div className="flex justify-between">
                  <span>Tạm tính</span>
                  <span className="font-medium text-gray-900">{formatPrice(1550000)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Phí vận chuyển</span>
                  <span className="font-medium text-gray-900">{formatPrice(30000)}</span>
                </div>
              </div>

              <div className="border-t border-gray-100 pt-4 mb-6">
                <div className="flex justify-between items-center">
                  <span className="font-bold text-gray-900">Tổng cộng</span>
                  <span className="font-bold text-2xl text-primary-600">{formatPrice(1580000)}</span>
                </div>
              </div>

              <Button type="submit" fullWidth size="lg" className="py-4">
                Đặt hàng ({formatPrice(1580000)})
              </Button>
            </div>
          </div>

        </form>
      </div>
    </div>
  );
};