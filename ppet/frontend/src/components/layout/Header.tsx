import { FormEvent, useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { FiSearch, FiBell, FiShoppingCart, FiMenu, FiX } from 'react-icons/fi';
import { MdPets } from 'react-icons/md';
import apiClient from '../../api/client';
import { useAuth } from '../../contexts/AuthContext';

const navigation = [['/', 'Trang chủ'], ['/pets', 'Thú cưng'], ['/products', 'Sản phẩm'], ['/services', 'Dịch vụ'], ['/pet-matching', 'Tìm thú cưng phù hợp']];
const accountLinks = [['/profile', 'Hồ sơ cá nhân'], ['/my-pets', 'Thú cưng của tôi'], ['/orders', 'Đơn hàng'], ['/loyalty', 'Điểm thưởng']];
const touch = 'inline-flex min-h-11 min-w-11 items-center justify-center rounded-xl hover:bg-primary-50 focus-visible:outline-primary-600';

export default function Header() {
  const { user, logout } = useAuth();
  const navigate = useNavigate(); const location = useLocation();
  const [unread, setUnread] = useState(0); const [open, setOpen] = useState(false); const [query, setQuery] = useState('');
  const toggle = useRef<HTMLButtonElement>(null); const panel = useRef<HTMLDivElement>(null); const account = useRef<HTMLDetailsElement>(null);
  useEffect(() => {
    setUnread(0); if (!user) return;
    const controller = new AbortController();
    const load = () => apiClient.get('/notifications/unread-count', { signal: controller.signal }).then(({ data }) => {
      if (!controller.signal.aborted) setUnread(data.count);
    }).catch(() => {});
    void load(); const timer = window.setInterval(load, 30000);
    return () => { controller.abort(); window.clearInterval(timer); };
  }, [user]);
  useEffect(() => { setOpen(false); if (account.current) account.current.open = false; }, [location.key]);
  useEffect(() => {
    if (!open) return;
    panel.current?.querySelector<HTMLElement>('a, input, button')?.focus();
    const escape = (event: KeyboardEvent) => { if (event.key === 'Escape') { setOpen(false); toggle.current?.focus(); } };
    const resized = () => { if (window.innerWidth >= 768) setOpen(false); };
    window.addEventListener('keydown', escape); window.addEventListener('resize', resized);
    return () => { window.removeEventListener('keydown', escape); window.removeEventListener('resize', resized); };
  }, [open]);
  const signOut = () => { setOpen(false); logout(); navigate('/login'); };
  const search = (event: FormEvent) => { event.preventDefault(); setOpen(false); navigate(`/products?keyword=${encodeURIComponent(query.trim())}`); };
  const links = navigation.map(([to, label]) => <Link key={to} to={to} onClick={() => setOpen(false)} className={`${touch} px-2 text-sm font-medium`}>{label}</Link>);
  const personal = accountLinks.map(([to, label]) => <Link key={to} to={to} onClick={() => setOpen(false)} className="flex min-h-11 items-center rounded-lg px-3 hover:bg-primary-50">{label}</Link>);
  return <header className="sticky top-0 z-50 border-b border-gray-100 bg-white shadow-xs">
    <div className="mx-auto max-w-7xl px-3 sm:px-6 lg:px-8">
      <div className="flex h-20 min-w-0 items-center justify-between gap-2">
        <Link to="/" aria-label="PetCare Hub — Trang chủ" className="min-w-0 shrink flex items-center gap-2">
          <MdPets className="shrink-0 rounded-lg bg-primary-500 p-1 text-white" size={32} />
          <span className="min-w-0"><span className="block whitespace-nowrap text-xl sm:text-2xl font-extrabold">PetCare Hub</span><span className="hidden sm:block text-[10px] text-gray-500">Better Care, Happier Pets</span></span>
        </Link>
        <nav aria-label="Điều hướng chính" className="hidden xl:flex items-center gap-1">{links}</nav>
        <div className="flex shrink-0 items-center gap-1 sm:gap-2">
          <Link to="/products" aria-label="Tìm kiếm sản phẩm" className={`${touch} hidden md:inline-flex`}><FiSearch size={22} /></Link>
          <Link to="/cart" aria-label="Giỏ hàng" className={touch}><FiShoppingCart size={22} /></Link>
          <div className="hidden md:flex min-w-0 items-center gap-2">
            {user ? <>
              <Link to="/notifications" aria-label={`Thông báo, ${unread} chưa đọc`} className={`${touch} relative`}><FiBell size={22} />{unread > 0 && <span className="absolute right-0 top-0 rounded-full bg-primary-600 px-1 text-[10px] text-white">{unread > 99 ? '99+' : unread}</span>}</Link>
              <details ref={account} className="relative">
                <summary className={`${touch} max-w-36 cursor-pointer list-none gap-2 px-2`}><img src={user.avatarUrl || 'https://placehold.co/32x32?text=U'} alt="" className="h-8 w-8 shrink-0 rounded-full" /><span className="truncate text-sm">{user.firstName}</span></summary>
                <div className="absolute right-0 w-56 rounded-xl border bg-white p-2 shadow-lg">{personal}<button onClick={signOut} className={`${touch} w-full text-red-600`}>Đăng xuất</button></div>
              </details>
            </> : <><Link to="/login" className={`${touch} px-2 text-sm`}>Đăng nhập</Link><Link to="/register" className={`${touch} bg-primary-600 px-3 text-sm text-white`}>Đăng ký</Link></>}
          </div>
          <button ref={toggle} className={`${touch} md:hidden`} aria-label={open ? 'Đóng menu' : 'Mở menu'} aria-expanded={open} aria-controls="mobile-navigation" onClick={() => setOpen(value => !value)}>{open ? <FiX size={24} /> : <FiMenu size={24} />}</button>
        </div>
      </div>
      <nav aria-label="Điều hướng máy tính bảng" className="hidden md:flex xl:hidden flex-wrap justify-center gap-2 pb-2">{links}</nav>
      {open && <div ref={panel} id="mobile-navigation" className="max-h-[calc(100dvh-5rem)] overflow-y-auto border-t pb-4 md:hidden">
        {!user ? <div className="flex gap-3 py-3"><Link to="/login" className={`${touch} flex-1 border`}>Đăng nhập</Link><Link to="/register" className={`${touch} flex-1 bg-primary-600 text-white`}>Đăng ký</Link></div> : <div className="py-3"><p className="break-words font-semibold">{user.firstName} {user.lastName}</p><Link to="/notifications" className={`${touch} px-2`}>Thông báo ({unread > 99 ? '99+' : unread})</Link>{personal}<button onClick={signOut} className={`${touch} px-3 text-red-600`}>Đăng xuất</button></div>}
        <form onSubmit={search} className="flex min-w-0 gap-2 py-2"><input aria-label="Từ khóa sản phẩm" value={query} onChange={event => setQuery(event.target.value)} className="min-w-0 flex-1 rounded-xl border px-3" placeholder="Tìm sản phẩm" /><button aria-label="Tìm kiếm" className={touch}><FiSearch size={22} /></button></form>
        <nav aria-label="Điều hướng di động" className="flex flex-col items-stretch">{links}</nav>
      </div>}
    </div>
  </header>;
}
