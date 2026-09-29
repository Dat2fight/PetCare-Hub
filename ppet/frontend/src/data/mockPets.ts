import { mockImages } from './mockImages';

export interface Pet {
  id: string;
  name: string;
  species: 'Dog' | 'Cat' | 'Other';
  breed: string;
  age: string;
  gender: 'Ð?c' | 'Cái';
  weight: number;
  price: number;
  healthStatus: 'T?t' | 'Ðang di?u tr?' | 'C?n tiêm phòng';
  vaccinationStatus: 'Ðã tiêm' | 'Chua tiêm' | 'Thi?u mui';
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
    gender: 'Ð?c',
    weight: 3.5,
    price: 9500000,
    healthStatus: 'T?t',
    vaccinationStatus: 'Ðã tiêm',
    description: 'Bé Corgi chân ng?n siêu dáng yêu, dã bi?t an h?t và di v? sinh dúng ch?.',
    personality: ['Nang d?ng', 'Thân thi?n', 'Thông minh'],
    careRequirements: ['Ch?i lông hàng tu?n', 'V?n d?ng 30p m?i ngày'],
    image: mockImages.dogCorgi,
    images: [mockImages.dogCorgi, mockImages.dogCorgi, mockImages.dogCorgi],
    location: 'Hà N?i',
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
    healthStatus: 'T?t',
    vaccinationStatus: 'Ðã tiêm',
    description: 'Bé Poodle size tiny, màu nâu d? c?c xinh x?n, ngoan ngoãn.',
    personality: ['Tình c?m', 'D? hu?n luy?n', 'Không r?ng lông'],
    careRequirements: ['C?t t?a lông m?i 2 tháng', 'Lau m?t hàng ngày'],
    image: mockImages.dogPoodle,
    images: [mockImages.dogPoodle, mockImages.dogPoodle],
    location: 'TP.HCM',
    isFavorite: true
  },
  {
    id: '3',
    name: 'Mèo Anh Lông Ng?n',
    species: 'Cat',
    breed: 'Mèo ALN',
    age: '2.5 tháng',
    gender: 'Ð?c',
    weight: 1.8,
    price: 5500000,
    healthStatus: 'T?t',
    vaccinationStatus: 'Ðã tiêm',
    description: 'Bé ALN màu xám xanh, m?t bánh bao, c?c k? qu?n ngu?i.',
    personality: ['Ði?m tinh', 'Thích vu?t ve', 'Ð?c l?p'],
    careRequirements: ['Ch?i lông 2 l?n/tu?n', 'Ch? d? an ki?m soát cân n?ng'],
    image: mockImages.catScottish,
    images: [mockImages.catScottish, mockImages.catScottish, mockImages.catScottish],
    location: 'Ðà N?ng',
    isFavorite: false
  },
  {
    id: '4',
    name: 'Golden Retriever',
    species: 'Dog',
    breed: 'Chó Golden',
    age: '2 tháng',
    gender: 'Ð?c',
    weight: 6.0,
    price: 12000000,
    healthStatus: 'T?t',
    vaccinationStatus: 'Chua tiêm',
    description: 'Bé Golden khung to, m?t vuông, form chu?n chó di show.',
    personality: ['Trung thành', 'R?t thân thi?n', 'Hòa d?ng v?i tr? em'],
    careRequirements: ['Không gian r?ng', 'V?n d?ng nhi?u m?i ngày'],
    image: mockImages.dogGolden,
    images: [mockImages.dogGolden, mockImages.dogHero],
    location: 'Hà N?i',
    isFavorite: false
  },
  {
    id: '5',
    name: 'Mèo Ba Tu',
    species: 'Cat',
    breed: 'Mèo Ba Tu',
    age: '4 tháng',
    gender: 'Cái',
    weight: 2.5,
    price: 8000000,
    healthStatus: 'C?n tiêm phòng',
    vaccinationStatus: 'Thi?u mui',
    description: 'Bé mèo Ba Tu m?t t?t, lông dài óng mu?t.',
    personality: ['Yên tinh', 'Thích ng?', 'D? ch?u'],
    careRequirements: ['Ch?i lông hàng ngày', 'V? sinh m?t thu?ng xuyên'],
    image: mockImages.catPersian,
    images: [mockImages.catPersian],
    location: 'TP.HCM',
    isFavorite: false
  }
];
