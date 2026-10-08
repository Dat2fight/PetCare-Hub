import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '../components/common/Button';
import { Card } from '../components/common/Card';
import { mockImages } from '../data/mockImages';
import { FiHeart, FiStar, FiChevronRight, FiChevronLeft, FiCheckCircle, FiShield, FiClock, FiTruck, FiUser } from 'react-icons/fi';
import { MdPets } from 'react-icons/md';

export const HomePage: React.FC = () => {
  return (
    <div className="bg-background min-h-screen">
      
      {/* 1. HERO SECTION */}
      <section className="relative bg-primary-50 pt-16 pb-20 lg:pt-24 lg:pb-32 overflow-hidden border-b border-primary-100/50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10 flex flex-col lg:flex-row items-center">
          
          <div className="lg:w-5/12 z-20 mb-12 lg:mb-0">
            <span className="inline-block py-1.5 px-4 rounded-full bg-primary-100 text-primary-700 font-bold text-sm mb-6 shadow-sm">
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
              <img src="https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&q=80&w=800" alt="Happy Golden Retriever" className="absolute right-10 bottom-0 w-[400px] h-[480px] object-cover rounded-[100px] rounded-br-[200px] shadow-2xl z-10 border-8 border-white" />
              <img src="https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&q=80&w=600" alt="Cute Cat" className="absolute left-10 bottom-10 w-[240px] h-[240px] object-cover rounded-full shadow-xl z-20 border-8 border-white" />
              
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

      {/* 2. TRUST / BENEFIT STRIP */}
      <section className="relative -mt-10 z-30 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mb-20">
        <div className="bg-white rounded-3xl shadow-sm p-4 flex flex-col md:flex-row justify-between items-center divide-y md:divide-y-0 md:divide-x divide-gray-100 border border-gray-100">
          {[
            { icon: <FiTruck />, title: "Giao hàng toàn quốc", desc: "Nhanh chóng & an toàn" },
            { icon: <FiShield />, title: "Thú cưng khỏe mạnh", desc: "Được kiểm tra sức khỏe" },
            { icon: <FiCheckCircle />, title: "Dịch vụ chuyên nghiệp", desc: "Tận tâm & uy tín" },
            { icon: <FiClock />, title: "Hỗ trợ 24/7", desc: "Luôn bên bạn" },
          ].map((item, idx) => (
            <div key={idx} className="flex items-center gap-4 px-6 py-4 w-full md:w-1/4 justify-center md:justify-start">
              <div className="w-12 h-12 rounded-full bg-primary-50 text-primary-600 flex items-center justify-center flex-shrink-0 text-xl">
                {item.icon}
              </div>
              <div>
                <h4 className="font-bold text-gray-900 text-sm">{item.title}</h4>
                <p className="text-xs text-gray-500">{item.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* 3. FEATURED PETS */}
      <section className="mb-24 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
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
          {[
            { id: 1, name: "Chó Golden Retriever", breed: "Golden Retriever", age: "2 tháng", gender: "Đực", weight: "15kg", price: "12.000.000đ", image: mockImages.dogGolden, status: "Đã tiêm phòng", bg: "bg-primary-500" },
            { id: 2, name: "Mèo Anh Lông Ngắn", breed: "Mèo ALN", age: "3 tháng", gender: "Cái", weight: "4kg", price: "8.000.000đ", image: mockImages.catScottish, status: "Sức khỏe tốt", bg: "bg-blue-500" },
            { id: 3, name: "Chó Corgi", breed: "Corgi", age: "2 tháng", gender: "Đực", weight: "10kg", price: "10.000.000đ", image: mockImages.dogCorgi, status: "Đang chăm sóc", bg: "bg-secondary-500" },
            { id: 4, name: "Poodle", breed: "Poodle", age: "4 tháng", gender: "Cái", weight: "5kg", price: "9.500.000đ", image: mockImages.dogPoodle, status: "Đã tiêm phòng", bg: "bg-primary-500" },
            { id: 5, name: "Mèo Munchkin", breed: "Munchkin", age: "3 tháng", gender: "Cái", weight: "3kg", price: "7.500.000đ", image: mockImages.catPersian, status: "Sức khỏe tốt", bg: "bg-blue-500" },
          ].map(pet => (
            <Card key={pet.id} noPadding className="group cursor-pointer hover:shadow-lg transition-all duration-300 h-full flex flex-col border-gray-100 hover:border-primary-200">
              <div className="relative aspect-[4/3] overflow-hidden bg-gray-50 rounded-t-2xl p-2">
                <img src={pet.image} alt={pet.name} className="w-full h-full object-cover rounded-xl group-hover:scale-105 transition-transform duration-500" />
                <button className="absolute top-4 right-4 w-8 h-8 bg-white/90 backdrop-blur rounded-full flex items-center justify-center text-gray-400 hover:text-coral-500 transition-colors shadow-sm">
                  <FiHeart size={16} />
                </button>
                <div className="absolute bottom-4 left-4">
                  <span className={`${pet.bg} text-white text-xs font-bold px-2.5 py-1 rounded-full shadow-sm`}>{pet.status}</span>
                </div>
              </div>
              <div className="p-4 flex flex-col flex-grow">
                <h3 className="font-bold text-[15px] text-gray-900 mb-2 line-clamp-1">{pet.name}</h3>
                <div className="flex items-center gap-2 text-xs text-gray-500 mb-4 divide-x divide-gray-200">
                  <span className="flex items-center gap-1"><FiUser size={12}/> {pet.age}</span>
                  <span className="pl-2">{pet.gender}</span>
                  <span className="pl-2">{pet.weight}</span>
                </div>
                <div className="mt-auto font-extrabold text-primary-600 text-lg">{pet.price}</div>
              </div>
            </Card>
          ))}
        </div>
      </section>

      {/* 4. PET MATCHING CTA */}
      <section className="mb-24 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-gradient-to-r from-primary-50 to-primary-100/50 rounded-[32px] p-8 md:p-12 flex flex-col md:flex-row items-center justify-between relative overflow-hidden border border-primary-100">
          <div className="md:w-1/2 relative z-10">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 bg-white rounded-xl shadow-sm flex items-center justify-center text-primary-500">
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

      {/* 5. POPULAR CATEGORIES */}
      <section className="mb-24 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-end mb-8">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-secondary-100 text-secondary-500 rounded-2xl flex items-center justify-center text-2xl">
              <FiStar />
            </div>
            <div>
              <h2 className="text-3xl font-bold text-gray-900 mb-1">Danh mục nổi bật</h2>
              <p className="text-gray-500 text-sm">Khám phá đa dạng lựa chọn cho thú cưng của bạn</p>
            </div>
          </div>
          <Link to="/products" className="text-primary-600 font-medium hover:text-primary-700 hidden sm:block text-sm">Xem tất cả →</Link>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-4">
          {[
            { name: "Chó", count: "120+ giống", icon: "🐶", bg: "bg-orange-50", hover: "hover:bg-orange-100", text: "text-orange-600" },
            { name: "Mèo", count: "100+ giống", icon: "🐱", bg: "bg-blue-50", hover: "hover:bg-blue-100", text: "text-blue-600" },
            { name: "Thức ăn", count: "200+ sản phẩm", icon: "🥩", bg: "bg-red-50", hover: "hover:bg-red-100", text: "text-red-600" },
            { name: "Đồ chơi", count: "150+ sản phẩm", icon: "🎾", bg: "bg-primary-50", hover: "hover:bg-primary-100", text: "text-primary-600" },
            { name: "Phụ kiện", count: "180+ sản phẩm", icon: "🎽", bg: "bg-purple-50", hover: "hover:bg-purple-100", text: "text-purple-600" },
            { name: "Chăm sóc", count: "120+ sản phẩm", icon: "🧴", bg: "bg-cyan-50", hover: "hover:bg-cyan-100", text: "text-cyan-600" },
            { name: "Sức khỏe", count: "100+ sản phẩm", icon: "💊", bg: "bg-emerald-50", hover: "hover:bg-emerald-100", text: "text-emerald-600" },
            { name: "Khác", count: "50+ sản phẩm", icon: "✨", bg: "bg-secondary-50", hover: "hover:bg-secondary-100", text: "text-secondary-600" },
          ].map((cat, idx) => (
            <Link to="/products" key={idx} className={`${cat.bg} ${cat.hover} rounded-[24px] p-6 flex flex-col items-center justify-center text-center transition-colors border border-white shadow-sm cursor-pointer`}>
              <div className="text-4xl mb-3 drop-shadow-sm">{cat.icon}</div>
              <h4 className="font-bold text-gray-900 text-sm mb-1">{cat.name}</h4>
              <p className="text-[10px] font-medium text-gray-500">{cat.count}</p>
            </Link>
          ))}
        </div>
      </section>

      {/* 6. PET CARE SERVICES */}
      <section className="mb-24 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-end mb-8">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-blue-100 text-blue-500 rounded-2xl flex items-center justify-center text-2xl">
              <FiCheckCircle />
            </div>
            <div>
              <h2 className="text-3xl font-bold text-gray-900 mb-1">Dịch vụ chăm sóc thú cưng</h2>
              <p className="text-gray-500 text-sm">Dịch vụ chuyên nghiệp - Thú cưng luôn khỏe mạnh</p>
            </div>
          </div>
          <Link to="/services" className="text-primary-600 font-medium hover:text-primary-700 hidden sm:block text-sm">Xem tất cả →</Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-6">
          {[
            { title: "Tắm & Grooming", desc: "Làm sạch, cắt tỉa, chăm sóc lông chuyên nghiệp", price: "200.000đ", image: mockImages.grooming, icon: "✂️" },
            { title: "Khám thú y", desc: "Kiểm tra sức khỏe, tư vấn điều trị", price: "150.000đ", image: mockImages.vet, icon: "🏥" },
            { title: "Gói chăm sóc theo tháng", desc: "Dinh dưỡng + khám định kỳ + ưu đãi", price: "1.000.000đ", image: mockImages.dogHero, icon: "🎁" },
            { title: "Pet Sitting", desc: "Chăm sóc thú cưng khi bạn vắng nhà", price: "300.000đ", image: mockImages.dogGolden, icon: "🏠" },
          ].map((srv, idx) => (
            <Card key={idx} noPadding className="group border-gray-100 hover:border-primary-200 transition-colors h-full flex flex-col rounded-3xl overflow-hidden">
              <div className="h-36 overflow-hidden relative">
                <img src={srv.image} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" alt={srv.title} />
              </div>
              <div className="p-5 flex flex-col flex-grow relative">
                <div className="absolute -top-6 left-4 w-12 h-12 bg-white rounded-xl shadow-md flex items-center justify-center text-2xl border border-gray-50">
                  {srv.icon}
                </div>
                <h3 className="font-bold text-gray-900 mt-6 mb-2">{srv.title}</h3>
                <p className="text-xs text-gray-500 mb-4 line-clamp-2 leading-relaxed">{srv.desc}</p>
                
                <div className="mt-auto flex items-center justify-between border-t border-gray-50 pt-4">
                  <span className="font-extrabold text-primary-600">{srv.price}</span>
                  <Link to="/services">
                    <button className="text-xs font-semibold text-primary-600 border border-primary-200 px-3 py-1.5 rounded-full hover:bg-primary-50 transition-colors">
                      Đặt lịch ngay →
                    </button>
                  </Link>
                </div>
              </div>
            </Card>
          ))}
          
          {/* Promo Service Card */}
          <div className="bg-primary-50 rounded-3xl p-6 flex flex-col items-center justify-center text-center relative overflow-hidden border border-primary-100 group cursor-pointer hover:shadow-lg transition-shadow">
            <div className="absolute top-0 right-0 w-32 h-32 bg-primary-200/50 rounded-full blur-2xl -mr-10 -mt-10"></div>
            <div className="absolute bottom-0 left-0 w-32 h-32 bg-secondary-200/30 rounded-full blur-2xl -ml-10 -mb-10"></div>
            
            <div className="relative z-10">
              <h3 className="text-xl font-extrabold text-primary-800 mb-3">Ưu đãi đặc biệt <br/> cho thành viên</h3>
              <p className="text-primary-700 text-sm font-medium mb-6">Tích điểm - Nhận quà - Giảm giá</p>
              <Link to="/membership">
                <Button className="rounded-full px-6 shadow-md shadow-primary-500/20">Khám phá ngay →</Button>
              </Link>
            </div>
            
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="absolute bottom-4 left-4 text-primary-300 opacity-50"><path d="M12 2v20"></path><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path></svg>
          </div>
        </div>
      </section>

      {/* 7. BEST-SELLING PRODUCTS */}
      <section className="mb-24 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-end mb-8">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-coral-50 text-coral-500 rounded-2xl flex items-center justify-center text-2xl">
              <FiHeart />
            </div>
            <div>
              <h2 className="text-3xl font-bold text-gray-900 mb-1">Sản phẩm bán chạy</h2>
              <p className="text-gray-500 text-sm">Những sản phẩm được yêu thích nhất hiện nay</p>
            </div>
          </div>
          <Link to="/products" className="text-primary-600 font-medium hover:text-primary-700 hidden sm:block text-sm">Xem tất cả →</Link>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-6">
          {[
            { name: "Thức ăn Royal Canin", desc: "1.2kg | Cho chó trưởng thành", price: "220.000đ", oldPrice: "260.000đ", image: mockImages.foodRoyalCanin, rating: 4.8, sale: "-15%" },
            { name: "Thức ăn Whiskas", desc: "1.2kg | Cho mèo trưởng thành", price: "180.000đ", oldPrice: "220.000đ", image: "https://images.unsplash.com/photo-1589924691995-400dc9ecc119?auto=format&fit=crop&q=80&w=400", rating: 4.7, sale: "-15%" },
            { name: "Đồ chơi bóng cao su", desc: "An toàn, bền bỉ", price: "45.000đ", image: mockImages.toyBall, rating: 4.9 },
            { name: "Dây dắt cho chó", desc: "Size M | Chất liệu cao cấp", price: "250.000đ", image: mockImages.collar, rating: 4.8 },
            { name: "Cát vệ sinh cho mèo", desc: "Khử mùi tốt, an toàn", price: "120.000đ", image: "https://images.unsplash.com/photo-1598134493206-89b533318991?auto=format&fit=crop&q=80&w=400", rating: 4.6 },
            { name: "Bàn chải lông", desc: "Phù hợp mọi giống chó mèo", price: "85.000đ", image: mockImages.carrier, rating: 4.7 },
          ].map((prod, idx) => (
            <Card key={idx} noPadding className="group h-full flex flex-col border-gray-100 hover:border-primary-200 transition-all rounded-3xl">
              <div className="relative aspect-square p-4 bg-white rounded-t-3xl overflow-hidden flex items-center justify-center">
                {prod.sale && (
                  <div className="absolute top-3 left-3 bg-coral-500 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-sm z-10">
                    {prod.sale}
                  </div>
                )}
                <button className="absolute top-3 right-3 text-gray-300 hover:text-coral-500 transition-colors z-10">
                  <FiHeart size={18} />
                </button>
                <img src={prod.image} alt={prod.name} className="w-full h-full object-contain mix-blend-multiply group-hover:scale-105 transition-transform duration-500" />
              </div>
              <div className="p-4 bg-gray-50/50 flex flex-col flex-grow rounded-b-3xl border-t border-gray-100">
                <h3 className="font-bold text-[13px] text-gray-900 mb-1 line-clamp-2">{prod.name}</h3>
                <p className="text-[11px] text-gray-500 mb-2">{prod.desc}</p>
                <div className="mt-auto">
                  <div className="flex items-end gap-2 mb-2">
                    <span className="font-extrabold text-primary-600 text-sm">{prod.price}</span>
                    {prod.oldPrice && <span className="text-[10px] text-gray-400 line-through mb-0.5">{prod.oldPrice}</span>}
                  </div>
                  <div className="flex justify-between items-center">
                    <div className="flex items-center gap-1 text-[11px] font-medium text-gray-600">
                      <FiStar className="text-secondary-500 fill-secondary-500" /> {prod.rating} (120)
                    </div>
                    <button className="w-7 h-7 rounded-lg bg-primary-50 text-primary-600 flex items-center justify-center hover:bg-primary-600 hover:text-white transition-colors shadow-sm">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path><line x1="3" y1="6" x2="21" y2="6"></line><path d="M16 10a4 4 0 0 1-8 0"></path></svg>
                    </button>
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      </section>

      {/* 8. PROMOTIONAL BRAND SECTION */}
      <section className="mb-24 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-primary-100 rounded-[32px] overflow-hidden flex flex-col md:flex-row items-center border border-primary-200/50">
          <div className="md:w-1/2 relative h-64 md:h-[300px] w-full">
            <img src={mockImages.dogHero} alt="Happy pets" className="w-full h-full object-cover" />
            <div className="absolute inset-0 bg-gradient-to-r from-transparent to-primary-100"></div>
          </div>
          <div className="md:w-1/2 p-8 md:p-12 z-10">
            <h2 className="text-3xl font-extrabold text-primary-900 mb-4 leading-tight">
              Cùng PetCare Hub <br/> kiến tạo một thế giới tốt đẹp hơn cho thú cưng!
            </h2>
            <p className="text-primary-800/80 mb-8 max-w-md">
              Chúng tôi cam kết mang đến những sản phẩm và dịch vụ tốt nhất, để mỗi thú cưng đều được yêu thương và chăm sóc đúng cách.
            </p>
            <Link to="/about">
              <Button className="rounded-full shadow-md px-8 shadow-primary-500/20">Khám phá ngay →</Button>
            </Link>
          </div>
        </div>
      </section>

      {/* 9. CUSTOMER REVIEWS */}
      <section className="mb-24 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-white rounded-[32px] shadow-sm border border-gray-100 p-8 md:p-12 flex flex-col lg:flex-row gap-12 items-center">
          
          <div className="lg:w-1/3 flex flex-col items-center lg:items-start text-center lg:text-left">
            <div className="flex items-center gap-3 mb-2">
              <div className="w-12 h-12 bg-secondary-100 text-secondary-500 rounded-2xl flex items-center justify-center text-2xl">
                <FiStar className="fill-current" />
              </div>
              <div>
                <h2 className="text-2xl font-bold text-gray-900">Đánh giá từ khách hàng</h2>
                <p className="text-gray-500 text-sm">Hàng ngàn khách hàng đã tin tưởng</p>
              </div>
            </div>
            
            <div className="flex items-center gap-6 mt-8">
              <div className="text-5xl font-extrabold text-gray-900">4.8<span className="text-2xl text-gray-400 font-bold">/5</span></div>
              <div className="flex flex-col gap-1">
                <div className="flex text-secondary-400 text-lg">
                  <FiStar className="fill-current" /><FiStar className="fill-current" /><FiStar className="fill-current" /><FiStar className="fill-current" /><FiStar className="fill-current" />
                </div>
                <span className="text-sm text-gray-500 font-medium">Dựa trên 2.500+ đánh giá</span>
              </div>
            </div>
          </div>

          <div className="lg:w-2/3 relative w-full">
            <div className="absolute top-1/2 -left-6 -translate-y-1/2 w-12 h-12 bg-white rounded-full shadow-md flex items-center justify-center text-gray-400 hover:text-primary-600 cursor-pointer z-10 border border-gray-100">
              <FiChevronLeft size={24} />
            </div>
            <div className="absolute top-1/2 -right-6 -translate-y-1/2 w-12 h-12 bg-white rounded-full shadow-md flex items-center justify-center text-gray-400 hover:text-primary-600 cursor-pointer z-10 border border-gray-100">
              <FiChevronRight size={24} />
            </div>

            <Card className="bg-gray-50/50 border border-gray-100 p-8 rounded-3xl relative overflow-hidden">
              <div className="absolute top-4 right-8 text-8xl text-gray-100 font-serif leading-none">"</div>
              
              <div className="flex items-start gap-4 mb-4 relative z-10">
                <img src={mockImages.avatar1} alt="Customer" className="w-12 h-12 rounded-full object-cover shadow-sm" />
                <div>
                  <h4 className="font-bold text-gray-900">Nguyễn Thị Mai</h4>
                  <div className="flex text-secondary-400 text-sm mt-0.5">
                    <FiStar className="fill-current" /><FiStar className="fill-current" /><FiStar className="fill-current" /><FiStar className="fill-current" /><FiStar className="fill-current" />
                  </div>
                </div>
                <span className="ml-auto text-xs text-gray-400 font-medium">12/04/2025</span>
              </div>
              <p className="text-gray-600 relative z-10 leading-relaxed">
                "Dịch vụ grooming rất chuyên nghiệp, bé nhà mình về rất sạch và thơm. Nhân viên thân thiện, nhiệt tình! Chắc chắn sẽ quay lại ủng hộ PetCare Hub thường xuyên."
              </p>
            </Card>
          </div>

        </div>
      </section>

    </div>
  );
};

export default HomePage;