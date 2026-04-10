# 🌱 Plantopolis — Backend

API REST para la tienda de plantas Plantopolis, desarrollada con arquitectura hexagonal.

## Descripción

Plantopolis es una plataforma de e-commerce especializada en la venta de plantas de interior y exterior. Este repositorio contiene el backend que gestiona usuarios, catálogo de productos, carrito de compras, pedidos y pagos.

## Objetivo

Proveer una API robusta y segura que soporte el flujo completo de compra de plantas: desde el registro del usuario hasta la confirmación del pedido con notificación por email.

## Tecnologías utilizadas

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 17 | Lenguaje principal |
| Spring Boot | 3.5.x | Framework backend |
| Spring Security | 6.x | Autenticación y autorización |
| JWT (jjwt) | 0.12.6 | Tokens de sesión |
| Oracle Database XE | 21.3 | Base de datos |
| Hibernate / JPA | 6.x | ORM |
| JavaMail | - | Notificaciones por email |
| Springdoc OpenAPI | 2.3.0 | Documentación Swagger |
| Lombok | - | Reducción de código boilerplate |
| Docker | - | Contenedorización |
| Maven | 3.9 | Gestión de dependencias |

## Arquitectura

El proyecto implementa **Arquitectura Hexagonal (Ports & Adapters)**:

```
src/main/java/com/plantopolis/backend/
├── domain/                     ← Núcleo del negocio
│   ├── model/                  ← Entidades de dominio
│   └── port/
│       ├── in/                 ← Casos de uso (interfaces)
│       └── out/                ← Puertos de salida (interfaces)
├── application/
│   └── service/                ← Implementación de casos de uso
└── infrastructure/
    ├── adapter/
│   │   ├── in/web/             ← Controllers REST + DTOs
│   │   └── out/
│   │       ├── persistence/    ← Adaptadores JPA
│   │       └── notification/   ← Adaptador de email
    ├── config/                 ← Configuración Spring
    ├── persistence/
    │   ├── entity/             ← Entidades JPA
    │   ├── mapper/             ← Conversores domain ↔ entity
    │   └── repository/         ← Repositorios JPA
    └── security/               ← JWT y Spring Security
```

## Instalación y ejecución

### Opción 1 — Docker (recomendado)

**Prerrequisitos:**
- Docker Desktop instalado
- Contenedor Oracle XE corriendo

```bash
# 1. Crear red Docker (solo la primera vez)
docker network create plantopolis-net
docker network connect plantopolis-net oracle-xe

# 2. Configurar variables de entorno
cp .env.example .env
# Editar .env con tus credenciales reales

# 3. Construir y levantar
docker-compose up --build

# 4. Verificar
docker ps
```

### Opción 2 — Local con Maven

**Prerrequisitos:**
- Java 17
- Maven 3.9+
- Oracle XE corriendo en localhost:1521

```bash
# Configurar application.yml con tus datos de BD
# Luego ejecutar:
./mvnw spring-boot:run
```

## Uso — Documentación API

Con el servidor corriendo, accede a:

```
http://localhost:8080/swagger-ui.html
```

### Flujo de uso básico

```
1. POST /api/auth/registro     → Crear cuenta
2. POST /api/auth/login        → Obtener token JWT
3. GET  /api/productos         → Ver catálogo (público)
4. POST /api/carrito/items     → Agregar al carrito 🔒
5. POST /api/pedidos/checkout  → Confirmar pedido 🔒
6. GET  /api/pedidos           → Ver mis pedidos 🔒
```

🔒 = Requiere token JWT en el header `Authorization: Bearer {token}`

## Variables de entorno

Copia `.env.example` como `.env` y completa con tus datos:

| Variable | Descripción |
|---|---|
| `DB_USERNAME` | Usuario de Oracle |
| `DB_PASSWORD` | Contraseña de Oracle |
| `JWT_SECRET` | Clave secreta para JWT (mínimo 256 bits) |
| `MAIL_USERNAME` | Email de Gmail para notificaciones |
| `MAIL_PASSWORD` | App Password de Gmail |

> ⚠️ **Nunca subas el archivo `.env` a GitHub**

## Sprints completados

| Sprint | Funcionalidad |
|---|---|
| Sprint 1 | Autenticación JWT, registro e inicio de sesión |
| Sprint 2 | Catálogo de productos con filtros y paginación |
| Sprint 3 | Carrito de compras (agregar, modificar, eliminar) |
| Sprint 4 | Pedidos, pagos, notificaciones email y Docker |

## Pruebas

```bash
# Ejecutar tests
./mvnw test

# Compilar sin tests
./mvnw package -DskipTests
```


---
*Proyecto académico — Ingeniería de Sistemas*