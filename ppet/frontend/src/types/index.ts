// ===== Auth Types =====
export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  firstName: string
  lastName: string
  email: string
  password: string
  phone?: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
}

export interface User {
  id: number
  username: string
  email: string
  firstName: string
  lastName: string
  phone?: string
  avatarUrl?: string
  roles: string[]
  enabled: boolean
}

// ===== Pet Types =====
export interface Pet {
  id: number
  name: string
  species: Species
  breed: Breed
  dateOfBirth: string
  gender: string
  color: string
  weight: number
  price: number
  description: string
  imageUrl?: string
  healthStatus: HealthStatus
  availabilityStatus: AvailabilityStatus
}

export interface Species {
  id: number
  name: string
  description?: string
}

export interface Breed {
  id: number
  speciesId: number
  name: string
  description?: string
  avgLifespan?: number
  sizeCategory?: string
  energyLevel?: string
  groomingNeeds?: string
  goodWithChildren?: boolean
  noiseLevel?: string
}

// ===== Product Types =====
export interface Product {
  id: number
  name: string
  description: string
  price: number
  imageUrl?: string
  category: ProductCategory
  sku: string
  active: boolean
}

export interface ProductCategory {
  id: number
  name: string
  description?: string
  parentCategoryId?: number
}

// ===== Order Types =====
export interface Order {
  id: number
  orderNumber: string
  status: OrderStatus
  subtotal: number
  discountAmount: number
  taxAmount: number
  totalAmount: number
  shippingAddress: string
  paymentMethod: string
  paymentStatus: string
  items: OrderItem[]
  createdAt: string
}

export interface OrderItem {
  id: number
  itemType: 'PET' | 'PRODUCT'
  petId?: number
  productId?: number
  quantity: number
  unitPrice: number
  subtotal: number
}

// ===== Notification Types =====
export interface Notification {
  id: number
  type: string
  title: string
  message: string
  read: boolean
  data?: Record<string, unknown>
  createdAt: string
}

// ===== Pagination =====
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// ===== Enums =====
export type HealthStatus = 'HEALTHY' | 'DUE_FOR_VACCINATION' | 'UNDER_TREATMENT' | 'RECOVERING' | 'HEALTH_WARNING'
export type AvailabilityStatus = 'AVAILABLE' | 'SOLD' | 'RESERVED' | 'UNAVAILABLE'
export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'PROCESSING' | 'SHIPPING' | 'DELIVERED' | 'CANCELLED'
export type MembershipLevel = 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM'
