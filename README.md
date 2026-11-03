# Pokédex API

A REST backend for a Pokédex application, built with Spring Boot and MongoDB.

The API supports user registration/login with JWT, public browsing and search of Pokémon and trainers, trainer profile management (seen/caught Pokémon), and admin-only Pokémon catalog management.

> This README documents the **`feature/jwt`** branch. The `master` branch uses HTTP Basic auth and a reduced feature set.

## Tech stack

- Java 17
- Spring Boot 3.2
- Spring Security + JWT (`jjwt` 0.9.1)
- Spring Data MongoDB
- Maven

## Getting started

### Prerequisites

- JDK 17+
- A running MongoDB instance (see [Database setup](#database-setup))

### Run

```bash
./mvnw spring-boot:run
```

The API is served at **`http://localhost:8090/api`** (`server.port=8090`, context path `/api`).

### Build & test

```bash
./mvnw clean package
./mvnw test
```

## Database setup

MongoDB is schemaless here: the collections (`User`, `Pkmn`, `Trainer`) are created automatically on first write. No migrations are needed.

### Option 1: Docker (recommended)

```bash
docker run -d \
  --name pokedex-mongo \
  -p 27017:27017 \
  -v pokedex-mongo-data:/data/db \
  mongo:7
```

Stop/start it later with `docker stop pokedex-mongo` and `docker start pokedex-mongo`.

### Option 2: Local MongoDB

Install MongoDB Community Server from https://www.mongodb.com/try/download/community, then start it:

```bash
# systemd (most Linux distros)
sudo systemctl enable --now mongod

# or macOS with Homebrew
brew services start mongodb-community
```

### Connection settings

The app uses Spring Boot's default MongoDB URI: `mongodb://localhost:27017/test`.

To use another host, database, or credentials, add to `src/main/resources/application.properties`:

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/pokedex
```

With authentication:

```properties
spring.data.mongodb.uri=mongodb://user:password@localhost:27017/pokedex?authSource=admin
```

Or override it at runtime:

```bash
SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/pokedex ./mvnw spring-boot:run
```

### Verify / inspect

```bash
mongosh --eval "db.version()"
mongosh test --eval "db.Pkmn.countDocuments()"
```

If you use Docker, run `mongosh` inside the container instead:

```bash
docker exec -it pokedex-mongo mongosh test --eval "db.version()"
```

## Authentication

Authentication is **stateless JWT**. Send the token on protected requests:

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8090/api/users/me
```

### Tokens

- Obtained from `POST /users/register` and `POST /users/login`.
- Signed with `jwt.secret` (default `mysecret`) using HS256.
- Expire after `jwt.expiration` milliseconds (default `86400000`, i.e. 24h).
- The subject is the user's login. Roles are reloaded from the database on each request, so role changes take effect without a new token.

### Roles

| Role | Capabilities |
|------|--------------|
| `ROLE_USER` | Browse/search everything, manage own trainer and seen/caught Pokémon |
| `ROLE_ADMIN` | Everything a user can do, plus create/update/delete Pokémon and regions |

### Authorization rules

| Method | Path | Access |
|--------|------|--------|
| POST | `/users/register`, `/users/login`, `/users/logged` | Public |
| PUT | `/users/admin` | Public (dev helper — see [Create an admin](#create-an-admin)) |
| GET | `/users/me` | Authenticated |
| GET | `/pkmn/**`, `/pkmn` | Public |
| POST | `/pkmn/ids` | Public |
| POST/PUT/DELETE | `/pkmn/**`, `/pkmn` | Admin |
| GET | `/trainer/**`, `/trainer` | Public |
| POST/PUT/DELETE | `/trainer/**`, `/trainer` | Authenticated |

## Endpoints

Base URL: `http://localhost:8090/api`

### Users

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/users/register` | Public | Register a user and return a token |
| POST | `/users/login` | Public | Log in and return a token |
| GET | `/users/me` | Bearer | Current user's info |
| GET | `/users/logged` | Public | Returns `200 OK` (connectivity check) |
| PUT | `/users/admin` | Public | Promote/demote a user (dev helper) |

**POST `/users/register`** — `isAdmin` in the body is ignored; new users are always non-admin.

```json
{ "login": "ash", "password": "pikachu123" }
```

**POST `/users/login`**

```json
{ "login": "ash", "password": "pikachu123" }
```

Both return:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": { "login": "ash", "isAdmin": false }
}
```

**GET `/users/me`** returns `{ "login": "ash", "isAdmin": false }`.

**PUT `/users/admin`**

```json
{ "id": "ash", "isAdmin": true }
```

Returns the updated `{ login, isAdmin }`. See the security note in [Create an admin](#create-an-admin).

### Pokémon

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/pkmn?id={id}` or `/pkmn?name={name}` | Public | Get one Pokémon |
| GET | `/pkmn/search` | Public | Paginated search |
| GET | `/pkmn/types` | Public | All types with colors |
| POST | `/pkmn/ids` | Public | Batch fetch by ids |
| POST | `/pkmn` | Admin | Create a Pokémon |
| PUT | `/pkmn?id={id}` | Admin | Update a Pokémon |
| DELETE | `/pkmn?id={id}` or `/pkmn?name={name}` | Admin | Delete a Pokémon |
| POST | `/pkmn/region` | Admin | Add a region to a Pokémon |
| DELETE | `/pkmn/region?id={id}&regionName={name}` | Admin | Remove a region |
| POST | `/pkmn/api` | Admin | Import Pokémon from Tyradex |

**GET `/pkmn/search`** query params (all optional):

| Param | Default | Description |
|-------|---------|-------------|
| `partialName` | `""` | Case-insensitive name substring |
| `typeOne` | – | Type filter (e.g. `ELECTRIC`) |
| `typeTwo` | – | Second type filter |
| `page` | `0` | Page index (0-based) |
| `size` | `12` | Page size |

Returns a Spring `Page<PkmnData>` (`content`, `totalElements`, `totalPages`, `pageable`, ...).

**GET `/pkmn/types`**

```json
{
  "types": [{ "name": "ELECTRIC", "color": "#F8D030" }],
  "count": 18
}
```

Available types: `NORMAL`, `FIRE`, `FIGHTING`, `WATER`, `FLYING`, `GRASS`, `POISON`, `ELECTRIC`, `GROUND`, `PSYCHIC`, `ROCK`, `ICE`, `BUG`, `DRAGON`, `GHOST`, `DARK`, `STEEL`, `FAIRY`.

**POST `/pkmn/ids`**

```json
["65f1c2a9e4b0a1b2c3d4e5f6", "65f1c2a9e4b0a1b2c3d4e5f7"]
```

Unknown ids are skipped. Returns a list of Pokémon.

**POST `/pkmn`** (admin)

```json
{
  "name": "Pikachu",
  "description": "Electric mouse Pokémon",
  "types": ["ELECTRIC"],
  "regions": [{ "regionName": "Kanto", "regionPokedexNumber": "025" }],
  "imgUrl": "https://example.com/pikachu.png"
}
```

Returns `200 OK` with an empty body.

**PUT `/pkmn?id={id}`** (admin) query params (all optional): `name`, `description`, `imgUrl`, `typeOne`, `typeTwo`.

**POST `/pkmn/region`** (admin)

```json
{
  "name": "Pikachu",
  "region": { "regionName": "Johto", "regionPokedexNumber": "022" }
}
```

**POST `/pkmn/api`** (admin) imports Pokémon from `https://tyradex.vercel.app/api/v1/pokemon`. It can take a while and returns a dummy empty Pokémon object. Existing names are skipped.

### Trainer

Each user has at most one trainer, tied to their login.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/trainer?id={id}` or `/trainer?username={username}` | Public | Get one trainer |
| GET | `/trainer/search` | Public | Paginated trainer search |
| POST | `/trainer` | Bearer | Create the current user's trainer |
| PUT | `/trainer` | Bearer | Update the current user's trainer |
| DELETE | `/trainer` | Bearer | Delete the current user's trainer |
| POST | `/trainer/mark?pkmnId={id}&isCaught={bool}` | Bearer | Mark a Pokémon seen/caught |

**GET `/trainer/search`** query params (all optional):

| Param | Default | Description |
|-------|---------|-------------|
| `partialName` | `""` | Case-insensitive **trainerName** substring (not username) |
| `page` | `0` | Page index |
| `size` | `12` | Page size |

**POST `/trainer`**

```json
{ "trainerName": "Ash", "imgUrl": "https://example.com/ash.png" }
```

Returns `201 Created` with the trainer.

**PUT `/trainer`** query params (all optional): `imgUrl`, `trainerName`. Returns `200 OK`.

**POST `/trainer/mark`**

| Param | Required | Description |
|-------|----------|-------------|
| `pkmnId` | yes | Pokémon ObjectId |
| `isCaught` | yes | `true` = caught (also marked seen), `false` = seen only |

## Data models

### PkmnData

```json
{
  "id": "65f1c2a9e4b0a1b2c3d4e5f6",
  "name": "Pikachu",
  "description": "Electric mouse Pokémon",
  "types": ["ELECTRIC"],
  "regions": [{ "regionName": "Kanto", "regionPokedexNumber": "025" }],
  "imgUrl": "https://example.com/pikachu.png"
}
```

### TrainerData

```json
{
  "id": "65f1a2b9e4b0a1b2c3d4e5f6",
  "username": "ash",
  "trainerName": "Ash",
  "imgUrl": "https://example.com/ash.png",
  "pkmnSeen": ["<pkmnId>"],
  "pkmnCaught": ["<pkmnId>"],
  "createdDate": "2026-01-01T00:00:00.000+00:00"
}
```

## Create an admin

Registration always creates a non-admin user. To test admin features:

```bash
# 1. Register (or reuse an existing) user
curl -X POST http://localhost:8090/api/users/register \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","password":"admin123"}'

# 2. Promote the user
curl -X PUT http://localhost:8090/api/users/admin \
  -H 'Content-Type: application/json' \
  -d '{"id":"admin","isAdmin":true}'
```

Then log in again to get a fresh `isAdmin: true` in the response (useful for frontends that read it from the login payload).

> **Security note:** `PUT /users/admin` is publicly accessible in this branch. This is a development convenience and must be locked down before any real deployment.

## Configuration

`src/main/resources/application.properties`:

```properties
server.servlet.contextPath=/api
server.port=8090

jwt.secret=mysecret
jwt.expiration=86400000
```

- Change `jwt.secret` in any non-local environment.
- CORS is configured in `CorsConfig` for `http://localhost:3000` (the Next.js dev server).

## Frontend integration

The companion frontend lives in `../pokedex_front` and expects:

```bash
BASE_API_URL=http://localhost:8090/api
```

The frontend stores the JWT in cookies and sends it as `Authorization: Bearer <token>`.

## Error responses

Errors are returned as plain text with the matching HTTP status:

| Status | Cause |
|--------|-------|
| `401 Unauthorized` | Missing/invalid/expired JWT for a protected route |
| `403 Forbidden` | Insufficient role (e.g. non-admin doing Pokémon writes) |
| `404 Not Found` | Pokémon, region, trainer, or user not found |
| `409 Conflict` | Pokémon, region, trainer, or user already exists |

## Project structure

```
src/main/java/fr/iut/bc/pkdxapi/
├── configurations/   # Security, JWT filter wiring, CORS
├── controllers/      # REST endpoints
├── errors/           # Custom exceptions mapped to HTTP statuses
├── filters/          # JWT request filter
├── models/           # Documents, DTOs, requests/responses
├── repositories/     # Spring Data MongoDB repositories
├── services/         # Business logic + auth entry point
└── utils/            # JWT generation/validation
```
