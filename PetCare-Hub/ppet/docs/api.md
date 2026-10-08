# PetCare Hub REST API Specification

## 1. API Conventions
- **Base URL**: `/api/v1`
- **Content Type**: `application/json` for request/response bodies
- **Standard HTTP status codes**: 200 (OK), 201 (Created), 400 (Bad Request), 401 (Unauthorized), 403 (Forbidden), 404 (Not Found), 500 (Internal Error)
- **Pagination**: `page`, `size`, `sort` query params
  - Response format: `{ "content": [], "page": 0, "size": 10, "totalElements": 100, "totalPages": 10 }`
- **Error response format**:
  ```json
  {
    "timestamp": "2023-10-01T12:00:00Z",
    "status": 400,
    "error": "Bad Request",
    "message": "Invalid input data",
    "path": "/api/v1/pets"
  }
  ```
- **Authentication**: Bearer JWT token in Authorization header
- **Date format**: ISO 8601

---

## 2. Authentication API
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/auth/register` | Register new customer | Public |
| POST | `/api/v1/auth/login` | Login, returns JWT | Public |
| POST | `/api/v1/auth/refresh` | Refresh token | Authenticated |
| GET | `/api/v1/auth/me` | Get current user profile | Authenticated |
| PUT | `/api/v1/auth/me` | Update profile | Authenticated |
| PUT | `/api/v1/auth/change-password` | Change password | Authenticated |

**Example Request (POST /register)**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "SecurePassword123",
  "phoneNumber": "+1234567890"
}
```

**Example Response (POST /login)**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
  "refreshToken": "dGVzdC1yZWZyZXNoLXRva2Vu...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

---

## 3. Pet API
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/pets` | List pets with filters | Public |
| GET | `/api/v1/pets/{id}` | Get pet detail | Public |
| POST | `/api/v1/pets` | Create pet | ADMIN, WAREHOUSE |
| PUT | `/api/v1/pets/{id}` | Update pet | ADMIN, WAREHOUSE |
| DELETE | `/api/v1/pets/{id}` | Soft delete pet | ADMIN |
| GET | `/api/v1/pets/{id}/medical-records` | Get pet medical history | CUSTOMER(owner), VET, ADMIN |
| GET | `/api/v1/pets/{id}/vaccinations` | Get pet vaccinations | CUSTOMER(owner), VET, ADMIN |

**Query Parameters (GET /pets)**:
`species`, `breed`, `minPrice`, `maxPrice`, `gender`, `healthStatus`, `availabilityStatus`, `ageMin`, `ageMax`, `page`, `size`, `sort`

**Example Request (POST /pets)**
```json
{
  "name": "Bella",
  "species": "DOG",
  "breed": "Golden Retriever",
  "gender": "FEMALE",
  "dateOfBirth": "2023-01-15T00:00:00Z",
  "price": 800.00,
  "description": "Friendly and playful.",
  "healthStatus": "HEALTHY",
  "availabilityStatus": "AVAILABLE"
}
```

---

## 4. Product API
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/products` | List products with filters | Public |
| GET | `/api/v1/products/{id}` | Get product detail | Public |
| POST | `/api/v1/products` | Create product | ADMIN |
| PUT | `/api/v1/products/{id}` | Update product | ADMIN |
| DELETE | `/api/v1/products/{id}` | Soft delete product | ADMIN |
| GET | `/api/v1/product-categories` | List categories | Public |
| POST | `/api/v1/product-categories` | Create category | ADMIN |

**Query Parameters (GET /products)**:
`categoryId`, `minPrice`, `maxPrice`, `brand`, `inStock`, `page`, `size`, `sort`

**Example Request (POST /products)**
```json
{
  "name": "Premium Dog Food",
  "categoryId": 1,
  "brand": "HealthyPaws",
  "price": 45.99,
  "stockQuantity": 100,
  "description": "High-quality dog food."
}
```

---

## 5. Cart API
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/cart` | Get current user's cart | CUSTOMER |
| POST | `/api/v1/cart/items` | Add item to cart | CUSTOMER |
| PUT | `/api/v1/cart/items/{itemId}` | Update cart item quantity | CUSTOMER |
| DELETE | `/api/v1/cart/items/{itemId}` | Remove item from cart | CUSTOMER |
| DELETE | `/api/v1/cart` | Clear cart | CUSTOMER |

**Example Request (POST /cart/items)**
```json
{
  "productId": 123,
  "quantity": 2
}
```

---

## 6. Order API
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/orders` | Create order from cart | CUSTOMER |
| GET | `/api/v1/orders` | List orders (own for customer, all for staff) | Authenticated |
| GET | `/api/v1/orders/{id}` | Get order detail | Authenticated |
| PUT | `/api/v1/orders/{id}/status` | Update order status | SALES, ADMIN |
| PUT | `/api/v1/orders/{id}/cancel` | Cancel order | CUSTOMER, SALES, ADMIN |
| POST | `/api/v1/orders/{id}/apply-promotion` | Apply promotion code | CUSTOMER |

**Example Request (POST /orders)**
```json
{
  "shippingAddressId": 45,
  "paymentMethod": "CREDIT_CARD",
  "notes": "Please leave at front door."
}
```

**Example Response (GET /orders/{id})**
```json
{
  "id": 1001,
  "status": "PROCESSING",
  "totalAmount": 91.98,
  "items": [
    { "productId": 123, "quantity": 2, "price": 45.99 }
  ],
  "createdAt": "2023-10-01T12:00:00Z"
}
```

---

## 7. Service & Appointment API
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/services` | List available services | Public |
| GET | `/api/v1/services/{id}` | Get service detail | Public |
| POST | `/api/v1/services` | Create service | ADMIN |
| PUT | `/api/v1/services/{id}` | Update service | ADMIN |
| GET | `/api/v1/appointments` | List appointments | Authenticated |
| POST | `/api/v1/appointments` | Book appointment | CUSTOMER |
| GET | `/api/v1/appointments/{id}` | Get appointment detail | Authenticated |
| PUT | `/api/v1/appointments/{id}/status` | Update appointment status | VET, SALES, ADMIN |
| PUT | `/api/v1/appointments/{id}/cancel` | Cancel appointment | CUSTOMER, ADMIN |
| GET | `/api/v1/appointments/available-slots` | Get available time slots | CUSTOMER |

**Example Request (POST /appointments)**
```json
{
  "serviceId": 5,
  "petId": 12,
  "appointmentDate": "2023-10-15T14:30:00Z",
  "notes": "Routine checkup."
}
```

---

## 8. Medical Records API
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/medical-records` | Create medical record | VET |
| GET | `/api/v1/medical-records/{id}` | Get medical record | VET, ADMIN, CUSTOMER(pet owner) |
| PUT | `/api/v1/medical-records/{id}` | Update medical record | VET |
| POST | `/api/v1/vaccinations` | Record vaccination | VET |
| GET | `/api/v1/vaccinations/{id}` | Get vaccination record | VET, ADMIN, CUSTOMER(pet owner) |
| POST | `/api/v1/treatments` | Create treatment | VET |
| PUT | `/api/v1/treatments/{id}` | Update treatment | VET |
| PUT | `/api/v1/pets/{id}/health-status` | Update pet health status | VET |

**Example Request (POST /medical-records)**
```json
{
  "petId": 12,
  "vetId": 3,
  "diagnosis": "Healthy",
  "notes": "General checkup completed. No issues found.",
  "weight": 25.4,
  "temperature": 38.5
}
```

---

## 9. Inventory API
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/inventory` | List inventory | WAREHOUSE, ADMIN |
| GET | `/api/v1/inventory/{id}` | Get inventory item | WAREHOUSE, ADMIN |
| PUT | `/api/v1/inventory/{id}` | Update inventory settings | WAREHOUSE, ADMIN |
| POST | `/api/v1/inventory/stock-in` | Record stock in | WAREHOUSE |
| POST | `/api/v1/inventory/stock-out` | Record stock out | WAREHOUSE |
| GET | `/api/v1/inventory/low-stock` | Get low-stock items | WAREHOUSE, ADMIN |
| GET | `/api/v1/inventory/transactions` | List inventory transactions | WAREHOUSE, ADMIN |

**Example Request (POST /inventory/stock-in)**
```json
{
  "productId": 123,
  "quantity": 50,
  "supplierId": 10,
  "costPrice": 30.00,
  "notes": "Monthly restock."
}
```

---

## 10. Review API
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/reviews` | Create review | CUSTOMER |
| GET | `/api/v1/reviews` | List reviews (filterable by type, item) | Public |
| PUT | `/api/v1/reviews/{id}` | Update own review | CUSTOMER |
| DELETE | `/api/v1/reviews/{id}` | Delete own review | CUSTOMER, ADMIN |
| PUT | `/api/v1/reviews/{id}/approve` | Approve review | SALES, ADMIN |

**Example Request (POST /reviews)**
```json
{
  "targetType": "PRODUCT",
  "targetId": 123,
  "rating": 5,
  "comment": "My dog loves this food!"
}
```

---

## 11. Loyalty API
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/loyalty/account` | Get own loyalty account | CUSTOMER |
| GET | `/api/v1/loyalty/transactions` | Get own loyalty transactions | CUSTOMER |
| POST | `/api/v1/loyalty/redeem` | Redeem points | CUSTOMER |
| GET | `/api/v1/loyalty/tiers` | Get tier information | Public |

**Example Request (POST /loyalty/redeem)**
```json
{
  "points": 500,
  "rewardId": 2
}
```

---

## 12. Promotion API
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/promotions` | List promotions | SALES, ADMIN |
| POST | `/api/v1/promotions` | Create promotion | SALES, ADMIN |
| PUT | `/api/v1/promotions/{id}` | Update promotion | SALES, ADMIN |
| DELETE | `/api/v1/promotions/{id}` | Deactivate promotion | ADMIN |
| POST | `/api/v1/promotions/validate` | Validate promotion code | CUSTOMER |

**Example Request (POST /promotions)**
```json
{
  "code": "SUMMER20",
  "discountPercentage": 20,
  "startDate": "2023-06-01T00:00:00Z",
  "endDate": "2023-08-31T23:59:59Z",
  "usageLimit": 1000
}
```

---

## 13. Pet Matching API
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/pet-matching/quiz` | Submit matching quiz | CUSTOMER |
| GET | `/api/v1/pet-matching/results/{quizId}` | Get matching results | CUSTOMER |
| GET | `/api/v1/pet-matching/history` | Get quiz history | CUSTOMER |

**Example Request (POST /pet-matching/quiz)**
```json
{
  "livingSpace": "APARTMENT",
  "activityLevel": "LOW",
  "experienceLevel": "BEGINNER",
  "hasChildren": false,
  "hasOtherPets": true
}
```

---

## 14. Notification API
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/notifications` | Get own notifications | Authenticated |
| PUT | `/api/v1/notifications/{id}/read` | Mark as read | Authenticated |
| PUT | `/api/v1/notifications/read-all` | Mark all as read | Authenticated |
| GET | `/api/v1/notifications/unread-count` | Get unread count | Authenticated |

**Example Response (GET /notifications)**
```json
{
  "content": [
    {
      "id": 1,
      "type": "ORDER_UPDATE",
      "title": "Order Shipped",
      "message": "Your order #1001 has been shipped.",
      "isRead": false,
      "createdAt": "2023-10-02T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

## 15. Admin API
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/admin/users` | List all users | ADMIN |
| GET | `/api/v1/admin/users/{id}` | Get user detail | ADMIN |
| PUT | `/api/v1/admin/users/{id}` | Update user | ADMIN |
| PUT | `/api/v1/admin/users/{id}/roles` | Update user roles | ADMIN |
| PUT | `/api/v1/admin/users/{id}/toggle-status` | Enable/disable user | ADMIN |
| GET | `/api/v1/admin/reports/revenue` | Revenue report | ADMIN |
| GET | `/api/v1/admin/reports/orders` | Order statistics | ADMIN |
| GET | `/api/v1/admin/reports/inventory` | Inventory report | ADMIN |
| GET | `/api/v1/admin/dashboard` | Dashboard summary | ADMIN |
| GET | `/api/v1/admin/config` | Get system config | ADMIN |
| PUT | `/api/v1/admin/config` | Update system config | ADMIN |

---

## 16. WebSocket Endpoints
- **Connection**: `ws://host/ws` (with JWT auth)
- **Subscribe (Personal)**: `/user/queue/notifications`
- **Subscribe (Sales Staff)**: `/topic/orders`
- **Subscribe (Warehouse Staff)**: `/topic/inventory/alerts`
- **Message format**:
  ```json
  {
    "type": "NOTIFICATION",
    "title": "Low Stock Alert",
    "message": "Product Premium Dog Food is running low.",
    "data": { "productId": 123 },
    "timestamp": "2023-10-01T12:00:00Z"
  }
  ```

---

## 17. Care Package API
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/care-packages` | List packages | Public |
| GET | `/api/v1/care-packages/{id}` | Get package detail | Public |
| POST | `/api/v1/care-packages` | Create package | ADMIN |
| PUT | `/api/v1/care-packages/{id}` | Update package | ADMIN |
| POST | `/api/v1/care-packages/subscribe` | Subscribe to package | CUSTOMER |
| GET | `/api/v1/care-packages/subscriptions` | Get own subscriptions | CUSTOMER |
| PUT | `/api/v1/care-packages/subscriptions/{id}/cancel` | Cancel subscription | CUSTOMER |

**Example Request (POST /care-packages/subscribe)**
```json
{
  "packageId": 3,
  "frequency": "MONTHLY",
  "shippingAddressId": 45
}
```
