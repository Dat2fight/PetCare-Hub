# PetCare Hub

A professional pet shop management platform that transforms traditional pet sales and care into a data-driven, responsible, and accessible ecosystem.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 17+, Spring Boot 3.3, Spring MVC, Spring Data JPA, Spring Security, WebSocket |
| **Frontend** | React 18, TypeScript, Tailwind CSS, Vite |
| **Database** | SQL Server (MSSQL) |
| **Testing** | JUnit 5, Mockito, Playwright |

## Project Structure

```
ppet/
├── backend/          # Spring Boot application
│   └── src/
│       ├── main/java/com/petcarehub/
│       └── main/resources/
├── frontend/         # React SPA
│   └── src/
│       ├── api/
│       ├── components/
│       ├── contexts/
│       ├── hooks/
│       ├── pages/
│       ├── services/
│       ├── types/
│       └── utils/
└── docs/             # Project documentation
    ├── requirements.md
    ├── architecture.md
    ├── database.md
    ├── api.md
    ├── design-patterns.md
    └── development-roadmap.md
```

## Prerequisites

- Java 17 or higher
- Maven 3.9+
- Node.js 18+ and npm
- SQL Server 2016+ (or SQL Server Express)

## Getting Started

### Database Setup

1. Open SQL Server Management Studio (SSMS) or Azure Data Studio.
2. Connect to your SQL Server instance (ensure TCP/IP is enabled on port 1433).
3. Create a new database named `petcarehub`:
   ```sql
   CREATE DATABASE petcarehub;
   ```
4. Open `backend/src/main/resources/application.yml` and `application-dev.yml`. Update the `password` field for the `sa` user to match your actual SQL Server `sa` password.

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173`.

## Documentation

See the [docs/](docs/) directory for:
- [Requirements Specification](docs/requirements.md)
- [System Architecture](docs/architecture.md)
- [Database Schema](docs/database.md)
- [REST API Specification](docs/api.md)
- [Design Patterns](docs/design-patterns.md)
- [Development Roadmap](docs/development-roadmap.md)

## License

This project is for educational purposes.
