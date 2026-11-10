import { HeartButton, useWishlistStatus } from '../components/customer/Wishlist';
import { useResource } from '../hooks/useResource';
import { PublicPetDTO } from '../services/petManagementService';
import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '../components/common/Button';
import type { Product, ProductCategory } from '../services/productService';
import type { Service } from '../services/serviceService';
import { ProductImage } from '../components/common/ProductImage';
import { FiHeart } from 'react-icons/fi';
import { MdPets } from 'react-icons/md';

export const HomePage: React.FC = () => {
  const featured=useResource<{content:PublicPetDTO[]}>('/pets?size=5&availabilityStatus=AVAILABLE&sort=id,desc');
  const hearts=useWishlistStatus('PET',featured.data?.content.map(p=>p.id) ?? []);
  const services = useResource<Service[]>('/services');
  const products = useResource<{items: Product[]; bestSelling: boolean}>('/home/products');
  const reviews = useResource<{id: number; displayName: string; rating: number; comment: string}[]>('/home/reviews');
  const categories = useResource<ProductCategory[]>('/categories');
  return (
    <div className="bg-background min-h-screen">
      
      {/* 1. HERO SECTION */}
      <section className="relative bg-primary-50 pt-16 pb-20 lg:pt-24 lg:pb-32 overflow-hidden border-b border-primary-100/50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10 flex flex-col lg:flex-row items-center">
          
          <div className="lg:w-5/12 z-20 mb-12 lg:mb-0">
            <span className="inline-block py-1.5 px-4 rounded-full bg-primary-100 text-primary-700 font-bold text-sm mb-6 shadow-xs">
              Chào mừng đến với PetCare Hub!
            </span>
            <h1 className="text-5xl lg:text-[64px] font-extrabold text-gray-900 leading-[1.1] mb-6 tracking-tight">
              Tìm người bạn <br/> bốn chân <br/>
              <span className="text-primary-600 flex items-center gap-3">
                phù hợp với bạn
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className="text-primary-400 rotate-12"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>
              </span>
            </h1>
            <p className="text-lg text-gray-600 mb-10 max-w-lg leading-relaxed">
              Không chỉ là nơi mua bán thú cưng, chúng tôi còn mang đến đầy đủ sản phẩm, dịch vụ chăm sóc và giải pháp sức khỏe để thú cưng luôn khỏe mạnh và hạnh phúc.
            </p>
            <div className="flex flex-wrap gap-4">
              <Link to="/pets">
                <Button size="lg" className="px-8 rounded-full shadow-lg shadow-primary-500/30">Tìm thú cưng ngay</Button>
              </Link>
              <Link to="/products">
                <Button variant="outline" size="lg" className="px-8 rounded-full bg-white text-primary-700 border-primary-100 hover:bg-primary-50">Khám phá sản phẩm →</Button>
              </Link>
            </div>
          </div>

          <div className="lg:w-7/12 relative flex justify-center lg:justify-end z-10">
            <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-primary-100/50 rounded-full blur-3xl -z-10"></div>
            
            <div className="relative w-full max-w-[600px] h-[500px]">
              <img src="https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&q=80&w=800" width={400} height={480} alt="Chó ngoài trời" className="absolute right-10 bottom-0 w-[400px] h-[480px] object-cover rounded-[100px] rounded-br-[200px] shadow-2xl z-10 border-8 border-white" />
              <img src="https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&q=80&w=600" width={240} height={240} alt="Mèo" className="absolute left-10 bottom-10 w-[240px] h-[240px] object-cover rounded-full shadow-xl z-20 border-8 border-white" />
              
              <div className="absolute top-10 right-0 bg-secondary-100 text-secondary-600 font-bold p-6 rounded-[2rem] rounded-tl-none shadow-lg z-30 max-w-[150px] transform rotate-3">
                <p className="text-center text-sm">Nơi khởi đầu cho những hành trình hạnh phúc!</p>
              </div>

              <div className="absolute top-4 left-1/4 -rotate-12 z-30 font-serif text-2xl text-primary-600 opacity-80 italic">
                Happy <br/> Together <FiHeart className="inline text-coral-500" />
              </div>
              
              <MdPets className="absolute bottom-1/4 -left-12 text-primary-200 text-5xl -rotate-45" />
              <MdPets className="absolute top-1/3 right-1/4 text-primary-300 text-4xl rotate-12" />
            </div>
          </div>
        </div>
      </section>

      {/* 3. FEATURED PETS */}
      {featured.data?.content.length !== 0 && <section className="mb-24 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-end mb-8">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-primary-100 text-primary-600 rounded-2xl flex items-center justify-center text-2xl">
              <MdPets />
            </div>
            <div>
              <h2 className="text-3xl font-bold text-gray-900 mb-1">Thú cưng nổi bật</h2>
              <p className="text-gray-500 text-sm">Những bé thú cưng đáng yêu đang chờ bạn</p>
            </div>
          </div>
          <Link to="/pets" className="text-primary-600 font-medium hover:text-primary-700 hidden sm:block text-sm">Xem tất cả →</Link>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-6">
          {featured.error ? <p role="alert">{featured.error} <button onClick={featured.retry}>Thử lại</button></p> : !featured.data ? <div role="status" aria-label="Đang tải thú cưng" className="aspect-[4/3] animate-pulse rounded-xl bg-gray-100" /> : featured.data.content.map(pet=>(
              <article key={pet.id} className="relative rounded-2xl bg-white shadow-sm p-3">
                <Link to={`/pets/${pet.id}`}>
                  <ProductImage src={pet.imageUrl} alt={pet.name} width={320} height={240} className="w-full aspect-[4/3] object-cover rounded-xl"/>
                  <h3 className="font-bold mt-3">{pet.name}</h3><p>{pet.breedName || pet.speciesName}</p>
                  {pet.price != null && <p className="text-primary-700">{pet.price.toLocaleString('vi-VN')} ₫</p>}
                </Link><div className="absolute top-4 right-4"><HeartButton type="PET" id={pet.id} status={hearts}/></div>
              </article>))}
        </div>
      </section>}

      {/* 4. PET MATCHING CTA */}
      <section className="mb-24 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-linear-to-r from-primary-50 to-primary-100/50 rounded-[32px] p-8 md:p-12 flex flex-col md:flex-row items-center justify-between relative overflow-hidden border border-primary-100">
          <div className="md:w-1/2 relative z-10">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 bg-white rounded-xl shadow-xs flex items-center justify-center text-primary-500">
                <MdPets size={20} />
              </div>
              <h2 className="text-3xl font-extrabold text-gray-900">Tìm thú cưng phù hợp với bạn</h2>
            </div>
            <p className="text-gray-600 mb-8 max-w-md">Trả lời vài câu hỏi đơn giản để nhận gợi ý những bé thú cưng phù hợp nhất</p>
            <Link to="/pet-matching">
              <Button size="lg" className="rounded-full shadow-md px-6">Làm bài quiz ngay →</Button>
            </Link>
          </div>
          
          <div className="md:w-1/2 relative mt-8 md:mt-0 flex justify-end">
            <div className="relative w-[300px] h-[200px]">
              <img src="https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&q=80&w=400" className="absolute right-0 bottom-0 w-48 h-48 object-cover rounded-full border-4 border-white shadow-xl z-10" alt="Dog" />
              <img src="https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&q=80&w=400" className="absolute right-32 bottom-0 w-32 h-32 object-cover rounded-full border-4 border-white shadow-lg z-20" alt="Cat" />
              
              <div className="absolute -top-6 right-10 bg-white px-4 py-2 rounded-2xl rounded-br-none shadow-md z-30 border border-primary-50 text-center">
                <p className="text-primary-600 font-bold text-sm">Quiz 2 phút</p>
                <p className="text-secondary-500 font-bold text-sm">Có ngay kết quả!</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <HomeSection title="Danh mục sản phẩm" resource={categories} empty={categories.data?.length === 0}>
        {categories.data?.map(category => <Link className="rounded-xl bg-white p-5" to={'/products?categoryId='+category.id} key={category.id}>{category.name}</Link>)}
      </HomeSection>
      <HomeSection title="Dịch vụ chăm sóc thú cưng" resource={services} empty={services.data?.length === 0}>
        {services.data?.slice(0,6).map(service => <Link to={'/services/'+service.id} key={service.id} className="rounded-xl bg-white p-5">
          <h3 className="font-bold">{service.name}</h3><p>{service.description}</p>
          <p>{service.durationMinutes} phút · {service.price.toLocaleString('vi-VN')} ₫</p>
        </Link>)}
      </HomeSection>
      <HomeSection title={products.data?.bestSelling ? 'Sản phẩm bán chạy' : 'Sản phẩm mới'} resource={products} empty={products.data?.items.length === 0}>
        {products.data?.items.map(product => <Link key={product.id} to={'/products/'+product.id} className="rounded-xl bg-white p-5">
          <ProductImage src={product.imageUrl} alt={product.name} width={320} height={240} className="aspect-[4/3] w-full object-contain" />
          <h3 className="font-bold">{product.name}</h3><p>{product.price.toLocaleString('vi-VN')} ₫</p>
        </Link>)}
      </HomeSection>
      <HomeSection title="Đánh giá từ khách hàng" resource={reviews} empty={reviews.data?.length === 0}>
        {reviews.data?.map(review => <figure key={review.id} className="rounded-xl bg-white p-5">
          <blockquote>{review.comment}</blockquote><figcaption>{review.displayName} · {review.rating}/5</figcaption>
        </figure>)}
      </HomeSection>
    </div>
  );
};

function HomeSection({ title, resource, empty, children }: { title: string; resource: { data?: unknown; error?: string; retry: () => void }; empty: boolean; children: React.ReactNode }) {
  if (empty) return null;
  return <section className="mb-16 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <h2 className="mb-6 text-2xl font-bold">{title}</h2>
    {resource.error ? <p role="alert">{resource.error} <button className="underline" onClick={resource.retry}>Thử lại</button></p>
      : !resource.data ? <div role="status" aria-label={'Đang tải '+title} className="h-28 animate-pulse rounded-xl bg-gray-100" />
      : <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">{children}</div>}
  </section>;
}

export default HomePage;
