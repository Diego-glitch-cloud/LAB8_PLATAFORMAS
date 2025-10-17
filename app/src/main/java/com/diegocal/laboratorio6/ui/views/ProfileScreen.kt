package com.diegocal.laboratorio6.ui.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.diegocal.laboratorio6.RetrofitPexels
import com.diegocal.laboratorio6.database.AppDatabase
import com.diegocal.laboratorio6.repository.PhotoRepository
import com.diegocal.laboratorio6.ui.theme.ThemeState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    themeState: ThemeState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Inicializar repositorio
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember {
        PhotoRepository(
            photoDao = database.photoDao(),
            recentSearchDao = database.recentSearchDao(),
            apiService = RetrofitPexels.retrofitService
        )
    }

    // Estado
    var selectedTab by remember { mutableStateOf(0) }
    val favorites by repository.getFavoritePhotos().collectAsState(initial = emptyList())
    val recentSearches by repository.getRecentSearches().collectAsState(initial = emptyList())

    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearSearchesDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Información de usuario (mock)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Usuario de Pexels",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Diego Andre",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Configuraciones
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Modo oscuro
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Modo oscuro",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Cambiar tema de la aplicación",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = themeState.isDark.value,
                            onCheckedChange = { themeState.toggleTheme() }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Limpiar caché
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Limpiar caché",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Elimina fotos almacenadas (mantiene favoritos)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showClearCacheDialog = true }) {
                            Text("Limpiar")
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Limpiar búsquedas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Limpiar historial",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Elimina búsquedas recientes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showClearSearchesDialog = true }) {
                            Text("Limpiar")
                        }
                    }
                }
            }

            // Tabs para Favoritos y Búsquedas recientes
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Favoritos (${favorites.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Búsquedas (${recentSearches.size})") }
                )
            }

            // Contenido de tabs
            when (selectedTab) {
                0 -> {
                    // Tab de Favoritos
                    if (favorites.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Sin favoritos",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "Marca fotos como favoritas para verlas aquí",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(favorites, key = { it.id }) { photo ->
                                PhotoCardWithFavorite(
                                    photo = photo,
                                    isFavorite = true,
                                    onClick = {
                                        navController.navigate("details/${photo.id}")
                                    },
                                    onToggleFavorite = {
                                        scope.launch {
                                            repository.toggleFavorite(photo.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Tab de Búsquedas recientes
                    if (recentSearches.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay búsquedas recientes")
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            recentSearches.forEach { query ->
                                ListItem(
                                    headlineContent = { Text(query) },
                                    trailingContent = {
                                        IconButton(onClick = {
                                            scope.launch {
                                                repository.deleteRecentSearch(query)
                                            }
                                        }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Eliminar"
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para limpiar caché
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Limpiar caché") },
            text = { Text("¿Estás seguro? Se eliminarán todas las fotos almacenadas excepto los favoritos.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.clearCache()
                        showClearCacheDialog = false
                    }
                }) {
                    Text("Limpiar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de confirmación para limpiar búsquedas
    if (showClearSearchesDialog) {
        AlertDialog(
            onDismissRequest = { showClearSearchesDialog = false },
            title = { Text("Limpiar historial") },
            text = { Text("¿Estás seguro? Se eliminarán todas las búsquedas recientes.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.clearRecentSearches()
                        showClearSearchesDialog = false
                    }
                }) {
                    Text("Limpiar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSearchesDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}