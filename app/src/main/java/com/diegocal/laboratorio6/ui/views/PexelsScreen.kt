package com.diegocal.laboratorio6.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.diegocal.laboratorio6.Photo
import com.diegocal.laboratorio6.RetrofitPexels
import com.diegocal.laboratorio6.database.AppDatabase
import com.diegocal.laboratorio6.repository.PhotoRepository
import com.diegocal.laboratorio6.ui.theme.ThemeState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun PexelsScreen(
    navController: NavController,
    themeState: ThemeState
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember {
        PhotoRepository(
            photoDao = database.photoDao(),
            recentSearchDao = database.recentSearchDao(),
            apiService = RetrofitPexels.retrofitService
        )
    }

    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var photos by remember { mutableStateOf(emptyList<Photo>()) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var searchText by rememberSaveable { mutableStateOf("Nature") }
    var currentPage by rememberSaveable { mutableStateOf(1) }
    var hasNextPage by remember { mutableStateOf(true) }
    var isFromCache by remember { mutableStateOf(false) }
    var isReallyOffline by remember { mutableStateOf(false) }

    val recentSearches by repository.getRecentSearches().collectAsState(initial = emptyList())
    val favorites by repository.getFavoritePhotos().collectAsState(initial = emptyList())

    val searchFlow = remember { snapshotFlow { searchText } }
    LaunchedEffect(searchFlow) {
        searchFlow
            .debounce(500L)
            .collect { query ->
                if (query.isNotEmpty()) {
                    photos = emptyList()
                    currentPage = 1
                    performSearch(
                        repository = repository,
                        query = query,
                        page = 1,
                        onLoading = { isLoading = it },
                        onSuccess = { newPhotos, hasNext, fromCache ->
                            photos = newPhotos
                            hasNextPage = hasNext
                            isFromCache = fromCache
                            isReallyOffline = false
                            errorMessage = null
                        },
                        onError = {
                            errorMessage = it
                            isReallyOffline = true
                        }
                    )
                }
            }
    }

    LaunchedEffect(Unit) {
        if (photos.isEmpty()) {
            val cachedPhotos = repository.getCachedPhotosForLastSearch()
            if (cachedPhotos.isNotEmpty()) {
                photos = cachedPhotos
                isFromCache = true
            }
        }
    }

    val listState = rememberLazyStaggeredGridState()
    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            lastVisibleItemIndex != null && lastVisibleItemIndex >= photos.size - 5
        }
            .collect { shouldLoadMore ->
                if (shouldLoadMore && !isLoading && hasNextPage && !isReallyOffline) {
                    performSearch(
                        repository = repository,
                        query = searchText,
                        page = currentPage + 1,
                        onLoading = { isLoading = it },
                        onSuccess = { newPhotos, hasNext, _ ->
                            photos = photos + newPhotos
                            hasNextPage = hasNext
                            isReallyOffline = false
                            currentPage++
                            errorMessage = null
                        },
                        onError = {
                            errorMessage = it
                            isReallyOffline = true
                        }
                    )
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Fotos de Pexels")
                        if (isReallyOffline) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "Offline",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (favorites.isNotEmpty()) {
                        BadgedBox(
                            badge = {
                                Badge { Text("${favorites.size}") }
                            }
                        ) {
                            IconButton(onClick = { navController.navigate("profile") }) {
                                Icon(Icons.Default.Person, contentDescription = "Perfil")
                            }
                        }
                    } else {
                        IconButton(onClick = { navController.navigate("profile") }) {
                            Icon(Icons.Default.Person, contentDescription = "Perfil")
                        }
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
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Buscar fotos...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { searchText = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            if (recentSearches.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(recentSearches) { query ->
                        SuggestionChip(
                            onClick = { searchText = query },
                            label = { Text(query) }
                        )
                    }
                }
            }

            when {
                isLoading && photos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Cargando fotos...")
                        }
                    }
                }
                errorMessage != null && photos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                            Button(onClick = {
                                scope.launch {
                                    val cachedPhotos = repository.getCachedPhotosForLastSearch()
                                    if (cachedPhotos.isNotEmpty()) {
                                        photos = cachedPhotos
                                        isFromCache = true
                                        errorMessage = null
                                    }
                                }
                            }) {
                                Text("Ver caché local")
                            }
                        }
                    }
                }
                photos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se encontraron fotos.")
                    }
                }
                else -> {
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
                            PhotoCardWithFavorite(
                                photo = photo,
                                isFavorite = favorites.any { it.id == photo.id },
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

private fun performSearch(
    repository: PhotoRepository,
    query: String,
    page: Int,
    onLoading: (Boolean) -> Unit,
    onSuccess: (List<Photo>, Boolean, Boolean) -> Unit,
    onError: (String) -> Unit
) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            android.util.Log.d("PexelsScreen", "Iniciando búsqueda: query=$query, page=$page")
            onLoading(true)
            val result = repository.searchPhotos(query, page, perPage = 15)

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                result.fold(
                    onSuccess = { searchResult ->
                        android.util.Log.d("PexelsScreen", "Búsqueda exitosa: ${searchResult.photos.size} fotos, hasNext=${searchResult.hasNextPage}, fromCache=${searchResult.fromCache}")
                        onSuccess(
                            searchResult.photos,
                            searchResult.hasNextPage,
                            searchResult.fromCache
                        )
                    },
                    onFailure = { exception ->
                        android.util.Log.e("PexelsScreen", "Error en búsqueda: ${exception.message}", exception)
                        onError("Error: ${exception.message}")
                    }
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("PexelsScreen", "Excepción inesperada: ${e.message}", e)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onError("Error inesperado: ${e.message}")
            }
        } finally {
            onLoading(false)
        }
    }
}