import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '../../components/common/Button';
import { mockProducts } from '../../data/mockProducts';
import { FiTrash2, FiMinus, FiPlus, FiArrowRight } from 'react-icons/fi';

export const CartPage: React.FC = () => {
  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const cartItems = [
    { product: mockProducts[0], quantity: 1 },
    { product: mockProducts[1], quantity: 2 },
    { product: mockProducts[2], quantity: 1 }
  ];

  const subtotal = cartItems.reduce((acc, item) => acc + (item.product.price * item.quantity), 0);
  const discount = 100000;
  const total = subtotal - discount;

  return (
    <div className="bg-background min-h-screen py-10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Giỏ hàng <span className="text-lg font-normal text-gray-500">({cartItems.length} sản phẩm)</span></h1>

        <div className="flex flex-col lg:flex-row gap-8">
          
          {/* Cart Items */}
          <div className="lg:w-2/3 space-y-4">
            {cartItems.map((item, idx) => (
              <div key={idx} className="bg-white rounded-2xl p-4 sm:p-6 shadow-sm border border-gray-100 flex flex-col sm:flex-row items-center gap-6">
                <div className="w-24 h-24 bg-gray-50 rounded-xl overflow-hidden flex-shrink-0 p-2">
                  <img src={item.product.image} alt={item.product.name} className="w-full h-full object-contain mix-blend-multiply" />
                </div>
                
                <div className="flex-1 text-center sm:text-left w-full">
                  <Link to={`/products/${item.product.id}`}>
                    <h3 className="font-semibold text-gray-900 hover:text-primary-600 transition-colors line-clamp-1">{item.product.name}</h3>
                  </Link>
                  <div className="font-bold text-primary-600 mt-2">{formatPrice(item.product.price)}</div>
                </div>

                <div className="flex items-center gap-4 w-full sm:w-auto justify-between sm:justify-end">
                  <div className="flex items-center border border-gray-200 rounded-xl bg-gray-50">
                    <button className="p-2 text-gray-500 hover:text-primary-600 transition-colors"><FiMinus size={16}/></button>
                    <span className="w-8 text-center font-semibold text-gray-900">{item.quantity}</span>
                    <button className="p-2 text-gray-500 hover:text-primary-600 transition-colors"><FiPlus size={16}/></button>
                  </div>
                  
                  <button className="p-2 text-gray-400 hover:text-red-500 transition-colors bg-white rounded-lg">
                    <FiTrash2 size={20} />
                  </button>
                </div>
              </div>
            ))}

            <Link to="/products" className="inline-flex mt-4 text-primary-600 font-medium hover:text-primary-700 transition-colors items-center">
              + Tiếp tục mua sắm
            </Link>
          </div>

          {/* Summary */}
          <div className="lg:w-1/3">
            <div className="bg-white rounded-3xl p-6 shadow-card border border-gray-100 sticky top-28">
              <h2 className="text-xl font-bold text-gray-900 mb-6">Tóm tắt đơn hàng</h2>
              
              <div className="space-y-4 text-sm text-gray-600 mb-6">
                <div className="flex justify-between">
                  <span>Tạm tính</span>
                  <span className="font-medium text-gray-900">{formatPrice(subtotal)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Giảm giá</span>
                  <span className="font-medium text-red-500">-{formatPrice(discount)}</span>
                </div>
              </div>
              
              <div className="border-t border-gray-100 pt-4 mb-6">
                <div className="flex justify-between items-center">
                  <span className="font-bold text-gray-900">Tổng cộng</span>
                  <span className="font-bold text-2xl text-primary-600">{formatPrice(total)}</span>
                </div>
                <p className="text-xs text-gray-400 text-right mt-1">(Đã bao gồm VAT)</p>
              </div>

              <Link to="/checkout" className="block w-full">
                <Button fullWidth size="lg" className="py-4">
                  Thanh toán ngay <FiArrowRight className="ml-2" />
                </Button>
              </Link>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
};