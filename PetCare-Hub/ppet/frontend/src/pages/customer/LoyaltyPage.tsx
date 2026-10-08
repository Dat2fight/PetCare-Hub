import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '../../components/common/Button';
import { FiGift, FiAward, FiClock } from 'react-icons/fi';

export const LoyaltyPage: React.FC = () => {
  return (
    <div className="bg-background min-h-screen py-10">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Điểm thưởng của bạn</h1>

        <div className="bg-gradient-to-r from-yellow-400 to-orange-400 rounded-3xl p-8 text-white mb-8 shadow-lg relative overflow-hidden">
          <div className="relative z-10 flex justify-between items-center">
            <div>
              <p className="text-yellow-100 font-medium mb-1">Thành viên Vàng</p>
              <div className="text-5xl font-bold flex items-center gap-3">
                <FiAward /> 1.250 <span className="text-2xl font-normal">điểm</span>
              </div>
            </div>
            <Link to="/membership">
              <Button className="bg-white text-orange-500 hover:bg-yellow-50">Đổi quà</Button>
            </Link>
          </div>
          <div className="absolute top-0 right-0 -mr-16 -mt-16 w-64 h-64 rounded-full bg-white opacity-10"></div>
          <div className="absolute bottom-0 left-0 -ml-16 -mb-16 w-48 h-48 rounded-full bg-white opacity-10"></div>
        </div>

        <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-8">
          <h2 className="text-xl font-bold text-gray-900 mb-6">Lịch sử tích điểm</h2>
          <div className="space-y-4">
            {[
              { title: 'Mua hàng (#DH123456)', points: '+120', date: '10/04/2026', type: 'earn' },
              { title: 'Đặt lịch grooming', points: '+50', date: '05/04/2026', type: 'earn' },
              { title: 'Đánh giá sản phẩm', points: '+20', date: '01/04/2026', type: 'earn' },
              { title: 'Đổi mã giảm giá', points: '-500', date: '15/03/2026', type: 'spend' },
            ].map((tx, idx) => (
              <div key={idx} className="flex items-center justify-between p-4 border-b border-gray-50 last:border-0">
                <div className="flex items-center gap-4">
                  <div
                    className={`w-10 h-10 rounded-full flex items-center justify-center ${
                      tx.type === 'earn' ? 'bg-green-100 text-green-600' : 'bg-red-100 text-red-500'
                    }`}
                  >
                    {tx.type === 'earn' ? <FiGift /> : <FiClock />}
                  </div>
                  <div>
                    <h4 className="font-semibold text-gray-900">{tx.title}</h4>
                    <p className="text-sm text-gray-500">{tx.date}</p>
                  </div>
                </div>
                <span
                  className={`font-bold ${
                    tx.type === 'earn' ? 'text-green-600' : 'text-red-500'
                  }`}
                >
                  {tx.points} điểm
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};