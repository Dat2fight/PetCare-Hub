import { mockImages } from './mockImages';

export interface Product {
  id: string;
  name: string;
  category: string;
  price: number;
  discount?: number; // discount amount
  rating: number;
  reviewCount: number;
  image: string;
  images: string[];
  inStock: boolean;
  description: string;
}

export const mockProducts: Product[] = [
  {
    id: '1',
    name: 'Th?c an Royal Canin Adult Cat 10kg',
    category: 'Th?c an',
    price: 1300000,
    discount: 100000,
    rating: 4.8,
    reviewCount: 130,
    image: mockImages.foodRoyalCanin,
    images: [mockImages.foodRoyalCanin, mockImages.foodRoyalCanin],
    inStock: true,
    description: 'Th?c an h?t khô dành cho mèo tru?ng thành. Giúp duy trì vóc dáng, làm mu?t lông và h? tr? tiêu hóa t?t.'
  },
  {
    id: '2',
    name: 'Vòng c? cao c?p có chuông',
    category: 'Ph? ki?n',
    price: 250000,
    rating: 4.5,
    reviewCount: 45,
    image: mockImages.collar,
    images: [mockImages.collar],
    inStock: true,
    description: 'Vòng c? ch?t li?u da th?t 100%, an toàn không gây kích ?ng da c? thú cung, di kèm chuông nh? âm thanh thanh thúy.'
  },
  {
    id: '3',
    name: 'Ð? choi bóng cao su nhai g?m',
    category: 'Ð? choi',
    price: 120000,
    rating: 4.9,
    reviewCount: 200,
    image: mockImages.toyBall,
    images: [mockImages.toyBall],
    inStock: true,
    description: 'Bóng cao su thiên nhiên an toàn, giúp cún cung làm s?ch rang mi?ng và x? stress.'
  },
  {
    id: '4',
    name: 'L?ng v?n chuy?n hàng không',
    category: 'V? sinh & Khác',
    price: 800000,
    rating: 4.7,
    reviewCount: 89,
    image: mockImages.carrier,
    images: [mockImages.carrier],
    inStock: false,
    description: 'L?ng v?n chuy?n d?t tiêu chu?n hàng không qu?c t?, ch?t li?u nh?a ABS c?ng cáp ch?ng va d?p.'
  }
];
