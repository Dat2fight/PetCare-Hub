import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Card } from '../../components/common/Card';
import { productService, Product } from '../../services/productService';
import { mockImages } from '../../data/mockImages';
import { FiSearch, FiShoppingCart, FiStar, FiLoader } from 'react-icons/fi';
import { FaBone, FaBath, FaTag } from 'react-icons/fa';
import { MdToys } from 'react-icons/md';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useAuth } from '../../contexts/AuthContext';
import toast from 'react-hot-toast';

export const ProductListPage: React.FC = () => {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [addingProductId, setAddingProductId] = useState<number | null>(null);

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        setLoading(true);
        // Using page 0, size 20 to grab enough products for now
        const data = await productService.getAllProducts(0, 20);
        
        // Map backend DTO to frontend display representation
        // Assign random/mock image and rating since they aren't in backend yet
        const displayProducts = data.content.map(p => ({
          ...p,
          imageUrl: p.imageUrl || mockImages.foodRoyalCanin, 
          rating: 4.5 + Math.random() * 0.5 
        }));
        
        setProducts(displayProducts);
      } catch (err) {
        console.error('Error fetching products:', err);
        setError('Không thể tải danh sách sản phẩm. Vui lòng thử lại sau.');
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, []);

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const handleAddToCart = async (e: React.MouseEvent, product: Product) => {
    e.preventDefault();
    e.stopPropagation();

    if (product.stockQuantity !== undefined && product.stockQuantity <= 0) {
      toast.error(`Sản phẩm "${product.name}" hiện đã hết hàng!`);
      return;
    }

    if (!isAuthenticated) {
      toast.error('Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.');
      navigate('/login');
      return;
    }

    if (addingProductId === product.id) {
      return;
    }

    try {
      setAddingProductId(product.id);
      await productService.addToCart(product.id, 1);
      toast.success(`Đã thêm "${product.name}" vào giỏ hàng!`);
    } catch (err: any) {
      console.error('Error adding product to cart:', err);
      const errorMessage =
        err?.response?.data?.message ||
        'Không thể thêm sản phẩm vào giỏ hàng. Vui lòng thử lại sau.';
      toast.error(errorMessage);
    } finally {
      setAddingProductId(null);
    }
  };

  const filteredProducts = products.filter(p => {
    const matchesSearch = p.name.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory = selectedCategory 
      ? (p.categoryName === selectedCategory || p.name.toLowerCase().includes(selectedCategory.toLowerCase()))
      : true;
    return matchesSearch && matchesCategory;
  });

  const categories = [
    { name: 'Thức ăn', icon: <FaBone size={24} />, color: 'bg-orange-100 text-orange-600' },
    { name: 'Đồ chơi', icon: <MdToys size={24} />, color: 'bg-purple-100 text-purple-600' },
    { name: 'Vệ sinh', icon: <FaBath size={24} />, color: 'bg-blue-100 text-blue-600' },
    { name: 'Phụ kiện', icon: <FaTag size={24} />, color: 'bg-green-100 text-green-600' },
  ];

  return (
    <div className="bg-background min-h-screen py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

        {/* Search */}
        <div className="flex flex-col md:flex-row md:items-center justify-between mb-10 gap-4">
          <h1 className="text-3xl font-bold text-gray-900">Sản phẩm nổi bật</h1>
          <div className="relative w-full md:w-96">
            <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
              <FiSearch size={20} />
            </div>
            <input
              type="text"
              className="block w-full rounded-full border border-gray-200 py-3 pl-12 pr-4 text-gray-900 focus:ring-2 focus:ring-primary-500 bg-white shadow-sm"
              placeholder="Tìm kiếm hạt Royal Canin, vòng cổ..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        {/* Categories */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-12">
          {categories.map((cat, idx) => (
            <button
              key={idx}
              onClick={() => setSelectedCategory(selectedCategory === cat.name ? null : cat.name)}
              className={`bg-white rounded-2xl p-6 flex flex-col items-center justify-center border transition-all cursor-pointer group ${
                selectedCategory === cat.name
                  ? 'border-primary-500 shadow-md ring-2 ring-primary-100'
                  : 'border-gray-100 hover:shadow-card hover:border-primary-100'
              }`}
            >
              <div
                className={`${cat.color} w-16 h-16 rounded-full flex items-center justify-center mb-4 transition-transform group-hover:scale-110`}
              >
                {cat.icon}
              </div>
              <span className="font-semibold text-gray-800">{cat.name}</span>
            </button>
          ))}
        </div>

        {loading ? (
          <div className="flex justify-center py-20">
            <LoadingSpinner size="lg" />
          </div>
        ) : error ? (
          <div className="text-center py-20 text-red-500">{error}</div>
        ) : filteredProducts.length === 0 ? (
          <div className="text-center py-20 text-gray-500">
            <p className="text-xl font-medium mb-2">Không tìm thấy sản phẩm nào</p>
            <p>Vui lòng thử lại với từ khóa khác</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {filteredProducts.map(product => (
              <Link to={`/products/${product.id}`} key={product.id}>
                <Card noPadding className="group cursor-pointer hover:shadow-lg transition-all duration-300 h-full flex flex-col border-transparent hover:border-primary-100">
                  <div className="relative aspect-square overflow-hidden bg-white p-6 flex items-center justify-center border-b border-gray-50">
                    <img
                      src={product.imageUrl}
                      alt={product.name}
                      className="w-full h-full object-contain mix-blend-multiply group-hover:scale-105 transition-transform duration-500"
                    />
                    <button
                      type="button"
                      aria-label="Thêm vào giỏ hàng"
                      title={product.stockQuantity !== undefined && product.stockQuantity <= 0 ? 'Hết hàng' : 'Thêm vào giỏ hàng'}
                      disabled={addingProductId === product.id || (product.stockQuantity !== undefined && product.stockQuantity <= 0)}
                      className={`absolute bottom-3 right-3 p-3 text-white rounded-xl shadow-md transition-colors z-10 translate-y-2 opacity-0 group-hover:translate-y-0 group-hover:opacity-100 ${
                        product.stockQuantity !== undefined && product.stockQuantity <= 0
                          ? 'bg-gray-400 cursor-not-allowed'
                          : addingProductId === product.id
                          ? 'bg-primary-400 cursor-wait'
                          : 'bg-primary-600 hover:bg-primary-700'
                      }`}
                      onClick={(e) => handleAddToCart(e, product)}
                    >
                      {addingProductId === product.id ? (
                        <FiLoader size={18} className="animate-spin" />
                      ) : (
                        <FiShoppingCart size={18} />
                      )}
                    </button>
                  </div>
                  <div className="p-5 flex flex-col flex-grow bg-white">
                    <div className="flex items-center text-yellow-400 mb-2 text-xs">
                      <FiStar fill="currentColor" />
                      <span className="text-gray-600 ml-1 font-medium">{product.rating?.toFixed(1)}</span>
                    </div>
                    <h3 className="font-medium text-gray-900 mb-2 line-clamp-2 group-hover:text-primary-600 transition-colors">
                      {product.name}
                    </h3>
                    <div className="mt-auto pt-2 flex flex-col">
                      <div className="font-bold text-lg text-primary-600">
                        {formatPrice(product.price)}
                      </div>
                    </div>
                  </div>
                </Card>
              </Link>
            ))}
          </div>
        )}

      </div>
    </div>
  );
};