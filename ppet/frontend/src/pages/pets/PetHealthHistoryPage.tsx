import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { petManagementService, PetDTO, MedicalRecordDTO } from '../../services/petManagementService';

export const PetHealthHistoryPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [pet, setPet] = useState<PetDTO | null>(null);
  const [records, setRecords] = useState<MedicalRecordDTO[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      try {
        const petData = await petManagementService.getPetById(Number(id));
        setPet(petData);
        
        const recordsData = await petManagementService.getMedicalRecords(Number(id));
        setRecords(recordsData);
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
          Lịch sử sức khỏe - {pet.name}
        </h1>

        <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-8 mb-8">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-xl font-bold text-gray-900">Tiến trình khám chữa bệnh</h2>
          </div>
          
          <div className="relative pl-6 border-l-2 border-primary-100 space-y-8">
            {records.length === 0 ? (
              <p className="text-gray-500">Chưa có lịch sử khám bệnh nào.</p>
            ) : (
              records.map(record => (
                <div key={record.id} className="relative">
                  <div className="absolute -left-[35px] top-1 w-6 h-6 rounded-full bg-green-100 flex items-center justify-center border-2 border-white">
                    <div className="w-3 h-3 rounded-full bg-green-500"></div>
                  </div>
                  <div className="flex justify-between items-start">
                    <div>
                      <h4 className="font-bold text-gray-900">{record.diagnosis || 'Khám tổng quát'}</h4>
                      <p className="text-sm text-gray-500 mt-1">Bác sĩ: {record.veterinarianName}</p>
                      <p className="text-sm text-gray-700 mt-2"><strong>Kết luận:</strong> {record.findings}</p>
                      <p className="text-sm text-gray-700 mt-1"><strong>Khuyên dùng:</strong> {record.recommendations}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-semibold text-gray-900">{new Date(record.examinationDate).toLocaleDateString('vi-VN')}</p>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};