import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../../components/common/Button';
import { Card } from '../../components/common/Card';
import { mockImages } from '../../data/mockImages';
import { mockPets } from '../../data/mockPets';
import { FiHome, FiCheck } from 'react-icons/fi';
import { FaCity, FaTree } from 'react-icons/fa';

export const PetMatchingPage: React.FC = () => {
  const [step, setStep] = useState(1);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [isCalculating, setIsCalculating] = useState(false);
  const navigate = useNavigate();

  const handleSelect = (questionId: string, answer: string) => {
    setAnswers({ ...answers, [questionId]: answer });
    setTimeout(() => {
      if (step < 3) {
        setStep(step + 1);
      } else {
        setIsCalculating(true);
        setTimeout(() => setStep(4), 2000);
      }
    }, 400);
  };

  const getMatchedPet = () => mockPets[1]; // Return Poodle as a demo match

  return (
    <div className="bg-background min-h-screen py-12">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">

        {step < 4 && (
          <div className="text-center mb-10">
            <h1 className="text-3xl font-bold text-gray-900 mb-4">Quiz tìm thú cưng phù hợp</h1>
            <p className="text-gray-500">Trả lời 3 câu hỏi đơn giản để tìm người bạn bốn chân lý tưởng!</p>

            <div className="flex justify-center mt-8 gap-2">
              {[1, 2, 3].map(s => (
                <div
                  key={s}
                  className={`h-2 rounded-full transition-all duration-500 ${
                    s === step ? 'w-8 bg-primary-600' : s < step ? 'w-2 bg-primary-300' : 'w-2 bg-gray-300'
                  }`}
                ></div>
              ))}
            </div>
          </div>
        )}

        <div className="relative min-h-[400px]">
          {/* Step 1 */}
          <div
            className={`transition-all duration-500 absolute w-full ${
              step === 1 ? 'opacity-100 translate-x-0 z-10' : 'opacity-0 -translate-x-full pointer-events-none'
            }`}
          >
            <h2 className="text-2xl font-bold text-gray-900 mb-8 text-center">1. Bạn sống ở đâu?</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {[
                { id: 'apt', label: 'Căn hộ chung cư', icon: <FaCity size={40} /> },
                { id: 'house', label: 'Nhà phố', icon: <FiHome size={40} /> },
                { id: 'yard', label: 'Nhà có sân vườn', icon: <FaTree size={40} /> },
              ].map(opt => (
                <button
                  key={opt.id}
                  onClick={() => handleSelect('home', opt.id)}
                  className={`flex flex-col items-center p-8 rounded-3xl border-2 transition-all ${
                    answers.home === opt.id
                      ? 'border-primary-500 bg-primary-50 text-primary-700'
                      : 'border-gray-100 bg-white text-gray-600 hover:border-primary-200 hover:shadow-md'
                  }`}
                >
                  <div className="mb-4">{opt.icon}</div>
                  <span className="font-bold">{opt.label}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Step 2 & 3 would go here similarly - skipping to calculation for brevity */}
          <div
            className={`transition-all duration-500 absolute w-full flex flex-col items-center justify-center h-full ${
              isCalculating && step < 4 ? 'opacity-100 z-10' : 'opacity-0 pointer-events-none'
            }`}
          >
            <div className="w-20 h-20 border-4 border-gray-200 border-t-primary-600 rounded-full animate-spin mb-6"></div>
            <h2 className="text-2xl font-bold text-gray-900">Đang phân tích dữ liệu...</h2>
            <p className="text-gray-500 mt-2">Chúng tôi đang tìm người bạn phù hợp nhất với bạn</p>
          </div>

          {/* Result Step 4 */}
          <div
            className={`transition-all duration-500 absolute w-full ${
              step === 4 ? 'opacity-100 translate-y-0 z-10' : 'opacity-0 translate-y-8 pointer-events-none'
            }`}
          >
            <div className="text-center mb-10">
              <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-green-100 text-green-500 mb-4">
                <FiCheck size={32} />
              </div>
              <h1 className="text-3xl font-bold text-gray-900 mb-2">Chúc mừng! Đây là gợi ý cho bạn</h1>
              <p className="text-gray-500">
                Dựa trên câu trả lời, bé {getMatchedPet().breed} là sự lựa chọn hoàn hảo (độ tương thích 98%).
              </p>
            </div>

            <Card className="flex flex-col md:flex-row overflow-hidden border-2 border-primary-200 shadow-xl" noPadding>
              <div className="md:w-1/2 relative h-64 md:h-auto">
                <img src={getMatchedPet().image} alt={getMatchedPet().name} className="w-full h-full object-cover" />
                <div className="absolute top-4 left-4 bg-primary-600 text-white font-bold px-4 py-2 rounded-full shadow-lg">
                  98% Match
                </div>
              </div>
              <div className="md:w-1/2 p-8 lg:p-12 flex flex-col justify-center">
                <h2 className="text-3xl font-bold text-gray-900 mb-2">{getMatchedPet().name}</h2>
                <p className="text-primary-600 font-semibold mb-6">{getMatchedPet().breed}</p>

                <h3 className="font-bold text-gray-900 mb-3">Tại sao bé phù hợp với bạn?</h3>
                <ul className="space-y-3 mb-8">
                  <li className="flex items-start">
                    <FiCheck className="text-green-500 mt-1 mr-3 flex-shrink-0" />
                    <span className="text-gray-600">Phù hợp hoàn hảo với không gian căn hộ</span>
                  </li>
                  <li className="flex items-start">
                    <FiCheck className="text-green-500 mt-1 mr-3 flex-shrink-0" />
                    <span className="text-gray-600">Mức độ vận động vừa phải, hợp với người bận rộn</span>
                  </li>
                  <li className="flex items-start">
                    <FiCheck className="text-green-500 mt-1 mr-3 flex-shrink-0" />
                    <span className="text-gray-600">Rất tình cảm và ít rụng lông</span>
                  </li>
                </ul>

                <Button size="lg" onClick={() => navigate('/pets/' + getMatchedPet().id)}>
                  Xem chi tiết bé
                </Button>
              </div>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
};