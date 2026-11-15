import React, { useEffect } from 'react';
import { ContentPage, ContentList, policyNames } from './pages/content/PublicContent';
import { MyReturns } from './pages/orders/Returns';
import { BrowserRouter as Router, Routes, Route, useLocation, Navigate } from 'react-router-dom';
import { PermissionProvider } from './contexts/PermissionContext';
import { useAuth } from './contexts/AuthContext';
const AdminPortal = React.lazy(() => import('./pages/admin/AdminPortal'));
const ChangePasswordPage = React.lazy(() => import('./pages/auth/ChangePasswordPage'));

function PublicHeader() { return useLocation().pathname.startsWith('/admin') ? null : <Header />; }
function PublicFooter() { return useLocation().pathname.startsWith('/admin') ? null : <Footer />; }
function SessionGate({ children }: { children: React.ReactNode }) {
  const { user } = useAuth(); const location = useLocation();
  if (user?.mustChangePassword && location.pathname !== '/change-password') return <Navigate to="/change-password" replace />;
  return children;
}

function ScrollToTop() {
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  return null;
}
import { CarePackages, PackageDetail, MyPackages } from './components/customer/CarePackages';
import { WishlistPage } from './components/customer/Wishlist';
import { MyReviews, ServiceDetail } from './components/customer/Reviews';
import ProtectedRoute from './components/common/ProtectedRoute';
import { OrdersPage } from './pages/orders/OrdersPage';
import { AuthProvider } from './contexts/AuthContext';
import Header from './components/layout/Header';
import Footer from './components/layout/Footer';
import { Toaster } from 'react-hot-toast';

import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { PetListPage } from './pages/pets/PetListPage';
import { PetDetailPage } from './pages/pets/PetDetailPage';
import { ProductListPage } from './pages/products/ProductListPage';
import { ProductDetailPage } from './pages/products/ProductDetailPage';
import { CartPage } from './pages/orders/CartPage';
import { CheckoutPage } from './pages/orders/CheckoutPage';
import { OrderTrackingPage } from './pages/orders/OrderTrackingPage';
import { ServiceListPage } from './pages/services/ServiceListPage';
import { AppointmentConfirmation, MyAppointments } from './pages/services/MyAppointments';
import { BookingPage } from './pages/services/BookingPage';
import { MyPetsPage } from './pages/pets/MyPetsPage';
import { PetHealthHistoryPage } from './pages/pets/PetHealthHistoryPage';
import { PetVaccinationPage } from './pages/pets/PetVaccinationPage';
import { LoyaltyPage } from './pages/customer/LoyaltyPage';
import { MembershipPage } from './pages/customer/MembershipPage';
import { NotificationsPage } from './pages/customer/NotificationsPage';
import { PetMatchingPage } from './pages/pets/PetMatchingPage';
import { HomePage } from './pages/HomePage';
import { ProfilePage } from './pages/customer/ProfilePage';

function App() {
  return (
    <Router basename={import.meta.env.BASE_URL}>
      <ScrollToTop />
      <AuthProvider>
        <PermissionProvider><SessionGate>
        <div className="flex flex-col min-h-screen">
          <PublicHeader />
          <main className="grow">
            <Routes>
              {Object.keys(policyNames).map(page=><Route key={page} path={"/"+page} element={<ContentPage page={page}/>}/>)}
              <Route path="/articles" element={<ContentList/>}/><Route path="/articles/:slug" element={<ContentPage/>}/><Route path="/faq" element={<ContentList kind="FAQ"/>}/><Route path="/returns" element={<ProtectedRoute roles={['CUSTOMER']}><MyReturns/></ProtectedRoute>}/>
              <Route path="/admin/*" element={<ProtectedRoute><React.Suspense fallback={<p role="status" className="p-6">Đang tải cổng quản trị…</p>}><AdminPortal /></React.Suspense></ProtectedRoute>} />
              <Route path="/change-password" element={<ProtectedRoute><React.Suspense fallback={<p>Đang tải…</p>}><ChangePasswordPage /></React.Suspense></ProtectedRoute>} />
              <Route path="/" element={<HomePage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/pets" element={<PetListPage />} />
              <Route path="/pets/:id" element={<PetDetailPage />} />
              <Route path="/products" element={<ProductListPage />} />
              <Route path="/products/:id" element={<ProductDetailPage />} />
              <Route path="/cart" element={<ProtectedRoute><CartPage /></ProtectedRoute>} />
              <Route path="/checkout" element={<ProtectedRoute><CheckoutPage /></ProtectedRoute>} />
              <Route path="/orders/tracking" element={<ProtectedRoute><OrderTrackingPage /></ProtectedRoute>} />
              <Route path="/care-packages" element={<CarePackages/>}/>
              <Route path="/care-packages/:id" element={<PackageDetail/>}/>
              <Route path="/care-packages/:id/checkout" element={<ProtectedRoute roles={['CUSTOMER']}><PackageDetail checkout/></ProtectedRoute>}/>
              <Route path="/my-care-packages" element={<ProtectedRoute roles={['CUSTOMER']}><MyPackages/></ProtectedRoute>}/>
              <Route path="/wishlist" element={<ProtectedRoute roles={['CUSTOMER']}><WishlistPage /></ProtectedRoute>} />
              <Route path="/my-reviews" element={<ProtectedRoute roles={['CUSTOMER']}><MyReviews /></ProtectedRoute>} />
              <Route path="/services/:id" element={<ServiceDetail />} />
              <Route path="/services" element={<ServiceListPage />} />
              <Route path="/appointments/:id/confirmation" element={<ProtectedRoute><AppointmentConfirmation/></ProtectedRoute>} />
              <Route path="/my-appointments" element={<ProtectedRoute><MyAppointments/></ProtectedRoute>} />
              <Route path="/services/booking" element={<ProtectedRoute><BookingPage /></ProtectedRoute>} />
              <Route path="/my-pets" element={<ProtectedRoute><MyPetsPage /></ProtectedRoute>} />
              <Route path="/my-pets/:id/health" element={<ProtectedRoute><PetHealthHistoryPage /></ProtectedRoute>} />
              <Route path="/my-pets/:id/vaccinations" element={<ProtectedRoute><PetVaccinationPage /></ProtectedRoute>} />
              <Route path="/loyalty" element={<ProtectedRoute><LoyaltyPage /></ProtectedRoute>} />
              <Route path="/membership" element={<ProtectedRoute><MembershipPage /></ProtectedRoute>} />
              <Route path="/notifications" element={<ProtectedRoute><NotificationsPage /></ProtectedRoute>} />
              <Route path="/pet-matching" element={<ProtectedRoute><PetMatchingPage /></ProtectedRoute>} />
              <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
              <Route path="/orders" element={<ProtectedRoute><OrdersPage /></ProtectedRoute>} />
              <Route path="/orders/:id/confirmation" element={<ProtectedRoute><OrderTrackingPage confirmation /></ProtectedRoute>} />
              <Route path="/orders/:id" element={<ProtectedRoute><OrderTrackingPage /></ProtectedRoute>} />
              <Route path="/unauthorized" element={<div className="p-8">403 — Bạn không có quyền truy cập.</div>} />
              <Route path="*" element={<div className="p-8">404 — Không tìm thấy trang.</div>} />
            </Routes>
          </main>
          <PublicFooter />
        </div>
        <Toaster position="top-right" />
        </SessionGate></PermissionProvider>
      </AuthProvider>
    </Router>
  );
}

export default App;







