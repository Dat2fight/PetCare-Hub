import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { petManagementService, PetDTO } from '../../services/petManagementService';

export const MyPetsPage: React.FC = () => {
  const [pets, setPets] = useState<PetDTO[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchPets = async () => {
      try {
        const data = await petManagementService.getMyPets();
        setPets(data);
      } catch (error) {
        console.error(error);
      } finally {
        setLoading(false);
      }
    };
    fetchPets();
  }, []);

  if (loading) {
    return <div className="min-h-screen py-20 flex justify-center"><div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div></div>;
  }

  return (
    <div className="bg-background min-h-screen py-10">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Thú cưng của tôi</h1>
          <Button>+ Thêm thú cưng</Button>
        </div>

        {pets.length === 0 ? (
          <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-8 text-center">
            <h2 className="text-xl font-bold text-gray-500 mb-4">Bạn chưa có thú cưng nào.</h2>
          </div>
        ) : (
          pets.map(pet => (
            <div key={pet.id} className="bg-white rounded-3xl shadow-sm border border-gray-100 p-8 flex flex-col md:flex-row gap-8 mb-8">
              <div className="flex flex-col items-center justify-center w-full md:w-1/4">
                <div className="w-32 h-32 rounded-full overflow-hidden border-4 border-primary-50 mb-4">
                  <img src={pet.imageUrl || 'https://images.unsplash.com/photo-1543466835-00a7907e9de1'} alt={pet.name} className="w-full h-full object-cover" />
                </div>
                <h2 className="text-2xl font-bold text-gray-900">{pet.name}</h2>
                <p className="text-gray-500 mb-2">{pet.breedName || 'Không rõ'} • {pet.age ? pet.age + ' tuổi' : 'Không rõ'}</p>
                <Badge variant={pet.healthStatus === 'HEALTHY' ? 'success' : 'warning'}>
                  {pet.healthStatus === 'HEALTHY' ? 'Khỏe mạnh' : 'Đang theo dõi'}
                </Badge>
              </div>

              <div className="w-full md:w-3/4">
                <div className="border-b border-gray-100 flex gap-6 mb-6">
                  <button className="pb-3 border-b-2 border-primary-600 text-primary-600 font-semibold text-lg">
                    Thông tin
                  </button>
                  <Link
                    to={`/my-pets/${pet.id}/health`}
                    className="pb-3 border-b-2 border-transparent text-gray-400 hover:text-gray-600 font-medium text-lg"
                  >
                    Sức khỏe
                  </Link>
                  <Link
                    to={`/my-pets/${pet.id}/vaccinations`}
                    className="pb-3 border-b-2 border-transparent text-gray-400 hover:text-gray-600 font-medium text-lg"
                  >
                    Lịch sử tiêm
                  </Link>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-y-4 gap-x-12">
                  <div className="flex justify-between border-b border-gray-50 pb-2">
                    <span className="text-gray-500">Giống loài:</span>
                    <span className="font-semibold text-gray-900">{pet.speciesName}</span>
                  </div>
                  <div className="flex justify-between border-b border-gray-50 pb-2">
                    <span className="text-gray-500">Trạng thái:</span>
                    <span className="font-semibold text-gray-900">{pet.availabilityStatus}</span>
                  </div>
                  <div className="flex justify-between border-b border-gray-50 pb-2">
                    <span className="text-gray-500">Ghi chú:</span>
                    <span className="font-semibold text-gray-900 line-clamp-1">{pet.description || 'Không có'}</span>
                  </div>
                </div>

                <div className="mt-8 flex justify-end">
                  <Button variant="outline">Chỉnh sửa thông tin</Button>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};