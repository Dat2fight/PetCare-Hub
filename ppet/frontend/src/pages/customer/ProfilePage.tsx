import React from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { FiUser, FiMail, FiShield } from 'react-icons/fi';

export const ProfilePage: React.FC = () => {
  const { user } = useAuth();

  if (!user) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <div className="bg-gray-50 min-h-screen py-12">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 className="text-3xl font-extrabold text-gray-900 mb-8">Hồ sơ cá nhân</h1>

        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
          <div className="p-8">
            <div className="flex items-center space-x-6 mb-8">
              <img
                src={user.avatarUrl || `https://placehold.co/150x150/ecfdf5/047857?text=${user.firstName[0]}`}
                alt="Profile"
                className="h-24 w-24 rounded-full border-4 border-primary-50 object-cover"
              />
              <div>
                <h2 className="text-2xl font-bold text-gray-900">{user.firstName} {user.lastName}</h2>
                <p className="text-gray-500 flex items-center mt-1">
                  <FiMail className="mr-2" />
                  {user.email}
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <div>
                <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                  <FiUser className="mr-2 text-primary-500" />
                  Thông tin cá nhân
                </h3>
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-500">Tên đăng nhập</label>
                    <p className="mt-1 text-gray-900 font-medium">{user.username}</p>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-500">Họ và tên</label>
                    <p className="mt-1 text-gray-900 font-medium">{user.firstName} {user.lastName}</p>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-500">Email</label>
                    <p className="mt-1 text-gray-900 font-medium">{user.email}</p>
                  </div>
                </div>
              </div>

              <div>
                <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                  <FiShield className="mr-2 text-primary-500" />
                  Bảo mật & Quyền hạn
                </h3>
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-500">Quyền hạn</label>
                    <div className="mt-2 flex flex-wrap gap-2">
                      {user.roles && user.roles.map(role => (
                        <span key={role} className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-primary-100 text-primary-800">
                          {role.replace('ROLE_', '')}
                        </span>
                      ))}
                      {(!user.roles || user.roles.length === 0) && (
                        <span className="text-gray-500">USER</span>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <div className="mt-10 pt-6 border-t border-gray-100 flex justify-end">
              <button className="bg-primary-600 text-white px-6 py-2.5 rounded-xl font-medium hover:bg-primary-700 transition-colors shadow-sm">
                Cập nhật hồ sơ
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

