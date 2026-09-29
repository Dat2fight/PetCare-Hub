import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import apiClient from '../../api/client';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { mockImages } from '../../data/mockImages';
import { FcGoogle } from 'react-icons/fc';
import { FaFacebook } from 'react-icons/fa';

export const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    
    try {
      const res = await apiClient.post('/auth/login', { username, password });
      await login(res.data.accessToken, res.data.user);
      navigate('/');
    } catch (err) {
      setError('Invalid credentials');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[calc(100vh-80px)] bg-background flex">
      {/* Left side - Image */}
      <div className="hidden lg:flex lg:w-1/2 relative bg-primary-900 overflow-hidden">
        <div className="absolute inset-0 bg-primary-900/20 z-10"></div>
        <img 
          src={mockImages.dogHero} 
          alt="Happy Golden Retriever and Cat" 
          className="absolute inset-0 w-full h-full object-cover opacity-90"
        />
        <div className="absolute bottom-0 left-0 right-0 p-12 z-20 bg-gradient-to-t from-black/80 to-transparent text-white">
          <h1 className="text-4xl font-bold mb-4">PetCare Hub</h1>
          <p className="text-xl text-gray-200">Better Care, Happier Pets</p>
        </div>
      </div>

      {/* Right side - Form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8 sm:p-12 lg:p-16">
        <div className="w-full max-w-md bg-white rounded-3xl shadow-soft p-8 border border-gray-100">
          <div className="text-center mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Ðang nh?p</h2>
            <p className="text-gray-500">Chào m?ng b?n quay l?i PetCare Hub!</p>
          </div>
          
          <div className="flex border-b border-gray-200 mb-8">
            <button className="flex-1 pb-4 text-primary-600 font-semibold border-b-2 border-primary-600">Ðang nh?p</button>
            <Link to="/register" className="flex-1 pb-4 text-center text-gray-400 font-medium hover:text-gray-600 transition-colors">Ðang ký</Link>
          </div>

          {error && (
            <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-100 text-red-700 text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <Input
              label="Email ho?c s? di?n tho?i"
              placeholder="Nh?p email ho?c s? di?n tho?i"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
            
            <Input
              type="password"
              label="M?t kh?u"
              placeholder="Nh?p m?t kh?u"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />

            <div className="flex items-center justify-between mt-2">
              <label className="flex items-center">
                <input type="checkbox" className="w-4 h-4 rounded text-primary-600 focus:ring-primary-500 border-gray-300" />
                <span className="ml-2 text-sm text-gray-600">Ghi nh? dang nh?p</span>
              </label>
              <a href="#" className="text-sm font-medium text-primary-600 hover:text-primary-500">Quên m?t kh?u?</a>
            </div>

            <Button type="submit" fullWidth isLoading={isLoading} className="mt-8">
              Ðang nh?p
            </Button>
          </form>

          <div className="mt-8 pt-6 border-t border-gray-100">
            <p className="text-center text-sm text-gray-500 mb-4">Ho?c dang nh?p b?ng</p>
            <div className="grid grid-cols-2 gap-4">
              <button className="flex items-center justify-center py-2.5 px-4 border border-gray-200 rounded-xl hover:bg-gray-50 transition-colors">
                <FcGoogle size={20} className="mr-2" />
                <span className="text-sm font-medium text-gray-700">Google</span>
              </button>
              <button className="flex items-center justify-center py-2.5 px-4 bg-[#1877F2] text-white rounded-xl hover:bg-[#1877F2]/90 transition-colors">
                <FaFacebook size={20} className="mr-2" />
                <span className="text-sm font-medium">Facebook</span>
              </button>
            </div>
          </div>
          
          <p className="mt-8 text-center text-sm text-gray-500">
            B?n chua có tài kho?n? <Link to="/register" className="font-semibold text-primary-600 hover:text-primary-500">Ðang ký ngay</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

