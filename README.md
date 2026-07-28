# Supermarket Meal Planner

Aplicación web independiente para crear, en fases posteriores, planes de
alimentación semanales basados en productos concretos del supermercado elegido,
objetivos nutricionales, presupuesto y aprovechamiento de paquetes.

> **Estado:** FASE 0 — Fundación y arquitectura, con un vertical slice del
> catálogo de demostración.

La versión actual permite seleccionar un supermercado, consultar 12 productos
controlados, comprobar sus datos nutricionales y validar la conexión completa
React → Spring Boot → PostgreSQL.

## Avisos importantes

**Datos de demostración.** Los productos, precios y valores nutricionales
incluidos son ficticios y no representan información real ni actual.

Supermarket Meal Planner es un proyecto independiente y no está afiliado,
patrocinado ni respaldado por los supermercados mostrados en la plataforma. Los
nombres y marcas pertenecen a sus respectivos propietarios.

El precio y la disponibilidad pueden variar según la tienda, la ubicación y el
momento de la consulta.

## Tecnologías

- Backend: Java 21, Spring Boot 3.5.16, Maven Wrapper, JPA, Validation,
  PostgreSQL, Flyway, Actuator y springdoc-openapi.
- Frontend: React 19, TypeScript, Vite 8, React Router, TanStack Query, React
  Hook Form, Zod y CSS Modules.
- Testing: JUnit 5, Mockito, Testcontainers, Vitest y React Testing Library.
- Infraestructura: Docker, Docker Compose, Nginx y Makefile.
- ETL futuro: Python y Apache Airflow; todavía no forman parte del runtime.

## Arquitectura

El backend es un monolito modular. Los controladores consumen servicios de
aplicación y nunca exponen entidades JPA ni acceden directamente a repositorios.
El catálogo externo entra mediante `SupermarketCatalogProvider` y la nutrición
mediante `NutritionDataProvider`.

Mercadona es solo el primer proveedor seleccionable. No forma parte del nombre
del producto ni de las entidades principales.

Más detalle:

- [Arquitectura](docs/architecture.md)
- [Modelo de dominio](docs/domain-model.md)
- [Contrato API](docs/api.md)
- [Roadmap](docs/roadmap.md)
- [Decisiones arquitectónicas](docs/adr/)

## Requisitos

### Método recomendado

- Docker Desktop con contenedores Linux.
- Docker Compose v2 o posterior.
- Git.

Este método no requiere tener Java, Maven, Node ni PostgreSQL instalados.

### Desarrollo sin Docker

- Java 21.
- Node.js 20.19 o superior.
- PostgreSQL 17.
- Maven no es necesario: se incluye Maven Wrapper.

Para ejecutar el backend fuera de Docker deben definirse `DB_URL`, `DB_USER` y
`DB_PASSWORD`; el repositorio no incorpora credenciales runtime por defecto.

`make` es opcional. En Windows se recomienda usar los comandos `docker compose`
desde PowerShell. El Makefile está orientado a macOS, Linux, Git Bash y WSL.

## Inicio rápido con Docker

Desde la raíz:

```bash
copy .env.example .env
docker compose up --build
```

En macOS, Linux, Git Bash o WSL:

```bash
cp .env.example .env
docker compose up --build
```

Docker Compose espera a que PostgreSQL esté saludable, arranca el backend,
aplica Flyway, importa el catálogo JSON y finalmente publica el frontend.

Para detenerlo:

```bash
docker compose down
```

El volumen de PostgreSQL se conserva. Para eliminarlo de forma deliberada:

```bash
docker compose down --volumes
```

## URLs locales

| Recurso | URL |
| --- | --- |
| Frontend | <http://localhost:5173> |
| API | <http://localhost:8081/api/v1> |
| Swagger UI | <http://localhost:8081/swagger-ui/index.html> |
| OpenAPI JSON | <http://localhost:8081/v3/api-docs> |
| Actuator Health | <http://localhost:8081/actuator/health> |
| PostgreSQL | `localhost:5433` |

Los puertos se pueden cambiar en `.env`.

## Endpoints implementados

```text
GET /api/v1/supermarkets
GET /api/v1/products?supermarketCode=MERCADONA&page=0&size=20
GET /api/v1/products/{id}
GET /actuator/health
GET /v3/api-docs
GET /swagger-ui/index.html
```

Los errores propios se devuelven como `application/problem+json`.

## Datos mock

`data/mock/mercadona-catalog.json` contiene cinco categorías y doce productos:

- pechuga de pollo;
- hamburguesa de pavo;
- huevos;
- arroz;
- pasta integral;
- pan integral;
- yogur alto en proteína;
- leche sin lactosa;
- atún en conserva;
- espinacas;
- tomate;
- plátano.

El importador es idempotente. Actualiza por `(supermarket_id, external_id)` y
marca como no disponibles los productos que desaparecen, sin borrarlos.

## Comandos

```bash
make dev       # levanta toda la aplicación
make backend   # PostgreSQL y backend
make frontend  # frontend y sus dependencias
make test      # pruebas backend y frontend en contenedores
make build     # construye todas las imágenes
make down      # detiene contenedores, conserva datos
make clean     # elimina solo artefactos de build, conserva PostgreSQL
```

Equivalentes sin `make`:

```bash
docker compose up --build
docker compose --profile tools run --rm --build backend-test
docker compose --profile tools run --rm --build frontend-test
docker compose build
docker compose down --remove-orphans
```

Desarrollo frontend local:

```bash
cd frontend
npm ci
npm run dev
```

El proxy de Vite espera el backend en `http://localhost:8081`. Se puede cambiar
con `API_PROXY_TARGET`.

## Pruebas

El backend utiliza PostgreSQL real mediante Testcontainers. No existe una base
H2 que pueda ocultar diferencias con producción.

```bash
cd backend
./mvnw verify

cd ../frontend
npm ci
npm run format:check
npm run lint
npm run test
npm run build
```

## Problemas conocidos y límites

- Todos los precios y datos nutricionales son de demostración.
- Solo Mercadona está habilitado; los demás proveedores son informativos.
- No existe todavía generación de planes, lista de compra ni histórico de
  precios.
- No hay autenticación, IA, scraping, Open Food Facts, Redis, Airflow funcional
  ni Kubernetes.
- Los puertos estándar 8080 y 5432 no se usan por defecto para evitar colisiones
  con otros proyectos locales.
- `npm audit` informa de un advisory alto en el modo RSC/Actions de React Router.
  Esta aplicación es una SPA cliente y no usa RSC, acciones de servidor ni SSR;
  se mantiene la última versión estable y deberá actualizarse cuando upstream
  publique una corrección aplicable.

## Roadmap

El desarrollo continúa en fases pequeñas. La siguiente tarea recomendada es la
**FASE 1 — Catálogo mock**, ampliando búsqueda, filtros, histórico de precios y
persistencia de alérgenos/etiquetas sin iniciar todavía el planificador.

Consulta [docs/roadmap.md](docs/roadmap.md) para el orden completo.
