# PetCare Hub - Development Roadmap

This document outlines the detailed, phased implementation plan for the PetCare Hub project.

## Phase 1: Project Setup & Foundation (2-3 days)
### Objectives
- Initialize Spring Boot project with Maven
- Initialize React + TypeScript + Vite + Tailwind CSS project
- Configure PostgreSQL connection
- Set up project structure (packages, folders)
- Configure basic Spring Security
- Set up Git repository with .gitignore

### Tasks
1. Create Spring Boot project (spring initializr deps: Web, JPA, Security, Validation, WebSocket, PostgreSQL, Lombok, DevTools)
2. Configure `application.yml` (dev profile, DB connection, JPA settings)
3. Create backend package structure
4. Create React project with Vite, install Tailwind, configure TypeScript
5. Create frontend folder structure
6. Configure CORS
7. Initialize Git repo with proper `.gitignore`

### Deliverables
- Running Spring Boot application
- Running React dev server
- PostgreSQL connection verified
- Git repository initialized

### Verification
- `mvn clean install` passes
- `npm run build` passes
- Application starts without errors
- Database connection established

## Phase 2: Domain Model & Database (2-3 days)
### Objectives
- Create all JPA entities
- Define enums (HealthStatus, OrderStatus, etc.)
- Set up Flyway/Liquibase migrations or JPA auto-DDL for dev
- Seed initial data

### Tasks
1. Create base entity (`BaseEntity` with id, createdAt, updatedAt)
2. Create `User`, `Role` entities + join table
3. Create `Species`, `Breed`, `Pet` entities
4. Create `Product`, `ProductCategory` entities
5. Create `Inventory`, `InventoryTransaction` entities
6. Create `Order`, `OrderItem` entities
7. Create `Service`, `Appointment` entities
8. Create `MedicalRecord`, `Vaccination`, `Treatment` entities
9. Create `Review` entity
10. Create `Promotion`, `PromotionUsage` entities
11. Create `LoyaltyAccount`, `LoyaltyTransaction` entities
12. Create `Notification` entity
13. Create `PetMatchingQuiz`, `PetMatchingResult` entities
14. Create `CarePackage`, `CarePackageSubscription` entities
15. Create `Cart`, `CartItem` entities
16. Create seed data (`data.sql` or `CommandLineRunner`)

### Deliverables
- All entities with proper JPA annotations
- Database schema auto-generated
- Seed data loaded
- All repositories created

### Verification
- Application starts, tables created
- Seed data visible in database
- No JPA mapping errors

## Phase 3: Authentication & Authorization (2-3 days)
### Objectives
- Implement JWT authentication
- Implement user registration and login
- Role-based access control
- Spring Security configuration

### Tasks
1. Create `JwtTokenProvider` (generate, validate, parse)
2. Create `JwtAuthenticationFilter`
3. Create `UserDetailsService` implementation
4. Create `SecurityConfig` (filter chain, authorization rules)
5. Create `AuthController` (register, login, refresh, me)
6. Create `UserFactory` for role-specific user creation
7. Create DTOs (`LoginRequest`, `RegisterRequest`, `AuthResponse`, `UserDTO`)
8. Write unit tests for JWT and auth services

### Deliverables
- Working registration and login
- JWT token generation and validation
- Role-based endpoint protection
- Auth unit tests passing

### Verification
- Can register a new user
- Can login and receive JWT
- Protected endpoints reject unauthorized requests
- Role-based access works correctly
- All tests pass

## Phase 4: Core CRUD APIs (3-4 days)
### Objectives
- Implement Pet CRUD with search/filter
- Implement Product CRUD with categories
- Implement Service CRUD
- Global exception handling
- Pagination support

### Tasks
1. Create `GlobalExceptionHandler`
2. Create Pet DTOs, `PetController`, `PetService`, `PetRepository`
3. Implement pet search with JPA Specifications
4. Create Product DTOs, `ProductController`, `ProductService`, `ProductRepository`
5. Create ProductCategory CRUD
6. Create Service DTOs, `ServiceController`, `ServiceService`
7. Create Species/Breed endpoints
8. Write unit tests for services
9. Write integration tests for controllers

### Deliverables
- Full Pet CRUD with search/filter
- Full Product CRUD with categories
- Service management
- Error handling
- Unit + integration tests

### Verification
- All CRUD operations work via REST
- Search/filter returns correct results
- Error responses are properly formatted
- Tests pass

## Phase 5: Shopping Cart & Orders (3-4 days)
### Objectives
- Implement shopping cart
- Implement checkout and order creation
- Implement Order State Pattern
- Order status management

### Tasks
1. Create `Cart`, `CartItem` entities and DTOs
2. Create `CartController`, `CartService`
3. Create `OrderController`, `OrderService`
4. Implement Order state machine (State Pattern)
5. Create order status update logic with validation
6. Implement stock deduction on order confirmation
7. Integration with inventory
8. Write tests

### Deliverables
- Working cart system
- Order creation from cart
- State machine for order status
- Inventory integration
- Tests passing

### Verification
- Can add/remove items to cart
- Can checkout and create order
- Order status transitions validated
- Invalid transitions rejected
- Stock correctly deducted

## Phase 6: Appointment & Service Booking (2-3 days)
### Objectives
- Implement appointment booking
- Time slot management
- Appointment status management

### Tasks
1. Create `AppointmentController`, `AppointmentService`
2. Implement available time slot logic
3. Implement appointment status management
4. Conflict detection (double booking prevention)
5. Write tests

### Deliverables
- Appointment booking system
- Time slot availability
- Status management
- Tests

### Verification
- Can book appointments
- Double bookings prevented
- Status updates work
- Tests pass

## Phase 7: Veterinary & Health Records (2-3 days)
### Objectives
- Implement medical records
- Implement vaccination tracking
- Implement treatment tracking
- Implement Pet Health State Pattern

### Tasks
1. Create MedicalRecord CRUD
2. Create Vaccination CRUD
3. Create Treatment CRUD
4. Implement Pet Health state machine (State Pattern)
5. Health status transitions with validation
6. Write tests

### Deliverables
- Complete medical records system
- Vaccination tracking
- Health state machine
- Tests

## Phase 8: Inventory Management (2 days)
### Objectives
- Implement inventory tracking
- Stock in/out operations
- Low-stock alerts
- Transaction history

### Tasks
1. Create `InventoryController`, `InventoryService`
2. Implement stock-in, stock-out operations
3. Low-stock detection and alerting
4. Transaction history with audit trail
5. Write tests

### Deliverables
- Inventory management system
- Low-stock alerts
- Transaction history
- Tests

## Phase 9: Pet Matching (Strategy Pattern) (2-3 days)
### Objectives
- Implement matching quiz
- Implement scoring strategies
- Implement result ranking with explanations

### Tasks
1. Create `MatchingStrategy` interface and implementations
2. Create `PetMatchingService` with weighted aggregation
3. Create quiz submission and result endpoints
4. Implement score explanations
5. Write tests for each strategy

### Deliverables
- Pet matching quiz system
- Transparent scoring
- Result explanations
- Tests

## Phase 10: Loyalty & Promotions (2 days)
### Objectives
- Implement loyalty points system
- Membership tiers
- Promotion management

### Tasks
1. Create `LoyaltyService`
2. Integrate points earning into order and service flows
3. Implement tier calculation
4. Create `PromotionController`, `PromotionService`
5. Promotion validation and application
6. Write tests

### Deliverables
- Loyalty system
- Tier management
- Promotion system
- Tests

## Phase 11: Reviews & Notifications (Observer Pattern) (2-3 days)
### Objectives
- Implement review system
- Implement Observer pattern with Spring events
- WebSocket notifications

### Tasks
1. Create `ReviewController`, `ReviewService`
2. Create custom `ApplicationEvent` classes
3. Create `NotificationEventListener`
4. Configure WebSocket with STOMP
5. Create `WebSocketNotificationService`
6. Integrate events into existing services
7. Write tests

### Deliverables
- Review system
- Event-driven notifications
- WebSocket real-time updates
- Tests

## Phase 12: Admin Dashboard & Reports (2 days)
### Objectives
- Admin management APIs
- Revenue reports
- Inventory reports
- Dashboard data

### Tasks
1. Create `AdminController`
2. Implement user management
3. Implement revenue report queries
4. Implement inventory report queries
5. Dashboard summary endpoint
6. System configuration management
7. Write tests

### Deliverables
- Admin APIs
- Reports
- Dashboard data
- Tests

## Phase 13: React Frontend — Foundation (3-4 days)
### Objectives
- Set up routing
- Authentication UI (login, register)
- Layout components (header, sidebar, footer)
- API client configuration
- Auth context and protected routes

### Tasks
1. Configure React Router with role-based routes
2. Create auth pages (Login, Register)
3. Create layout components
4. Set up Axios with JWT interceptor
5. Create `AuthContext` with login/logout/token management
6. Create protected route wrapper
7. Responsive navigation

## Phase 14: React Frontend — Customer Pages (4-5 days)
### Objectives
- Pet browsing and detail
- Product browsing and detail
- Shopping cart
- Checkout
- Order history and tracking
- Service booking
- Pet profiles
- Pet matching quiz
- Loyalty dashboard
- Notifications

## Phase 15: React Frontend — Staff & Admin Pages (3-4 days)
### Objectives
- Sales staff order management
- Warehouse inventory management
- Veterinarian medical records
- Admin dashboard and management

## Phase 16: WebSocket Integration (2 days)
### Objectives
- Connect frontend to WebSocket
- Real-time notification display
- Toast notifications
- Notification bell with unread count

## Phase 17: Testing (3-4 days)
### Objectives
- Unit tests (JUnit + Mockito)
- Integration tests
- Playwright E2E tests
- Test coverage report

## Phase 18: Security Review & Polish (2-3 days)
### Objectives
- Security audit
- Input validation review
- UI/UX polish
- Performance optimization
- Documentation update

## Summary Timeline

| Phase | Description | Estimated Duration |
|-------|------------|-------------------|
| 1 | Project Setup & Foundation | 2-3 days |
| 2 | Domain Model & Database | 2-3 days |
| 3 | Authentication & Authorization | 2-3 days |
| 4 | Core CRUD APIs | 3-4 days |
| 5 | Shopping Cart & Orders | 3-4 days |
| 6 | Appointment & Service Booking | 2-3 days |
| 7 | Veterinary & Health Records | 2-3 days |
| 8 | Inventory Management | 2 days |
| 9 | Pet Matching (Strategy Pattern) | 2-3 days |
| 10 | Loyalty & Promotions | 2 days |
| 11 | Reviews & Notifications (Observer Pattern)| 2-3 days |
| 12 | Admin Dashboard & Reports | 2 days |
| 13 | React Frontend — Foundation | 3-4 days |
| 14 | React Frontend — Customer Pages | 4-5 days |
| 15 | React Frontend — Staff & Admin Pages | 3-4 days |
| 16 | WebSocket Integration | 2 days |
| 17 | Testing | 3-4 days |
| 18 | Security Review & Polish | 2-3 days |
| **Total** | | **~45-55 days** |

## Risk Register

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Scope Creep due to excessive frontend features | High | Medium | Strictly adhere to the MVP features defined in the roadmap; defer nice-to-have features to post-launch phases. |
| Integration issues with WebSockets | Medium | Medium | Develop small proof of concepts early (Phase 11) to validate STOMP and WebSocket configurations before full rollout. |
| Complex state management in React | Medium | High | Utilize robust state management tools (e.g. Redux Toolkit or React Query) instead of relying solely on Context API for complex states. |
| Delays in backend API completion blocking frontend | High | Medium | Define clear API contracts (e.g., Swagger/OpenAPI) early on so frontend can use mock servers until real APIs are ready. |
| Database schema changes mid-development | Medium | High | Use Flyway or Liquibase from the beginning to manage migrations, and solidify domain model thoroughly in Phase 2. |
