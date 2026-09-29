import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
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
import { GroomingBookingPage } from './pages/services/GroomingBookingPage';
import { VetBookingPage } from './pages/services/VetBookingPage';
import { MyPetsPage } from './pages/pets/MyPetsPage';
import { PetHealthHistoryPage } from './pages/pets/PetHealthHistoryPage';
import { PetVaccinationPage } from './pages/pets/PetVaccinationPage';
import { LoyaltyPage } from './pages/customer/LoyaltyPage';
import { MembershipPage } from './pages/customer/MembershipPage';
import { NotificationsPage } from './pages/customer/NotificationsPage';
import { PetMatchingPage } from './pages/pets/PetMatchingPage';
// Placeholder for HomePage
const HomePage = () => <div className="p-8 text-center"><h1 className="text-3xl font-bold">Homepage Coming Soon</h1></div>;

function App() {
  return (
    <Router>
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
              <Route path="/cart" element={<CartPage />} />
              <Route path="/checkout" element={<CheckoutPage />} />
              <Route path="/orders/tracking" element={<OrderTrackingPage />} />
              <Route path="/services" element={<ServiceListPage />} />
              <Route path="/services/grooming/booking" element={<GroomingBookingPage />} />
              <Route path="/services/vet/booking" element={<VetBookingPage />} />
              <Route path="/my-pets" element={<MyPetsPage />} />
              <Route path="/my-pets/:id/health" element={<PetHealthHistoryPage />} />
              <Route path="/my-pets/:id/vaccinations" element={<PetVaccinationPage />} />
              <Route path="/loyalty" element={<LoyaltyPage />} />
              <Route path="/membership" element={<MembershipPage />} />
              <Route path="/notifications" element={<NotificationsPage />} />
              <Route path="/pet-matching" element={<PetMatchingPage />} />
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






