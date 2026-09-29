import React from 'react';
import { FiBox, FiCalendar, FiBell } from 'react-icons/fi';

export const NotificationsPage: React.FC = () => {
  return (
    <div className="bg-background min-h-screen py-10">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Thông báo</h1>
          <button className="text-primary-600 font-medium hover:underline">
            Đánh dấu tất cả đã đọc
          </button>
        </div>

        <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
          {[
            { id: 1, icon: <FiBox />, title: 'Đơn hàng #DH123456 đã được giao', time: '2 phút trước', color: 'bg-green-100 text-green-600', unread: true },
            { id: 2, icon: <FiCalendar />, title: 'Lịch hẹn Grooming sắp đến', time: '1 giờ trước', color: 'bg-blue-100 text-blue-600', unread: true },
            { id: 3, icon: <FiBell />, title: 'Bạn nhận được 120 điểm thưởng', time: '1 ngày trước', color: 'bg-yellow-100 text-yellow-600', unread: false },
          ].map(notif => (
            <div
              key={notif.id}
              className={`flex items-start p-6 border-b border-gray-50 transition-colors ${
                notif.unread ? 'bg-primary-50/30' : ''
              }`}
            >
              <div
                className={`w-12 h-12 rounded-full flex items-center justify-center mr-4 flex-shrink-0 ${notif.color}`}
              >
                {notif.icon}
              </div>
              <div className="flex-1">
                <h4
                  className={`text-base ${
                    notif.unread ? 'font-semibold text-gray-900' : 'font-medium text-gray-600'
                  }`}
                >
                  {notif.title}
                </h4>
                <p className="text-sm text-gray-500 mt-1">{notif.time}</p>
              </div>
              {notif.unread && (
                <div className="w-3 h-3 bg-primary-600 rounded-full mt-2"></div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};