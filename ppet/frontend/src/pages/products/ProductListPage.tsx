import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Card } from '../../components/common/Card';
import { productService, Product, ProductCategory } from '../../services/productService';
import { ProductImage } from '../../components/common/ProductImage';
import { FiSearch, FiShoppingCart, FiLoader } from 'react-icons/fi';
import { FaBone, FaBath, FaTag } from 'react-icons/fa';
import { MdToys } from 'react-icons/md';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useAuth } from '../../contexts/AuthContext';
import toast from 'react-hot-toast';

export const ProductListPage: React.FC = () => {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [searchParams, setSearchParams] = useSearchParams();
  const categoryParam = searchParams.get('categoryId');
  const categoryId = categoryParam === null ? undefined : Number(categoryParam);
  const invalidCategory = categoryId !== undefined && (!Number.isSafeInteger(categoryId) || categoryId <= 0);
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [categoriesError, setCategoriesError] = useState<string | null>(null);
  const addingRef = useRef(false);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [addingProductId, setAddingProductId] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    productService.getAllCategories()
      .then(data => { if (!cancelled) setCategories(data); })
      .catch(() => { if (!cancelled) setCategoriesError('Không thể tải danh mục sản phẩm.'); });
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    const fetchProducts = async () => {
      try {
        if (invalidCategory) {
          setProducts([]);
          setError('Danh mục không hợp lệ. Vui lòng chọn lại danh mục.');
          return;
        }
        const data = await productService.getAllProducts(0, 20, categoryId);
        if (!cancelled) setProducts(data.content);
      } catch (err) {
        if (!cancelled) setError('Không thể tải danh sách sản phẩm. Vui lòng thử lại sau.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    fetchProducts();
    return () => { cancelled = true; };
  }, [categoryId, invalidCategory]);

  const selectCategory = (id?: number) => {
    setSearchParams(previous => {
      const next = new URLSearchParams(previous);
      if (id === undefined || id === categoryId) next.delete('categoryId');
      else next.set('categoryId', String(id));
      return next;
    });
  };

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

    if (addingRef.current) {
      return;
    }

    try {
      addingRef.current = true;
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
      addingRef.current = false;
      setAddingProductId(null);
    }
  };

  const filteredProducts = products.filter(p => p.name.toLowerCase().includes(searchTerm.toLowerCase()));
  const categoryStyles = [
    { icon: <FaBone size={24} />, color: 'bg-orange-100 text-orange-600' },
    { icon: <MdToys size={24} />, color: 'bg-purple-100 text-purple-600' },
    { icon: <FaTag size={24} />, color: 'bg-green-100 text-green-600' },
    { icon: <FaBath size={24} />, color: 'bg-blue-100 text-blue-600' },
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
              key={cat.id}
              aria-pressed={categoryId === cat.id}
              onClick={() => selectCategory(cat.id)}
              className={`bg-white rounded-2xl p-6 flex flex-col items-center justify-center border transition-all cursor-pointer group ${
                categoryId === cat.id
                  ? 'border-primary-500 shadow-md ring-2 ring-primary-100'
                  : 'border-gray-100 hover:shadow-card hover:border-primary-100'
              }`}
            >
              <div
                className={`${categoryStyles[idx % categoryStyles.length].color} w-16 h-16 rounded-full flex items-center justify-center mb-4 transition-transform group-hover:scale-110`}
              >
                {categoryStyles[idx % categoryStyles.length].icon}
              </div>
              <span className="font-semibold text-gray-800">{cat.name}</span>
            </button>
          ))}
        </div>

        {categoriesError && <p role="alert" className="mb-4 text-red-500">{categoriesError}</p>}
        {categoryParam !== null && <button onClick={() => selectCategory()} className="mb-6 text-primary-600">Xóa bộ lọc danh mục</button>}
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
                    <ProductImage
                      src={product.imageUrl}
                      alt={product.name}
                      className="w-full h-full object-contain mix-blend-multiply group-hover:scale-105 transition-transform duration-500"
                    />
                    <button
                      type="button"
                      aria-label="Thêm vào giỏ hàng"
                      title={product.stockQuantity !== undefined && product.stockQuantity <= 0 ? 'Hết hàng' : 'Thêm vào giỏ hàng'}
                      disabled={addingProductId !== null || (product.stockQuantity !== undefined && product.stockQuantity <= 0)}
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