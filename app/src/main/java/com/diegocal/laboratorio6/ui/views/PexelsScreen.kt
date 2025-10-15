package com.diegocal.laboratorio6.ui.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.diegocal.laboratorio6.Photo
import com.diegocal.laboratorio6.RetrofitPexels
import com.diegocal.laboratorio6.ui.theme.ThemeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun PexelsScreen(
    navController: NavController,
    themeState: ThemeState
) {
    // Estado del UI
    var isLoading by remember { mutableStateOf(false) }
    var photos by remember { mutableStateOf(emptyList<Photo>()) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var searchText by rememberSaveable { mutableStateOf("Nature") }
    var currentPage by rememberSaveable { mutableStateOf(1) }
    var hasNextPage by remember { mutableStateOf(true) }

    // Debounce para la búsqueda
    val searchFlow = remember { snapshotFlow { searchText } }
    LaunchedEffect(searchFlow) {
        searchFlow
            .debounce(500L) // Espera 500 ms antes de emitir un valor
            .collect { query ->
                if (query.isNotEmpty()) {
                    photos = emptyList() // Limpia la lista para la nueva búsqueda
                    currentPage = 1
                    searchPhotos(query, 1,
                        onLoading = { isLoading = it },
                        onSuccess = { newPhotos, hasNext ->
                            photos = newPhotos
                            hasNextPage = hasNext
                        },
                        onError = { errorMessage = it }
                    )
                }
            }
    }

    // Carga de la siguiente página
    val listState = rememberLazyStaggeredGridState()
    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            lastVisibleItemIndex != null && lastVisibleItemIndex >= photos.size - 5
        }
            .collect { shouldLoadMore ->
                if (shouldLoadMore && !isLoading && hasNextPage) {
                    searchPhotos(searchText, currentPage + 1,
                        onLoading = { isLoading = it },
                        onSuccess = { newPhotos, hasNext ->
                            photos = photos + newPhotos
                            hasNextPage = hasNext
                            currentPage++
                        },
                        onError = { errorMessage = it }
                    )
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fotos de Pexels") },
                actions = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil")
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
            // Barra de búsqueda
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Buscar fotos...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Manejo de estados de la UI
            when {
                isLoading && photos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(errorMessage ?: "Error desconocido")
                    }
                }
                photos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se encontraron fotos.")
                    }
                }
                else -> {
                    // Grilla de fotos
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp
                    ) {
                        items(
                            items = photos,
                            key = { it.id }
                        ) { photo ->
                            PhotoCard(
                                photo = photo,
                                onClick = {
                                    navController.navigate("details/${photo.id}")
                                }                            )
                        }
                        // Indicador de carga al final de la lista
                        if (isLoading && hasNextPage) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun searchPhotos(
    query: String,
    page: Int,
    onLoading: (Boolean) -> Unit,
    onSuccess: (List<Photo>, Boolean) -> Unit,
    onError: (String) -> Unit
) {
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    coroutineScope.launch {
        try {
            onLoading(true)
            val response = RetrofitPexels.retrofitService.searchPhotos(
                query = query,
                page = page,
                perPage = 15
            )
            withContext(Dispatchers.Main) {
                onSuccess(response.photos, response.nextPageUrl != null)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Error de red: ${e.message}")
            }
        } finally {
            onLoading(false)
        }
    }
}