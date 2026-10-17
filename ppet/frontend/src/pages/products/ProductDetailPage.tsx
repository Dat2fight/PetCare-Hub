import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { productService, Product } from '../../services/productService';
import { ProductImage } from '../../components/common/ProductImage';
import { useAuth } from '../../contexts/AuthContext';
import { isAxiosError } from 'axios';
import toast from 'react-hot-toast';
import { Button } from '../../components/common/Button';
import { FiShoppingCart, FiHeart, FiMinus, FiPlus } from 'react-icons/fi';
import LoadingSpinner from '../../components/common/LoadingSpinner';

export const ProductDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [quantity, setQuantity] = useState(1);
  const [addingToCart, setAddingToCart] = useState(false);
  const addingRef = useRef(false);
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    setProduct(null);
    setQuantity(1);

    const fetchProduct = async () => {
      try {
        const productId = Number(id);
        if (!Number.isSafeInteger(productId) || productId <= 0) {
          setError('Sản phẩm không tồn tại');
          return;
        }
        const data = await productService.getProductById(productId);
        if (!cancelled) setProduct(data);
      } catch (err) {
        if (!cancelled) {
          setError(isAxiosError(err) && err.response?.status === 404
            ? 'Sản phẩm không tồn tại'
            : 'Không thể tải thông tin sản phẩm. Vui lòng thử lại sau.');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    fetchProduct();
    return () => { cancelled = true; };
  }, [id]);

  const handleAddToCart = async () => {
    if (!product || addingRef.current) return;
    if (!isAuthenticated) {
      toast.error('Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.');
      navigate('/login');
      return;
    }
    if (quantity < 1 || quantity > (product.stockQuantity ?? 0)) {
      toast.error('Số lượng vượt quá tồn kho hoặc sản phẩm đã hết hàng.');
      return;
    }
    addingRef.current = true;
    setAddingToCart(true);
    try {
      await productService.addToCart(product.id, quantity);
      toast.success(`Đã thêm "${product.name}" vào giỏ hàng!`);
    } catch (err) {
      toast.error(isAxiosError(err) && err.response?.data?.message
        ? err.response.data.message
        : 'Không thể thêm sản phẩm vào giỏ hàng. Vui lòng thử lại sau.');
    } finally {
      addingRef.current = false;
      setAddingToCart(false);
    }
  };

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
                <ProductImage src={product.imageUrl} alt={product.name} className="w-full h-full object-contain mix-blend-multiply" />
              </div>
            </div>

            {/* Right - Details */}
            <div className="w-full lg:w-1/2 p-6 lg:p-10 flex flex-col">
              <h1 className="text-3xl font-bold text-gray-900 mb-4">{product.name}</h1>

              <div className="mb-6 pb-6 border-b border-gray-100 flex items-end space-x-4">
                <span className="text-4xl font-bold text-primary-600">{formatPrice(product.price)}</span>
              </div>

              <div className="mb-8">
                <div className="mt-4 pt-4 border-t border-gray-50">
                  <h3 className="font-semibold text-gray-900 mb-2">Mô tả:</h3>
                  <p className="text-sm text-gray-600 leading-relaxed">{product.description}</p>
                </div>
              </div>

              <div className="flex items-center mb-8">
                <span className="font-semibold text-gray-900 w-24">Số lượng:</span>
                <div className="flex items-center border border-gray-200 rounded-xl bg-gray-50">
                  <button
                    disabled={addingToCart || quantity <= 1}
                    aria-label="Giảm số lượng"
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
                    disabled={addingToCart || quantity >= (product.stockQuantity ?? 0)}
                    aria-label="Tăng số lượng"
                    onClick={() => setQuantity(Math.min(product.stockQuantity ?? 0, quantity + 1))}
                    className="p-3 text-gray-500 hover:text-primary-600 transition-colors"
                  >
                    <FiPlus />
                  </button>
                </div>
                <span className="ml-4 text-sm text-gray-500">
                  {(product.stockQuantity || 0) > 0 ? `Còn hàng (${product.stockQuantity})` : 'Hết hàng'}
                </span>
              </div>

              {/* Action Buttons */}
              <div className="flex space-x-4 mt-auto">
                <Button size="lg" className="flex-1 py-4 text-lg" isLoading={addingToCart} onClick={handleAddToCart} disabled={(product.stockQuantity ?? 0) <= 0}>
                  <FiShoppingCart className="mr-2" /> Thêm vào giỏ hàng
                </Button>
                <button className="p-4 rounded-2xl border-2 border-gray-200 text-gray-400 hover:text-coral-500 hover:border-coral-500 transition-colors bg-white shadow-xs">
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
