import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { petManagementService, PetDTO, VaccinationDTO } from '../../services/petManagementService';

export const PetVaccinationPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [pet, setPet] = useState<PetDTO | null>(null);
  const [vaccinations, setVaccinations] = useState<VaccinationDTO[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      try {
        const petData = await petManagementService.getPetById(Number(id));
        setPet(petData);
        
        const vaxData = await petManagementService.getVaccinations(Number(id));
        setVaccinations(vaxData);
      } catch (error) {
        console.error(error);
      } finally {
        setLoading(false);
      }
    };
    if (id) loadData();
  }, [id]);

  if (loading) {
    return <div className="min-h-screen py-20 flex justify-center"><div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div></div>;
  }

  if (!pet) return <div>Không tìm thấy thú cưng</div>;

  return (
    <div className="bg-background min-h-screen py-10">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-8 flex items-center gap-4">
          <img src={pet.imageUrl || 'https://images.unsplash.com/photo-1543466835-00a7907e9de1'} alt={pet.name} className="w-12 h-12 rounded-full object-cover" />
          Lịch sử tiêm phòng - {pet.name}
        </h1>

        <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-8 mb-8">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-xl font-bold text-gray-900">Danh sách vaccine đã tiêm</h2>
          </div>
          
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50 rounded-t-xl">
                <tr>
                  <th scope="col" className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider rounded-tl-xl">
                    Tên Vaccine
                  </th>
                  <th scope="col" className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                    Lô sản xuất
                  </th>
                  <th scope="col" className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                    Bác sĩ
                  </th>
                  <th scope="col" className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                    Ngày tiêm
                  </th>
                  <th scope="col" className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider rounded-tr-xl">
                    Tiêm nhắc lại
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {vaccinations.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-6 py-8 text-center text-gray-500">
                      Chưa có lịch sử tiêm phòng nào.
                    </td>
                  </tr>
                ) : (
                  vaccinations.map(vax => (
                    <tr key={vax.id} className="hover:bg-gray-50 transition-colors">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="font-medium text-gray-900">{vax.vaccineName}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {vax.vaccineBatchNumber || '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {vax.veterinarianName}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 font-medium">
                        {new Date(vax.dateAdministered).toLocaleDateString('vi-VN')}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {vax.nextDueDate ? (
                          <span className="inline-block px-2.5 py-1 text-xs font-medium text-orange-700 bg-orange-100 rounded-full">
                            {new Date(vax.nextDueDate).toLocaleDateString('vi-VN')}
                          </span>
                        ) : (
                          <span className="text-gray-400">-</span>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};