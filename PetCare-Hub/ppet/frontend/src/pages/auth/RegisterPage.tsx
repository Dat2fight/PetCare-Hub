import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { mockImages } from '../../data/mockImages';
import apiClient from '../../api/client';
import toast from 'react-hot-toast';

export const RegisterPage: React.FC = () => {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: ''
  });
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (formData.password !== formData.confirmPassword) {
      toast.error('Mật khẩu không khớp!');
      return;
    }
    
    setIsLoading(true);
    try {
      await apiClient.post('/auth/register', {
        username: formData.email,
        email: formData.email,
        password: formData.password,
        firstName: formData.firstName,
        lastName: formData.lastName
      });
      toast.success('Đăng ký thành công!');
      navigate('/login');
    } catch (err) {
      toast.error('Đăng ký thất bại. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  return (
    <div className="min-h-[calc(100vh-80px)] bg-background flex">
      {/* Left side - Form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8 sm:p-12 lg:p-16">
        <div className="w-full max-w-md bg-white rounded-3xl shadow-soft p-8 border border-gray-100">
          <div className="text-center mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Đăng ký</h2>
            <p className="text-gray-500">Tạo tài khoản PetCare Hub của bạn</p>
          </div>
          
          <div className="flex border-b border-gray-200 mb-8">
            <Link to="/login" className="flex-1 pb-4 text-center text-gray-400 font-medium hover:text-gray-600 transition-colors">Đăng nhập</Link>
            <button className="flex-1 pb-4 text-primary-600 font-semibold border-b-2 border-primary-600">Đăng ký</button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Input label="Họ" name="lastName" placeholder="Nguyễn" value={formData.lastName} onChange={handleChange} required />
              <Input label="Tên" name="firstName" placeholder="Văn A" value={formData.firstName} onChange={handleChange} required />
            </div>
            
            <Input type="email" label="Email" name="email" placeholder="example@gmail.com" value={formData.email} onChange={handleChange} required />
            
            <Input type="password" label="Mật khẩu" name="password" placeholder="Tạo mật khẩu" value={formData.password} onChange={handleChange} required />
            
            <Input type="password" label="Xác nhận mật khẩu" name="confirmPassword" placeholder="Nhập lại mật khẩu" value={formData.confirmPassword} onChange={handleChange} required />

            <Button type="submit" fullWidth isLoading={isLoading} className="mt-8">
              Tạo tài khoản
            </Button>
          </form>
          
          <p className="mt-8 text-center text-sm text-gray-500">
            Đã có tài khoản? <Link to="/login" className="font-semibold text-primary-600 hover:text-primary-500">Đăng nhập</Link>
          </p>
        </div>
      </div>

      {/* Right side - Image */}
      <div className="hidden lg:flex lg:w-1/2 relative bg-primary-900 overflow-hidden">
        <div className="absolute inset-0 bg-primary-900/20 z-10"></div>
        <img 
          src={mockImages.dogPoodle} 
          alt="Happy Poodle" 
          className="absolute inset-0 w-full h-full object-cover opacity-90"
        />
        <div className="absolute bottom-0 left-0 right-0 p-12 z-20 bg-gradient-to-t from-black/80 to-transparent text-white">
          <h1 className="text-4xl font-bold mb-4">Cộng đồng yêu thú cưng</h1>
          <p className="text-xl text-gray-200">Hãy tham gia cùng chúng tôi để mang lại cuộc sống tốt đẹp nhất cho những người bạn bốn chân.</p>
        </div>
      </div>
    </div>
  );
};