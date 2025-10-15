package com.diegocal.laboratorio6.ui.views

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.diegocal.laboratorio6.Photo
import com.diegocal.laboratorio6.RetrofitPexels
import com.diegocal.laboratorio6.database.AppDatabase
import com.diegocal.laboratorio6.repository.PhotoRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    navController: NavController,
    photoId: Int
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

    var photo by remember { mutableStateOf<Photo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFavorite by remember { mutableStateOf(false) }

    // Cargar foto al inicio
    LaunchedEffect(photoId) {
        isLoading = true
        errorMessage = null

        val result = repository.getPhotoById(photoId)
        result.fold(
            onSuccess = {
                photo = it
                isFavorite = repository.isFavorite(photoId)
            },
            onFailure = {
                errorMessage = "Error al cargar la foto: ${it.message}"
            }
        )
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isLoading) "Cargando..." else photo?.photographer ?: "Detalles"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    // Botón de compartir
                    if (photo != null) {
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT,
                                    "Mira esta foto de ${photo!!.photographer}: ${photo!!.url}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir foto"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir")
                        }

                        // Botón de favorito
                        IconButton(onClick = {
                            scope.launch {
                                repository.toggleFavorite(photoId)
                                isFavorite = !isFavorite
                            }
                        }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                                tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "Error desconocido",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }) {
                            Text("Volver")
                        }
                    }
                }
                photo != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Imagen grande
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(photo!!.width.toFloat() / photo!!.height.toFloat())
                        ) {
                            AsyncImage(
                                model = photo!!.imageUrl.large,
                                contentDescription = photo!!.photographer,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Información detallada
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Fotógrafo
                                Text(
                                    text = "Fotógrafo",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = photo!!.photographer,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Divider()
                                Spacer(Modifier.height(16.dp))

                                // Dimensiones
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Ancho",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${photo!!.width} px",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Alto",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${photo!!.height} px",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Relación",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "%.2f".format(photo!!.width.toFloat() / photo!!.height.toFloat()),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                                Divider()
                                Spacer(Modifier.height(16.dp))

                                // ID de la foto
                                Text(
                                    text = "ID de la foto",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "#${photo!!.id}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Botón para ver en Pexels
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse(photo!!.url)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ver en Pexels.com")
                        }
                    }
                }
                else -> {
                    Text("No se pudo cargar la foto")
                }
            }
        }
    }
}