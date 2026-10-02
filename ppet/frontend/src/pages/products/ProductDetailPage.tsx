import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { productService, Product } from '../../services/productService';
import { mockImages } from '../../data/mockImages';
import { Button } from '../../components/common/Button';
import { FiShoppingCart, FiStar, FiHeart, FiMinus, FiPlus, FiCheck } from 'react-icons/fi';
import LoadingSpinner from '../../components/common/LoadingSpinner';

export const ProductDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [activeImage, setActiveImage] = useState(mockImages.foodRoyalCanin);
  const [quantity, setQuantity] = useState(1);

  // Mock array for thumbnail preview since backend doesn't support multiple images yet
  const mockImageGallery = [
    mockImages.foodRoyalCanin,
    mockImages.toyBall,
    mockImages.collar
  ];

  useEffect(() => {
    const fetchProduct = async () => {
      try {
        setLoading(true);
        if (id) {
          const data = await productService.getProductById(parseInt(id));
          // Assign mock rating
          const displayProduct = {
            ...data,
            rating: 4.8
          };
          setProduct(displayProduct);
          setActiveImage(mockImages.foodRoyalCanin);
        }
      } catch (err) {
        console.error('Error fetching product:', err);
        setError('Không thể tải thông tin sản phẩm. Vui lòng thử lại sau.');
      } finally {
        setLoading(false);
      }
    };

    fetchProduct();
  }, [id]);

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  if (loading) {
    return (
      <div className="bg-background min-h-screen py-20 flex justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="bg-background min-h-screen py-20 flex flex-col items-center justify-center">
        <p className="text-xl text-gray-500 mb-4">{error || 'Không tìm thấy sản phẩm'}</p>
        <Link to="/products">
          <Button variant="outline">Quay lại cửa hàng</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="bg-background min-h-screen py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

        {/* Breadcrumb */}
        <nav className="flex text-sm text-gray-500 mb-8">
          <Link to="/" className="hover:text-primary-600 transition-colors">Trang chủ</Link>
          <span className="mx-2">/</span>
          <Link to="/products" className="hover:text-primary-600 transition-colors">Sản phẩm</Link>
          <span className="mx-2">/</span>
          <span className="text-gray-900 font-medium">Chi tiết</span>
        </nav>

        <div className="bg-white rounded-3xl shadow-soft border border-gray-100 overflow-hidden mb-12">
          <div className="flex flex-col lg:flex-row">

            {/* Left - Images */}
            <div className="w-full lg:w-1/2 p-6 lg:p-10 border-b lg:border-b-0 lg:border-r border-gray-100">
              <div className="relative aspect-square rounded-2xl overflow-hidden bg-white border border-gray-50 mb-6 flex items-center justify-center p-8">
                <img src={activeImage} alt={product.name} className="w-full h-full object-contain mix-blend-multiply" />
              </div>
              <div className="flex justify-center space-x-4">
                {mockImageGallery.map((img, idx) => (
                  <button
                    key={idx}
                    onClick={() => setActiveImage(img)}
                    className={`relative w-20 h-20 rounded-xl overflow-hidden border-2 transition-colors p-2 bg-white ${
                      activeImage === img ? 'border-primary-600' : 'border-gray-100'
                    }`}
                  >
                    <img src={img} alt="thumbnail" className="w-full h-full object-contain" />
                  </button>
                ))}
              </div>
            </div>

            {/* Right - Details */}
            <div className="w-full lg:w-1/2 p-6 lg:p-10 flex flex-col">
              <div className="flex items-center text-yellow-400 mb-3 text-sm">
                <FiStar fill="currentColor" />
                <FiStar fill="currentColor" />
                <FiStar fill="currentColor" />
                <FiStar fill="currentColor" />
                <FiStar fill="currentColor" className="text-gray-200" />
                <span className="text-gray-600 ml-2 font-medium">{product.rating} (124 đánh giá)</span>
              </div>

              <h1 className="text-3xl font-bold text-gray-900 mb-4">{product.name}</h1>

              <div className="mb-6 pb-6 border-b border-gray-100 flex items-end space-x-4">
                <span className="text-4xl font-bold text-primary-600">{formatPrice(product.price)}</span>
              </div>

              <div className="mb-8">
                <h3 className="font-semibold text-gray-900 mb-2">Đặc điểm nổi bật:</h3>
                <ul className="space-y-2 text-gray-600">
                  <li className="flex items-start"><FiCheck className="text-primary-500 mt-1 mr-2" /> Thích hợp cho mọi giống chó/mèo</li>
                  <li className="flex items-start"><FiCheck className="text-primary-500 mt-1 mr-2" /> Thành phần tự nhiên 100%</li>
                  <li className="flex items-start"><FiCheck className="text-primary-500 mt-1 mr-2" /> Đóng gói cẩn thận, bảo quản dễ dàng</li>
                </ul>
                <div className="mt-4 pt-4 border-t border-gray-50">
                  <h3 className="font-semibold text-gray-900 mb-2">Mô tả:</h3>
                  <p className="text-sm text-gray-600 leading-relaxed">{product.description}</p>
                </div>
              </div>

              <div className="flex items-center mb-8">
                <span className="font-semibold text-gray-900 w-24">Số lượng:</span>
                <div className="flex items-center border border-gray-200 rounded-xl bg-gray-50">
                  <button
                    onClick={() => setQuantity(Math.max(1, quantity - 1))}
                    className="p-3 text-gray-500 hover:text-primary-600 transition-colors"
                  >
                    <FiMinus />
                  </button>
                  <input
                    type="number"
                    value={quantity}
                    readOnly
                    className="w-12 text-center bg-transparent border-none font-semibold text-gray-900 focus:ring-0 p-0"
                  />
                  <button
                    onClick={() => setQuantity(Math.min(product.stockQuantity || 99, quantity + 1))}
                    className="p-3 text-gray-500 hover:text-primary-600 transition-colors"
                  >
                    <FiPlus />
                  </button>
                </div>
                <span className="ml-4 text-sm text-gray-500">
                  {product.stockQuantity > 0 ? `Còn hàng (${product.stockQuantity})` : 'Hết hàng'}
                </span>
              </div>

              {/* Action Buttons */}
              <div className="flex space-x-4 mt-auto">
                <Button size="lg" className="flex-1 py-4 text-lg" disabled={product.stockQuantity <= 0}>
                  <FiShoppingCart className="mr-2" /> Thêm vào giỏ hàng
                </Button>
                <button className="p-4 rounded-2xl border-2 border-gray-200 text-gray-400 hover:text-coral-500 hover:border-coral-500 transition-colors bg-white shadow-sm">
                  <FiHeart size={24} />
                </button>
              </div>

            </div>
          </div>
        </div>

      </div>
    </div>
  );
};