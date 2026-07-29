# Supermarket Meal Planner

Aplicación web independiente para crear, en fases posteriores, planes de
alimentación semanales basados en productos concretos del supermercado elegido,
objetivos nutricionales, presupuesto y aprovechamiento de paquetes.

> **Estado:** FASE 7 — Usuarios, autenticación y propiedad de datos.

La versión actual permite explorar 24 productos controlados, mantener plantillas
de comidas y generar planes de 1 a 14 días con 1 a 6 comidas diarias. El motor
usa objetivos, presupuesto, disponibilidad y restricciones para construir un
resultado reproducible, puntuarlo y explicar sus advertencias. Los planes se
pueden previsualizar, guardar como snapshot, consultar y archivar. Desde un plan
guardado se genera una lista de compra agregada que calcula envases enteros,
coste real de compra, sobrante y diferencia frente al presupuesto semanal.
Cada cuenta dispone de preferencias propias y solo puede consultar o modificar
sus planes, listas e historial. La edición parcial de FASE 6 continúa disponible
tras autenticarse.

## Avisos importantes

**Datos de demostración.** Los productos, precios y valores nutricionales
incluidos son ficticios y no representan información real ni actual.

Supermarket Meal Planner es un proyecto independiente y no está afiliado,
patrocinado ni respaldado por los supermercados mostrados en la plataforma. Los
nombres y marcas pertenecen a sus respectivos propietarios.

El precio y la disponibilidad pueden variar según la tienda, la ubicación y el
momento de la consulta.

## Tecnologías

- Backend: Java 21, Spring Boot 3.5.16, Maven Wrapper, Spring Security,
  Nimbus JWT, Argon2id, JPA, Validation, PostgreSQL, Flyway, Actuator y
  springdoc-openapi.
- Frontend: React 19, TypeScript, Vite 8, React Router, TanStack Query, React
  Hook Form, Zod y CSS Modules.
- Testing: JUnit 5, Mockito, Testcontainers, Vitest, React Testing Library y
  Playwright.
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
- [Plantillas de comidas y reglas de cálculo](docs/meal-templates.md)
- [Generación semanal, scoring y determinismo](docs/meal-plan-generation.md)
- [Listas de compra, paquetes y desperdicio](docs/shopping-lists.md)
- [Autenticación y propiedad de datos](docs/authentication-and-data-ownership.md)
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
- Node.js 22.22 o superior (requisito de React Router 8.3).
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

Antes de arrancar deben generarse tres secretos externos distintos. No se
incluyen valores predeterminados en el repositorio:

```bash
openssl rand -base64 48 # APP_AUTH_ACCESS_TOKEN_SECRET
openssl rand -base64 48 # APP_AUTH_REFRESH_TOKEN_SECRET
openssl rand -base64 48 # MEAL_PLAN_PREVIEW_HMAC_SECRET
```

Registro y login requieren obtener primero el token CSRF mediante
`GET /api/v1/auth/csrf`. Demo y ADMIN están deshabilitados por defecto.

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
GET /api/v1/categories?supermarketCode=MERCADONA
GET /api/v1/dietary-tags
GET /api/v1/allergens
GET /api/v1/products?supermarketCode=MERCADONA&page=0&size=12
GET /api/v1/products/{id}
GET /api/v1/products/{id}/price-history
GET /api/v1/meal-templates
GET /api/v1/meal-templates/{id}
POST /api/v1/meal-templates
PUT /api/v1/meal-templates/{id}
PATCH /api/v1/meal-templates/{id}/status
DELETE /api/v1/meal-templates/{id}
POST /api/v1/meal-templates/preview
POST /api/v1/meal-plans/generate
GET /api/v1/meal-plans
GET /api/v1/meal-plans/{id}
PATCH /api/v1/meal-plans/{id}/status
DELETE /api/v1/meal-plans/{id}
POST /api/v1/meal-plans/{id}/shopping-list
GET /api/v1/meal-plans/{id}/shopping-list
POST /api/v1/meal-plans/{id}/shopping-list/regenerate
PATCH /api/v1/meal-plans/{id}/shopping-list/status
DELETE /api/v1/meal-plans/{id}/shopping-list
GET /api/v1/shopping-lists
GET /api/v1/shopping-lists/{id}
GET /api/v1/shopping-lists/{id}/export?format=csv
GET /actuator/health
GET /v3/api-docs
GET /swagger-ui/index.html
```

Los errores propios se devuelven como `application/problem+json`.

## Datos mock

`data/mock/mercadona-catalog.json` contiene ocho categorías y 24 productos:

- carnes, huevos y lácteos;
- alternativas sin lactosa y vegetales;
- cereales, pasta convencional y sin gluten, y pan;
- legumbres y proteína vegetal;
- frutas, verduras y conservas.

El importador es idempotente. Actualiza por `(supermarket_id, external_id)` y
marca como no disponibles los productos que desaparecen, sin borrarlos. También
sincroniza etiquetas dietéticas, alérgenos con tipo de presencia e histórico de
precios sin duplicar registros. Dos productos carecen deliberadamente de ficha
nutricional para validar ese estado.

## Catálogo y filtros

La pantalla completa está disponible en <http://localhost:5173/products>. Cada
tarjeta abre `/products/{id}`.

```text
/products?supermarket=MERCADONA&query=pollo
/products?tags=HIGH_PROTEIN,LACTOSE_FREE&exclude=MILK,GLUTEN
GET /api/v1/products?minimumProtein=20&maximumCalories=250
GET /api/v1/products?available=true&maximumPrice=3&sort=currentPrice,asc
```

La búsqueda usa un debounce de 400 ms. Los filtros, ordenación y página se
reflejan en query parameters. Consulta
[Filtrado del catálogo](docs/catalog-filtering.md) para la semántica completa.

## Plantillas de comidas

La pantalla principal está en <http://localhost:5173/meal-templates>. Desde
ella se puede filtrar, ordenar y paginar, abrir el detalle o crear una plantilla.
El editor está disponible en `/meal-templates/new` y
`/meal-templates/{id}/edit`.

El selector de ingredientes consulta el catálogo de forma remota y limita los
resultados. La previsualización valida y calcula sin guardar. El coste mostrado
en las plantillas es el **coste proporcional consumido**. El coste de paquetes
completos se calcula después, en la lista de compra del plan.

El arranque importa de forma idempotente 16 plantillas controladas:
4 desayunos, 4 comidas, 3 meriendas y 5 cenas. Hay casos deliberadamente
incompletos para comprobar avisos de nutrición y precio.

## Planes semanales

- Listado: <http://localhost:5173/meal-plans>
- Generación: <http://localhost:5173/meal-plans/new>
- Detalle: `/meal-plans/{id}`

El formulario permite introducir directamente objetivos y restricciones. Primero
genera un preview con `persist=false`: no escribe en PostgreSQL y devuelve la
seed efectiva. Al pulsar **Guardar este plan**, el frontend repite la petición
con la misma seed y el `generationToken`; el backend solo persiste si los datos
de entrada siguen produciendo exactamente el mismo snapshot.

Para una comprobación rápida de API:

```bash
curl -X POST http://localhost:8081/api/v1/meal-plans/generate \
  -H "Content-Type: application/json" \
  --data @docs/examples/generate-meal-plan.json
```

El algoritmo y los pesos se explican en
[docs/meal-plan-generation.md](docs/meal-plan-generation.md).

La estrategia predeterminada desde la FASE 5 es `PURCHASE_AWARE_SCORING`.
Optimiza el coste real de envases, el desperdicio y la reutilización
económicamente útil sin dejar de puntuar nutrición y variedad. El modo
`SCORING` continúa disponible para reproducir el comportamiento clásico.
Cuando no se configura presupuesto, la normalización económica usa una
referencia configurable de 2,50 € por comida; no se trata de un precio real.
Consulta
[docs/purchase-aware-meal-plan-optimization.md](docs/purchase-aware-meal-plan-optimization.md).

## Listas de compra

- Listado: <http://localhost:5173/shopping-lists>
- Detalle: `/shopping-lists/{id}`
- Acceso desde el detalle de cualquier plan guardado.

La lista agrega cada producto en toda la semana antes de calcular paquetes. Por
ejemplo, 1.200 g requeridos de un envase de 500 g producen 3 paquetes, 1.500 g
comprados y 300 g de sobrante. El presupuesto se compara con el coste de esos
paquetes, mientras que el coste consumido del plan se conserva como métrica
separada.

Cada lista es un snapshot: mantiene nombres, marcas, categorías, formato,
precios y disponibilidad usados al generarla. Un plan antiguo que no contenga
esos datos sigue siendo legible, pero sus artículos no calculables aparecen con
valores nulos y avisos explícitos; nunca se inventan precios o formatos. La
regeneración reemplaza la lista activa dentro de una transacción y conserva la
anterior archivada. El detalle permite exportar CSV UTF-8 e imprimir.

Las fórmulas, redondeos y límites están en
[docs/shopping-lists.md](docs/shopping-lists.md).

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
npm run typecheck
npm run build
```

## Problemas conocidos y límites

- Todos los precios y datos nutricionales son de demostración.
- Solo Mercadona está habilitado; los demás proveedores son informativos.
- El modo clásico `SCORING` usa coste proporcional consumido. La estrategia
  predeterminada calcula durante el beam coste real, paquetes y desperdicio.
- Un plan guardado antes de la FASE 4 carece de snapshots de formato y precio;
  su lista se genera como parcial y explica qué artículos no puede calcular.
- Los planes persistidos admiten sustitución y regeneración parcial, bloqueos,
  preview antes/después, historial y undo del último cambio de contenido.
- `editVersion` protege todas las operaciones; `contentVersion` solo cambia
  cuando cambia el contenido y determina si la lista activa queda desactualizada.
- El filtrado de plantillas se calcula en memoria tras cargar el pequeño conjunto
  de demostración; se migrará a consulta SQL cuando el volumen lo justifique.
- No hay IA, scraping, Open Food Facts, Redis, Airflow funcional ni Kubernetes.
- El dashboard, la actividad y los listados usan proyecciones persistidas; no
  recalculan los planes.
- Los tres usuarios manuales y el plan privado de validación se conservan
  temporalmente y los E2E usan una base independiente y desechable.
- Los puertos estándar 8080 y 5432 no se usan por defecto para evitar colisiones
  con otros proyectos locales.
- `npm audit` no informa vulnerabilidades con el lockfile actual.

## Roadmap

La FASE 8 incorpora dashboard, actividad, organización, temas y duplicación
histórica. La siguiente fase es Airflow y no forma parte del runtime actual.

Consulta [docs/roadmap.md](docs/roadmap.md) para el orden completo.

## Seguridad de previews de edición

El backend exige `MEAL_PLAN_PREVIEW_HMAC_SECRET` con al menos 32 bytes. No hay
valor predeterminado ni secreto incluido en el repositorio. Para desarrollo:

```bash
openssl rand -base64 48
```

Guarda el resultado solo en tu `.env` local. Docker Compose falla antes de
arrancar el backend si falta. Los tokens expiran a los 15 minutos por defecto y
nunca deben escribirse completos en logs.
