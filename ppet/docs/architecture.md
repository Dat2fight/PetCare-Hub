# PetCare Hub - System Architecture

## 1. System Architecture Overview
The PetCare Hub project follows a clean layered architecture pattern:
`Controller → Service → Repository → Database`

### Technology Stack
- **Backend:** Java 17+, Spring Boot 3.x, Spring MVC, Spring Data JPA, Spring Security, WebSocket, Bean Validation
- **Frontend:** React 18+, TypeScript, Tailwind CSS
- **Database:** PostgreSQL 15+
- **Testing:** JUnit 5, Mockito, Playwright
- **Build:** Maven (backend), Vite (frontend)
- **Version Control:** Git

## 2. High-Level Architecture Diagram
```mermaid
flowchart TD
    %% Frontend Layer
    subgraph Frontend [React SPA Frontend]
        UI[React Components]
        State[State Management / Contexts]
        Client[API Client / Axios]
        WSClient[WebSocket Client]
        
        UI --> State
        State --> Client
        State --> WSClient
    end

    %% Backend Layer
    subgraph Backend [Spring Boot Backend]
        Filter[Spring Security Filter Chain]
        WSConfig[WebSocket Endpoint / STOMP]
        REST[REST Controllers]
        Service[Service Layer]
        Repo[Spring Data JPA Repositories]
        
        Filter --> REST
        WSConfig --> Service
        REST --> Service
        Service --> Repo
    end

    %% Database Layer
    subgraph DB [Database Layer]
        Postgres[(PostgreSQL)]
    end

    %% Connections
    Client -->|HTTPS / REST API| Filter
    WSClient <-->|WSS / STOMP| WSConfig
    Repo -->|JDBC / Hibernate| Postgres
```

## 3. Backend Package Structure
```text
com.petcarehub
├── config/           (Security, WebSocket, CORS, etc.)
├── controller/       (REST controllers)
├── dto/              (Request/Response DTOs)
├── entity/           (JPA entities)
├── enums/            (State enums, role enums)
├── exception/        (Custom exceptions, global handler)
├── factory/          (Factory pattern implementations)
├── mapper/           (Entity ↔ DTO mappers)
├── observer/         (Observer pattern: events, listeners)
├── repository/       (Spring Data JPA repositories)
├── security/         (JWT, filters, user details)
├── service/          (Business logic)
│   ├── impl/
│   └── strategy/     (Strategy pattern: matching algorithms)
├── state/            (State pattern: health states, order states)
├── util/             (Utilities)
└── websocket/        (WebSocket handlers, config)
```

## 4. Frontend Structure
```text
src/
├── api/              (API client, axios config)
├── components/       (Reusable UI components)
│   ├── common/
│   ├── layout/
│   └── ui/
├── contexts/         (React contexts: Auth, Cart, Notification)
├── hooks/            (Custom hooks)
├── pages/            (Page components by role)
│   ├── customer/
│   ├── admin/
│   ├── sales/
│   ├── warehouse/
│   └── vet/
├── routes/           (Route definitions, guards)
├── services/         (WebSocket service, etc.)
├── store/            (State management)
├── types/            (TypeScript interfaces)
└── utils/            (Helpers)
```

## 5. Security Architecture
The system employs a robust security model to protect user data and ensure proper access control.

- **JWT-based stateless authentication:** No server-side session state is maintained.
- **Role-based access control (RBAC):** Administered via Spring Security to enforce permissions per user role.
- **Password Hashing:** Utilizes BCrypt for secure password storage.
- **CORS Configuration:** Configured to allow cross-origin requests specifically from the configured frontend domain.
- **CSRF Protection:** Adjusted for the Single Page Application (SPA) architecture, typically relying on stateless tokens.
- **Method-Level Security:** Business layer methods are secured using `@PreAuthorize` annotations.
- **Token Refresh Strategy:** Supports short-lived access tokens and longer-lived refresh tokens.

### Authentication Flow
```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant SecurityFilter as Spring Security
    participant AuthController
    participant DB

    User->>Frontend: Enter credentials
    Frontend->>SecurityFilter: POST /api/auth/login
    SecurityFilter->>AuthController: Forward request
    AuthController->>DB: Validate credentials
    DB-->>AuthController: User details
    AuthController-->>Frontend: Returns Access & Refresh JWTs
    
    Note over Frontend,SecurityFilter: Subsequent Requests
    
    Frontend->>SecurityFilter: GET /api/protected (Bearer AccessToken)
    SecurityFilter->>SecurityFilter: Validate Token
    SecurityFilter->>REST API: Allow request
    
    Note over Frontend,SecurityFilter: Token Expiry Flow
    
    Frontend->>SecurityFilter: GET /api/protected (Expired Token)
    SecurityFilter-->>Frontend: 401 Unauthorized
    Frontend->>AuthController: POST /api/auth/refresh (RefreshToken)
    AuthController-->>Frontend: Returns New Access Token
    Frontend->>SecurityFilter: Retry GET /api/protected (New Token)
```

## 6. WebSocket Architecture
Real-time communication is handled through WebSockets to provide instantaneous feedback and notifications.

- **Protocol:** STOMP over WebSocket with SockJS fallback for wider browser support.
- **User-Specific Channels:** Addressed to `/user/{userId}/notifications` for private alerts.
- **Topic Channels:** Used for role-based broadcasts, e.g., `/topic/warehouse/low-stock`, `/topic/sales/new-orders`.
- **Authentication:** JWTs are intercepted and validated during the initial WebSocket handshake.

### WebSocket Notification Flow
```mermaid
sequenceDiagram
    participant Client
    participant WS Interceptor
    participant WS Config
    participant Service Layer
    participant Notification Broker

    Client->>WS Interceptor: Connect (Send JWT in Header/Param)
    WS Interceptor->>WS Interceptor: Validate JWT
    WS Interceptor-->>WS Config: Accept Connection
    WS Config-->>Client: Connected
    Client->>WS Config: SUBSCRIBE /user/{userId}/queue/notifications
    
    Note over Service Layer: System Event Occurs
    Service Layer->>Notification Broker: Send Message to User
    Notification Broker-->>Client: Deliver Real-Time Notification
```

## 7. Class Diagram (Core Domain Model)
```mermaid
classDiagram
    class User {
        <<abstract>>
        +Long id
        +String email
        +String passwordHash
        +Role role
        +login()
    }
    class Customer {
        +String firstName
        +String lastName
        +String phoneNumber
    }
    class Staff {
        +String employeeId
        +Department department
    }
    
    User <|-- Customer
    User <|-- Staff
    
    class Pet {
        +Long id
        +String name
        +Date birthDate
        +Species species
        +Breed breed
    }
    
    Customer "1" -- "*" Pet : owns
    
    class Product {
        +Long id
        +String name
        +String description
        +BigDecimal price
    }
    class ProductCategory {
        +Long id
        +String name
    }
    
    Product "*" -- "1" ProductCategory : belongs_to
    
    class Order {
        +Long id
        +Date orderDate
        +OrderStatus status
        +BigDecimal totalAmount
        +calculateTotal()
    }
    class OrderItem {
        +Long id
        +Integer quantity
        +BigDecimal unitPrice
    }
    
    Order *-- "*" OrderItem : contains
    OrderItem "*" -- "1" Product : references
    Customer "1" -- "*" Order : places
    
    class Appointment {
        +Long id
        +Date appointmentTime
        +AppointmentStatus status
    }
    class Service {
        +Long id
        +String name
        +BigDecimal price
    }
    
    Appointment "*" -- "1" Pet : for
    Appointment "*" -- "1" Service : includes
    Appointment "*" -- "1" Staff : assigned_to
    
    class MedicalRecord {
        +Long id
        +Date date
        +String diagnosis
        +String treatment
    }
    class Vaccination {
        +Long id
        +String vaccineName
        +Date dateAdministered
        +Date nextDueDate
    }
    
    Pet "1" *-- "*" MedicalRecord : has
    Pet "1" *-- "*" Vaccination : has
    MedicalRecord "*" -- "1" Staff : recorded_by
    
    class LoyaltyAccount {
        +Long id
        +Integer points
        +addPoints()
        +redeemPoints()
    }
    class LoyaltyTransaction {
        +Long id
        +Integer pointsChange
        +Date date
        +String reason
    }
    
    Customer "1" -- "1" LoyaltyAccount : has
    LoyaltyAccount "1" *-- "*" LoyaltyTransaction : tracks
    
    class Notification {
        +Long id
        +String message
        +Boolean read
        +Date createdAt
    }
    User "1" *-- "*" Notification : receives
    
    class Review {
        +Long id
        +Integer rating
        +String comment
    }
    Customer "1" -- "*" Review : writes
    Product "1" -- "*" Review : has
    
    class Promotion {
        +Long id
        +String code
        +Double discountPercent
        +Date validUntil
    }
    
    class Inventory {
        +Long id
        +Integer currentStock
        +Integer reorderLevel
    }
    class InventoryTransaction {
        +Long id
        +Integer quantityChanged
        +TransactionType type
        +Date date
    }
    
    Product "1" -- "1" Inventory : tracked_in
    Inventory "1" *-- "*" InventoryTransaction : logs
```

## 8. Component Interaction Diagram
**Scenario: Customer places order**

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    participant Frontend
    participant OrderController
    participant OrderService
    participant InventoryService
    participant OrderRepo as OrderRepository
    participant LoyaltyService
    participant NotifService as NotificationService
    participant Broker as WebSocket Broker

    Customer->>Frontend: Clicks "Checkout"
    Frontend->>OrderController: POST /api/orders
    OrderController->>OrderService: createOrder(orderDto)
    
    OrderService->>InventoryService: checkStockAndReserve(items)
    alt Out of Stock
        InventoryService-->>OrderService: throw InsufficientStockException
        OrderService-->>OrderController: exception propagated
        OrderController-->>Frontend: 400 Bad Request (Stock Error)
    else Stock Available
        InventoryService-->>OrderService: stock reserved
        OrderService->>OrderRepo: save(order)
        OrderRepo-->>OrderService: savedOrder
        OrderService->>InventoryService: commitStockDeduction(items)
        
        OrderService->>LoyaltyService: calculateAndAddPoints(customer, amount)
        
        OrderService->>NotifService: sendOrderConfirmation(customer)
        NotifService->>Broker: publish to /user/{id}/notifications
        Broker-->>Frontend: Real-time update via WS
        
        OrderService-->>OrderController: OrderResponseDTO
        OrderController-->>Frontend: 201 Created
        Frontend-->>Customer: Order Success Page
    end
```

## 9. Cross-Cutting Concerns
- **Exception Handling:** Centralized through a `GlobalExceptionHandler` utilizing `@ControllerAdvice` to map exceptions to standardized API error responses.
- **Logging Strategy:** Unified logging setup utilizing SLF4J and Logback to trace requests and record application events.
- **Validation:** Utilizes Java Bean Validation (`@Valid`, `@NotNull`, etc.) alongside custom validators to enforce business rules at the entry point.
- **Pagination & Sorting:** Built using Spring Data's `Pageable` interface to ensure efficient data retrieval and consistent API responses.
- **Audit Fields:** Automatic timestamping (`createdAt`, `updatedAt`) and tracking (`createdBy`) managed via JPA Auditing (`@CreatedDate`, `@LastModifiedDate`).
- **API Versioning:** Controlled via URI path routing (e.g., `/api/v1/...`) to maintain backward compatibility.

## 10. Deployment Consideration
- **Development Environment:** Embedded Tomcat, H2 in-memory database for testing, and PostgreSQL for active development.
- **Containerization:** Comprehensive Docker support with `Dockerfile`s defined for both the Java backend and the React frontend.
- **Configuration Management:** Environment-specific settings isolated in `application-dev.yml` and `application-prod.yml` to support different deployment profiles.
