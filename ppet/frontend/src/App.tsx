import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, useLocation } from 'react-router-dom';

function ScrollToTop() {
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  return null;
}
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
        <div className="flex flex-col min-h-screen">
          <Header />
          <main className="flex-grow">
            <Routes>
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
              <Route path="/services" element={<ServiceListPage />} />
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
              <Route path="/unauthorized" element={<div className="p-8">403 — Bạn không có quyền truy cập.</div>} />
              <Route path="*" element={<div className="p-8">404 — Không tìm thấy trang.</div>} />
            </Routes>
          </main>
          <Footer />
        </div>
        <Toaster position="top-right" />
      </AuthProvider>
    </Router>
  );
}

export default App;







