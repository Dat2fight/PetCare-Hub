import React from 'react';
import { Link, useParams } from 'react-router-dom';
import { mockPets } from '../../data/mockPets';

export const PetHealthHistoryPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const myPet = mockPets.find(p => p.id === id) || mockPets[0];

  return (
    <div className="bg-background min-h-screen py-10">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-8 flex items-center gap-4">
          <img src={myPet.image} alt={myPet.name} className="w-12 h-12 rounded-full object-cover" />
          L?ch s? s?c kh?e - {myPet.name}
        </h1>

        <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-8 mb-8">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-xl font-bold text-gray-900">Ti?n tr�nh kh�m ch?a b?nh</h2>
          </div>
          
          <div className="relative pl-6 border-l-2 border-primary-100 space-y-8">
            <div className="relative">
              <div className="absolute -left-[35px] top-1 w-6 h-6 rounded-full bg-green-100 flex items-center justify-center border-2 border-white">
                <div className="w-3 h-3 rounded-full bg-green-500"></div>
              </div>
              <div className="flex justify-between items-start">
                <div>
                  <h4 className="font-bold text-gray-900">Kh�m t?ng qu�t</h4>
                  <p className="text-sm text-gray-500 mt-1">Ph�ng kh�m th� y Th?o �i?n</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-semibold text-gray-900">10/04/2026</p>
                  <span className="inline-block mt-1 px-2.5 py-1 text-xs font-medium text-green-700 bg-green-100 rounded-full">S?c kh?e T?t</span>
                </div>
              </div>
            </div>
            
            <div className="relative">
              <div className="absolute -left-[35px] top-1 w-6 h-6 rounded-full bg-blue-100 flex items-center justify-center border-2 border-white">
                <div
                  key={s}
                  className={`h-2 rounded-full transition-all duration-500 ${
                    s === step ? 'w-8 bg-primary-500' : 'w-2 bg-gray-300'
                  }`}
                ></div>
              </div>
              <div className="flex justify-between items-start">
                <div>
                  <h4 className="font-bold text-gray-900">Ti�m ph�ng 5 b?nh</h4>
                  <p className="text-sm text-gray-500 mt-1">B�c si Nguy?n Van A</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-semibold text-gray-900">15/02/2026</p>
                  <span className="inline-block mt-1 px-2.5 py-1 text-xs font-medium text-blue-700 bg-blue-100 rounded-full">Vaccine Mui 1</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div className="text-center">
          <Link to="/my-pets" className="text-primary-600 font-medium hover:underline">? Quay l?i h? so chung</Link>
        </div>
      </div>
    </div>
  );
};
