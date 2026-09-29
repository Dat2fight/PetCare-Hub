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
    name: 'Thức ăn Royal Canin Adult Cat 10kg',
    category: 'Thức ăn',
    price: 1300000,
    discount: 100000,
    rating: 4.8,
    reviewCount: 130,
    image: mockImages.foodRoyalCanin,
    images: [mockImages.foodRoyalCanin, mockImages.foodRoyalCanin],
    inStock: true,
    description: 'Thức ăn hạt khô dành cho mèo trưởng thành. Giúp duy trì vóc dáng, làm mượt lông và hỗ trợ tiêu hóa tốt.'
  },
  {
    id: '2',
    name: 'Vòng cổ cao cấp có chuông',
    category: 'Phụ kiện',
    price: 250000,
    rating: 4.5,
    reviewCount: 45,
    image: 'https://cutepetshop.vn/wp-content/uploads/2023/03/images-upload-woo2Fc695a0498dc5ce87244fbdfcbfe83bbb.jpg',
    images: ['https://cutepetshop.vn/wp-content/uploads/2023/03/images-upload-woo2Fc695a0498dc5ce87244fbdfcbfe83bbb.jpg'],
    inStock: true,
    description: 'Vòng cổ chất liệu da thật 100%, an toàn không gây kích ứng da cổ thú cưng, đi kèm chuông nhỏ âm thanh thanh thúy.'
  },
  {
    id: '3',
    name: 'Đồ chơi bóng cao su nhai gặm',
    category: 'Đồ chơi',
    price: 120000,
    rating: 4.9,
    reviewCount: 200,
    image: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSH0mY_Sb-w0y1CeulYqSqy97CJtEaIPV4T0hqIX4IJwA&s=10',
    images: ['https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSH0mY_Sb-w0y1CeulYqSqy97CJtEaIPV4T0hqIX4IJwA&s=10'],
    inStock: true,
    description: 'Bóng cao su thiên nhiên an toàn, giúp cún cưng làm sạch răng miệng và xả stress.'
  },
  {
    id: '4',
    name: 'Lồng vận chuyển hàng không',
    category: 'Vệ sinh & Khác',
    price: 800000,
    rating: 4.7,
    reviewCount: 89,
    // Thay thế trực tiếp bằng link ảnh chiếc lồng ở đây:
    image: 'https://thapxanh.com/images/thumbs/0013761.jpeg',
    images: ['https://thapxanh.com/images/thumbs/0013761.jpeg'],
    inStock: false,
    description: 'Lồng vận chuyển đạt tiêu chuẩn hàng không quốc tế, chất liệu nhựa ABS cứng cáp chống va đập.'
  }
];