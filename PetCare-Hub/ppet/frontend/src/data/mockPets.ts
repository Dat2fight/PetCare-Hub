import { mockImages } from './mockImages';

export interface Pet {
  id: string;
  name: string;
  species: 'Dog' | 'Cat' | 'Other';
  breed: string;
  age: string;
  gender: 'Đực' | 'Cái';
  weight: number;
  price: number;
  healthStatus: 'Tốt' | 'Đang điều trị' | 'Cần tiêm phòng';
  vaccinationStatus: 'Đã tiêm' | 'Chưa tiêm' | 'Thiếu mũi';
  description: string;
  personality: string[];
  careRequirements: string[];
  image: string;
  images: string[];
  location: string;
  isFavorite: boolean;
}

export const mockPets: Pet[] = [
  {
    id: '1',
    name: 'Corgi',
    species: 'Dog',
    breed: 'Chó Corgi',
    age: '2 tháng',
    gender: 'Đực',
    weight: 3.5,
    price: 9500000,
    healthStatus: 'Tốt',
    vaccinationStatus: 'Đã tiêm',
    description: 'Bé Corgi chân ngắn siêu đáng yêu, đã biết ăn hạt và đi vệ sinh đúng chỗ.',
    personality: ['Năng động', 'Thân thiện', 'Thông minh'],
    careRequirements: ['Chải lông hàng tuần', 'Vận động 30p mỗi ngày'],
    image: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSpC8efzZNSOq-NBByGVtjyo5Ei3j6cmBc9uZwMxZkBTQ&s',
    images: ['https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSpC8efzZNSOq-NBByGVtjyo5Ei3j6cmBc9uZwMxZkBTQ&s'],
    location: 'Hà Nội',
    isFavorite: false
  },
  {
    id: '2',
    name: 'Poodle',
    species: 'Dog',
    breed: 'Chó Poodle',
    age: '3 tháng',
    gender: 'Cái',
    weight: 2.2,
    price: 6000000,
    healthStatus: 'Tốt',
    vaccinationStatus: 'Đã tiêm',
    description: 'Bé Poodle size tiny, màu nâu đỏ cực xinh xắn, ngoan ngoãn.',
    personality: ['Tình cảm', 'Dễ huấn luyện', 'Không rụng lông'],
    careRequirements: ['Cắt tỉa lông mỗi 2 tháng', 'Lau mặt hàng ngày'],
    image: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTYzd1XPSMGX0sI6hMxeUrw4OWgsFmLSSvwv_aHnbh-rw&s=10',
    images: ['https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTYzd1XPSMGX0sI6hMxeUrw4OWgsFmLSSvwv_aHnbh-rw&s=10'],
    location: 'TP.HCM',
    isFavorite: true
  },
  {
    id: '3',
    name: 'Mèo Anh Lông Ngắn',
    species: 'Cat',
    breed: 'Mèo ALN',
    age: '2.5 tháng',
    gender: 'Đực',
    weight: 1.8,
    price: 5500000,
    healthStatus: 'Tốt',
    vaccinationStatus: 'Đã tiêm',
    description: 'Bé ALN màu xám xanh, mặt bánh bao, cực kỳ quấn người.',
    personality: ['Điềm tĩnh', 'Thích vuốt ve', 'Độc lập'],
    careRequirements: ['Chải lông 2 lần/tuần', 'Chế độ ăn kiểm soát cân nặng'],
    image: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR1ZklG_FJA3mSDoTbe46cvQ-4YTL1-Eo2MJDHQWqrzew&s=10',
    images: ['https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR1ZklG_FJA3mSDoTbe46cvQ-4YTL1-Eo2MJDHQWqrzew&s=10'],
    location: 'Đà Nẵng',
    isFavorite: false
  },
  {
    id: '4',
    name: 'Golden Retriever',
    species: 'Dog',
    breed: 'Chó Golden',
    age: '2 tháng',
    gender: 'Đực',
    weight: 6.0,
    price: 12000000,
    healthStatus: 'Tốt',
    vaccinationStatus: 'Chưa tiêm',
    description: 'Bé Golden khung to, mặt vuông, form chuẩn chó đi show.',
    personality: ['Trung thành', 'Rất thân thiện', 'Hòa đồng với trẻ em'],
    careRequirements: ['Không gian rộng', 'Vận động nhiều mỗi ngày'],
    image: mockImages.dogGolden,
    images: [mockImages.dogGolden, mockImages.dogHero],
    location: 'Hà Nội',
    isFavorite: false
  },
  {
    id: '5',
    name: 'Mèo Ba Tư',
    species: 'Cat',
    breed: 'Mèo Ba Tư',
    age: '4 tháng',
    gender: 'Cái',
    weight: 2.5,
    price: 8000000,
    healthStatus: 'Cần tiêm phòng',
    vaccinationStatus: 'Thiếu mũi',
    description: 'Bé mèo Ba Tư mặt tịt, lông dài óng mượt.',
    personality: ['Yên tĩnh', 'Thích ngủ', 'Dễ chịu'],
    careRequirements: ['Chải lông hàng ngày', 'Vệ sinh mặt thường xuyên'],
    image: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQhN3Eq4wEJeCTUajZ_BuwyEGpRF-EZPskFSM3yHRlRHg&s=10',
    images: ['https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQhN3Eq4wEJeCTUajZ_BuwyEGpRF-EZPskFSM3yHRlRHg&s=10'],
    location: 'TP.HCM',
    isFavorite: false
  }
];