# Galería Pexels con Room

Aplicación Android que integra la API de Pexels con persistencia local mediante Room Database. Implementa cache por query, soporte offline básico, favoritos persistentes e historial de búsquedas.

**Autor:** Diego Andre Calderón Salazar  
**Carné:** 241263  
**Institución:** Universidad del Valle de Guatemala  
**Curso:** Plataformas Móviles - Laboratorio 8

---

## Tecnologías

- Kotlin 2.0.21
- Jetpack Compose (Material 3)
- Room 2.6.1
- Retrofit 2.9.0 + Moshi
- Coil 2.5.0
- Navigation Compose
- Coroutines + Flow

---

## Funcionalidades Implementadas

### Core
- Búsqueda de fotos con debounce de 500ms
- Paginación infinita
- Cache local por query y página
- Modo offline básico
- Favoritos persistentes
- Historial de búsquedas recientes (últimas 10)
- Tema claro/oscuro

### Pantallas
- **Home:** Lista en grid con búsqueda y scroll infinito
- **Details:** Vista ampliada con información de la foto
- **Profile:** Gestión de favoritos, historial y configuración

---

## Arquitectura

### Estructura de Capas

```
Presentation → Repository → Data Sources (Remote API + Local DB)
```

### Estrategia de Cache

**Cache-First con Network Fallback:**
1. Consulta base de datos local
2. Si existe data, retorna inmediatamente
3. En paralelo, actualiza desde API
4. Persiste resultados en Room

**Invalidación:**
- Automática: Datos mayores a 24 horas se eliminan
- Manual: Opción de limpiar cache en perfil

---

## Esquema de Base de Datos

### Tabla: photos

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | INTEGER (PK) | Identificador único |
| width, height | INTEGER | Dimensiones en píxeles |
| photographer | TEXT | Autor de la foto |
| url | TEXT | URL de Pexels |
| originalUrl, largeUrl, mediumUrl, smallUrl | TEXT | URLs de distintas resoluciones |
| queryKey | TEXT (indexed) | Query normalizada |
| pageIndex | INTEGER (indexed) | Número de página |
| isFavorite | BOOLEAN (indexed) | Estado de favorito |
| updatedAt | LONG | Timestamp de última actualización |

### Tabla: recent_searches

| Campo | Tipo | Descripción |
|-------|------|-------------|
| query | TEXT (PK) | Query normalizada |
| lastUsedAt | LONG | Timestamp de último uso |

### Índices
- `(queryKey, pageIndex)` para búsquedas eficientes
- `isFavorite` para filtrado rápido

---

## Instalación

### Requisitos
- Android Studio Narwhal 4+ (2025.1.4)
- JDK 21
- Android SDK 26+
- API Key de Pexels

---

## Consideraciones de Implementación

### Manejo de Estado
Se utilizan primitivas de Compose (`remember`, `rememberSaveable`, `collectAsState`) en lugar de ViewModels para simplificar el código. Apropiado para el alcance del proyecto pero no escalable para aplicaciones grandes.

### Offline
El modo offline es básico: solo muestra datos previamente cacheados. No hay sincronización bidireccional ni resolución de conflictos. Para búsquedas no previamente realizadas, se muestra mensaje de error con opción de ver última búsqueda cacheada.

### Performance
- Índices en Room optimizan consultas frecuentes
- Coil maneja cache de imágenes automáticamente
- LazyStaggeredGrid previene cargas innecesarias
- Debounce reduce llamadas a API

---

## Testing

### Pruebas Manuales Realizadas

**Cache Funcional**
- Realizar búsqueda con internet
- Navegar fuera y regresar
- Verificar carga instantánea sin parpadeos

**Offline Básico**
- Realizar búsqueda
- Activar modo avión
- Reiniciar app
- Verificar que se muestren fotos cacheadas

**Favoritos Persistentes**
- Marcar fotos como favoritas
- Cerrar app
- Reabrir
- Verificar persistencia en tab de favoritos

**Búsquedas Recientes**
- Realizar múltiples búsquedas
- Verificar orden cronológico inverso
- Confirmar límite de 10 entradas

---

## Estructura del Proyecto

```
app/src/main/java/com/diegocal/laboratorio6/
├── database/
│   ├── AppDatabase.kt
│   ├── entities/
│   │   ├── PhotoEntity.kt
│   │   └── RecentSearchEntity.kt
│   └── dao/
│       ├── PhotoDao.kt
│       └── RecentSearchDao.kt
├── repository/
│   └── PhotoRepository.kt
├── ui/
│   ├── theme/
│   └── views/
│       ├── PexelsScreen.kt
│       ├── DetailsScreen.kt
│       ├── ProfileScreen.kt
│       └── PhotoCardWithFavorite.kt
├── MainActivity.kt
├── PexelsApiServices.kt
├── PexelsModels.kt
└── RetrofitPexels.kt
```


Diego Andre Calderón Salazar  
Carné: 241263  
Universidad del Valle de Guatemala
