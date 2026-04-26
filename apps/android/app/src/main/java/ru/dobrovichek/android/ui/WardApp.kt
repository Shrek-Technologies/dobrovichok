@file:OptIn(ExperimentalMaterial3Api::class)

package ru.dobrovichek.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ru.dobrovichek.android.data.AuthRepository
import ru.dobrovichek.android.data.RequestRepository
import ru.dobrovichek.android.data.SessionStore
import ru.dobrovichek.android.data.UserSession
import ru.dobrovichek.android.util.PersonNameFormat
import ru.dobrovichek.android.data.RequestSummaryDto
import ru.dobrovichek.android.data.RequestResponseDto
import ru.dobrovichek.android.data.UserRepository
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.runtime.image.ImageProvider
import android.Manifest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import ru.dobrovichek.android.R
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.drawable.toBitmap
import android.content.Intent
import android.net.Uri

enum class HelpCategory(val title: String) {
    DELIVERY("Доставка"),
    TECH("Настройка техники"),
    COMPANION("Сопровождение"),
    OTHER("Другое")
}

enum class Urgency(val title: String) {
    ASAP("Как можно скорее"),
    LATER("В другое время")
}

data class WardUiState(
    val category: HelpCategory = HelpCategory.TECH,
    val urgency: Urgency = Urgency.ASAP, 
    val address: String = "Политехническая улица, 29В",
    val apartment: String = "",
    val comment: String = "",
    val latitude: Double = 60.0092,
    val longitude: Double = 30.3578,
    val assignedVolunteerId: String? = null,
    val createdRequestId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class WardViewModel(
    private val repository: RequestRepository,
    private val sessionStore: SessionStore
) : ViewModel() {
    private val _state = MutableStateFlow(WardUiState())
    val state: StateFlow<WardUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    fun chooseCategory(category: HelpCategory) = _state.update { it.copy(category = category) }
    fun chooseUrgency(urgency: Urgency) = _state.update { it.copy(urgency = urgency) }
    fun updateApartment(value: String) = _state.update { it.copy(apartment = value) }
    fun updateComment(value: String) = _state.update { it.copy(comment = value) }
    fun updateAddressPoint(latitude: Double, longitude: Double) {
        _state.update {
            it.copy(
                latitude = latitude,
                longitude = longitude,
                address = "Адрес: уточните по карте"
            )
        }
    }

    fun updateAddressText(address: String) {
        _state.update { it.copy(address = address) }
    }

    fun createRequest(onSuccess: () -> Unit) {
        val session = sessionStore.load() ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.createRequest(
                    category = state.value.category.title,
                    urgency = state.value.urgency.title,
                    address = state.value.address,
                    apartment = state.value.apartment,
                    comment = state.value.comment,
                    latitude = state.value.latitude,
                    longitude = state.value.longitude,
                    wardFirstName = session.firstName.trim(),
                    wardLastName = session.lastName.trim(),
                    wardPatronymic = session.patronymic?.trim()?.takeIf { it.isNotEmpty() },
                    contactPhone = session.phone.trim().ifBlank { "+79990000000" }
                )
            }.onSuccess { requestId ->
                _state.update { it.copy(createdRequestId = requestId, assignedVolunteerId = null, isLoading = false) }
                onSuccess()
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message ?: "Ошибка сети") }
            }
        }
    }

    fun startSearching(onFound: () -> Unit) {
        val requestId = state.value.createdRequestId ?: return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            repeat(120) {
                runCatching { repository.getRequest(requestId) }
                    .onSuccess { request ->
                        if (request.status.equals("ACCEPTED", ignoreCase = true) && request.volunteerId != null) {
                            _state.update { it.copy(assignedVolunteerId = request.volunteerId) }
                            onFound()
                            return@launch
                        }
                    }
                delay(3000)
            }
        }
    }

    fun cancelRequest(onCancelled: () -> Unit) {
        val requestId = state.value.createdRequestId ?: return
        searchJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.cancelRequest(requestId) }
                .onSuccess {
                    _state.update { WardUiState() }
                    onCancelled()
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message ?: "Не удалось отменить заявку") }
                }
        }
    }
}

data class WardFoundUiState(
    val volunteerName: String? = null,
    val volunteerPhone: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class WardFoundViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _state = MutableStateFlow(WardFoundUiState())
    val state: StateFlow<WardFoundUiState> = _state.asStateFlow()

    fun loadVolunteerName(volunteerId: String?) {
        if (volunteerId.isNullOrBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { userRepository.getVolunteerContact(volunteerId) }
                .onSuccess { (name, phone) ->
                    _state.update { it.copy(isLoading = false, volunteerName = name, volunteerPhone = phone) }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Не удалось загрузить волонтёра") } }
        }
    }
}

enum class AuthRegisterStep {
    CHOOSE_ROLE,
    ENTER_DETAILS
}

data class AuthUiState(
    val firstName: String = "",
    val lastName: String = "",
    val patronymic: String = "",
    val phone: String = "",
    val password: String = "",
    val role: String = "WARD",
    val isRegisterMode: Boolean = false,
    val registerStep: AuthRegisterStep = AuthRegisterStep.CHOOSE_ROLE,
    val isLoading: Boolean = false,
    val error: String? = null,
    val session: UserSession? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState(session = authRepository.currentSession()))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun setRegisterMode(enabled: Boolean) = _state.update {
        it.copy(
            isRegisterMode = enabled,
            registerStep = if (enabled) AuthRegisterStep.CHOOSE_ROLE else AuthRegisterStep.CHOOSE_ROLE,
            error = null
        )
    }

    fun selectRegisterRole(role: String) = _state.update {
        it.copy(role = role, registerStep = AuthRegisterStep.ENTER_DETAILS, error = null)
    }

    fun backFromRegisterDetailsToRole() = _state.update {
        it.copy(registerStep = AuthRegisterStep.CHOOSE_ROLE, error = null)
    }

    fun updateFirstName(value: String) = _state.update { it.copy(firstName = value) }
    fun updateLastName(value: String) = _state.update { it.copy(lastName = value) }
    fun updatePatronymic(value: String) = _state.update { it.copy(patronymic = value) }
    fun updatePhone(value: String) = _state.update { it.copy(phone = value) }
    fun updatePassword(value: String) = _state.update { it.copy(password = value) }

    fun submit() {
        val s = state.value
        if (s.isRegisterMode && s.registerStep == AuthRegisterStep.CHOOSE_ROLE) {
            return
        }
        val validationError = validateAuthInput(s)
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val authAction: suspend () -> UserSession = if (state.value.isRegisterMode) {
                {
                    authRepository.register(
                        firstName = state.value.firstName.trim(),
                        lastName = state.value.lastName.trim(),
                        patronymic = state.value.patronymic.trim().takeIf { it.isNotEmpty() },
                        phone = state.value.phone.trim(),
                        password = state.value.password,
                        role = state.value.role
                    )
                }
            } else {
                {
                    authRepository.login(
                        phone = state.value.phone.trim(),
                        password = state.value.password
                    )
                }
            }
            runCatching { authAction() }
                .onSuccess { session ->
                    _state.update { it.copy(isLoading = false, session = session, error = null) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = mapAuthError(error)) }
                }
        }
    }

    fun logout() {
        authRepository.logout()
        _state.update { AuthUiState() }
    }

    private val namePartRegex = Regex("^[\\p{L}]([\\p{L}\\-']){0,59}$")

    private fun validateAuthInput(state: AuthUiState): String? {
        if (state.isRegisterMode && state.registerStep == AuthRegisterStep.ENTER_DETAILS) {
            validateNamePart("Имя", state.firstName)?.let { return it }
            validateNamePart("Фамилия", state.lastName)?.let { return it }
            if (state.patronymic.isNotBlank()) {
                validateNamePart("Отчество", state.patronymic)?.let { return it }
            }
            validatePassword(state.password)?.let { return it }
        } else {
            if (state.password.isBlank()) {
                return "Введите пароль"
            }
        }
        if (!state.phone.matches(Regex("^[0-9+() -]{7,20}$"))) {
            return "Введите корректный телефон (от 7 символов, можно +7...)"
        }
        return null
    }

    private fun validateNamePart(label: String, value: String): String? {
        val t = value.trim()
        if (t.isEmpty()) {
            return "$label: обязательное поле"
        }
        if (t.length > 60 || !namePartRegex.matches(t)) {
            return "$label: только буквы, дефис или апостроф, до 60 символов"
        }
        return null
    }

    private fun validatePassword(password: String): String? {
        if (password.length < 8) {
            return "Пароль: не менее 8 символов"
        }
        if (password.length > 128) {
            return "Пароль: слишком длинный"
        }
        if (!password.any { it.isDigit() }) {
            return "Пароль: нужна хотя бы одна цифра"
        }
        if (!password.any { ch -> ch.isLetter() && ch.isUpperCase() }) {
            return "Пароль: нужна хотя бы одна заглавная буква"
        }
        return null
    }

    private fun mapAuthError(error: Throwable): String {
        if (error is HttpException && error.code() == 400) {
            return "Некорректные данные: проверьте телефон и пароль"
        }
        if (error is HttpException && error.code() == 401) {
            return "Неверный телефон или пароль"
        }
        if (error is HttpException && error.code() == 409) {
            return "Пользователь с таким телефоном уже существует"
        }
        return error.message ?: "Ошибка авторизации"
    }
}

data class VolunteerUiState(
    val isLoading: Boolean = false,
    val acceptingRequest: Boolean = false,
    val error: String? = null,
    val requests: List<RequestSummaryDto> = emptyList(),
    val selected: RequestSummaryDto? = null,
    val acceptedRequestId: String? = null,
    val acceptedDetails: RequestResponseDto? = null
)

class VolunteerViewModel(
    private val repository: RequestRepository
) : ViewModel() {
    private val _state = MutableStateFlow(VolunteerUiState())
    val state: StateFlow<VolunteerUiState> = _state.asStateFlow()

    fun refreshNearby(latitude: Double, longitude: Double, showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _state.update { it.copy(isLoading = true, error = null) }
            } else {
                _state.update { it.copy(error = null) }
            }
            runCatching { repository.findNearby(latitude, longitude, radiusKm = 1.0) }
                .onSuccess { items -> _state.update { it.copy(isLoading = false, requests = items) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, error = error.message ?: "Ошибка загрузки") } }
        }
    }

    fun select(item: RequestSummaryDto?) {
        _state.update { it.copy(selected = item, error = null) }
    }

    /** Закрывает карточку и открывает экран подтверждения без вызова API (принятие — только после «Подтвердить»). */
    fun startConfirmFlow(onNavigate: (String) -> Unit) {
        val selected = state.value.selected ?: return
        val id = selected.id
        _state.update { it.copy(selected = null, error = null) }
        onNavigate(id)
    }

    fun confirmAccept(requestId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(acceptingRequest = true, error = null) }
            runCatching { repository.acceptRequest(requestId) }
                .onSuccess {
                    _state.update { it.copy(acceptingRequest = false, acceptedRequestId = requestId) }
                    onSuccess()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(acceptingRequest = false, error = error.message ?: "Не удалось принять заявку")
                    }
                }
        }
    }

    fun loadAcceptedDetails(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.getRequest(requestId) }
                .onSuccess { details -> _state.update { it.copy(isLoading = false, acceptedDetails = details) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, error = error.message ?: "Не удалось загрузить заявку") } }
        }
    }
}

class AppViewModelFactory(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
    private val requestRepository: RequestRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepository) as T
        }
        if (modelClass.isAssignableFrom(WardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WardViewModel(requestRepository, sessionStore) as T
        }
        if (modelClass.isAssignableFrom(VolunteerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VolunteerViewModel(requestRepository) as T
        }
        if (modelClass.isAssignableFrom(WardFoundViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WardFoundViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun WardApp(
    sessionStore: SessionStore,
    authRepository: AuthRepository,
    requestRepository: RequestRepository,
    userRepository: UserRepository
) {
    val factory = AppViewModelFactory(sessionStore, authRepository, requestRepository, userRepository)
    val authVm: AuthViewModel = viewModel(factory = factory)
    val wardVm: WardViewModel = viewModel(factory = factory)
    val authState by authVm.state.collectAsStateWithLifecycle()
    if (authState.session == null) {
        AuthScreen(
            state = authState,
            onFirstNameChange = authVm::updateFirstName,
            onLastNameChange = authVm::updateLastName,
            onPatronymicChange = authVm::updatePatronymic,
            onPhoneChange = authVm::updatePhone,
            onPasswordChange = authVm::updatePassword,
            onRegisterRoleChosen = authVm::selectRegisterRole,
            onBackFromRegisterDetails = authVm::backFromRegisterDetailsToRole,
            onToggleMode = authVm::setRegisterMode,
            onSubmit = authVm::submit
        )
    } else {
        val session = authState.session!!
        LaunchedEffect(session.userId) {
            withContext(Dispatchers.IO) {
                userRepository.syncMyProfileAfterAuth(session)
            }
        }
        val wardState by wardVm.state.collectAsStateWithLifecycle()
        val volunteerVm: VolunteerViewModel = viewModel(factory = factory)
        val volunteerState by volunteerVm.state.collectAsStateWithLifecycle()
        val navController = rememberNavController()
        val start = if (authState.session?.role == "VOLUNTEER") "volunteer_map" else "home"
        NavHost(navController = navController, startDestination = start) {
            composable("volunteer_map") {
                VolunteerMapScreen(
                    state = volunteerState,
                    onRefresh = { lat, lon -> volunteerVm.refreshNearby(lat, lon, showLoading = false) },
                    onSelect = volunteerVm::select,
                    onAcceptSelected = {
                        volunteerVm.startConfirmFlow { id -> navController.navigate("volunteer_confirm/$id") }
                    },
                    onLogout = authVm::logout
                )
            }
            composable("volunteer_confirm/{requestId}") { backStackEntry ->
                val requestId = backStackEntry.arguments?.getString("requestId") ?: return@composable
                val vState by volunteerVm.state.collectAsStateWithLifecycle()
                VolunteerAcceptConfirmScreen(
                    accepting = vState.acceptingRequest,
                    error = vState.error,
                    onBack = { navController.popBackStack() },
                    onConfirm = {
                        volunteerVm.confirmAccept(requestId) {
                            navController.navigate("volunteer_details/$requestId")
                        }
                    }
                )
            }
            composable("volunteer_details/{requestId}") { backStackEntry ->
                val requestId = backStackEntry.arguments?.getString("requestId") ?: return@composable
                LaunchedEffect(requestId) { volunteerVm.loadAcceptedDetails(requestId) }
                VolunteerRequestDetailsScreen(
                    state = volunteerState,
                    onBackToMap = { navController.popBackStack("volunteer_map", inclusive = false) }
                )
            }
            composable("home") {
                HomeScreen(
                    session = authState.session,
                    onCreate = { navController.navigate("step1") },
                    onLogout = authVm::logout
                )
            }
            composable("step1") {
                StepOneScreen(
                    state = wardState,
                    onCategory = wardVm::chooseCategory,
                    onUrgency = wardVm::chooseUrgency,
                    onNext = { navController.navigate("address") }
                )
            }
            composable("address") {
                AddressScreen(
                    state = wardState,
                    onAddressPointChange = wardVm::updateAddressPoint,
                    onAddressTextChange = wardVm::updateAddressText,
                    onNext = { navController.navigate("confirm") }
                )
            }
            composable("confirm") {
                ConfirmScreen(
                    state = wardState,
                    onApartmentChange = wardVm::updateApartment,
                    onCommentChange = wardVm::updateComment,
                    onCreate = {
                        wardVm.createRequest { navController.navigate("searching") }
                    }
                )
            }
            composable("searching") {
                SearchingScreen(
                    state = wardState,
                    onStartSearch = { wardVm.startSearching { navController.navigate("found") } },
                    onCancel = { wardVm.cancelRequest { navController.navigate("home") } }
                )
            }
            composable("found") {
                val foundVm: WardFoundViewModel = viewModel(factory = factory)
                LaunchedEffect(wardState.assignedVolunteerId) {
                    foundVm.loadVolunteerName(wardState.assignedVolunteerId)
                }
                val foundState by foundVm.state.collectAsStateWithLifecycle()
                VolunteerFoundScreen(
                    state = wardState,
                    volunteerName = foundState.volunteerName,
                    volunteerPhone = foundState.volunteerPhone,
                    loadingContact = foundState.isLoading,
                    contactError = foundState.error,
                    onCancel = { wardVm.cancelRequest { navController.navigate("home") } }
                )
            }
        }
    }
}

@Composable
private fun VolunteerMapScreen(
    state: VolunteerUiState,
    onRefresh: (Double, Double) -> Unit,
    onSelect: (RequestSummaryDto?) -> Unit,
    onAcceptSelected: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var userLocationLayer: UserLocationLayer? by remember { mutableStateOf(null) }
    var currentLocation: Point? by remember { mutableStateOf(null) }
    var mapInitialized by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op */ }

    DisposableEffect(mapView) {
        mapView.onStart()
        onDispose { mapView.onStop() }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val fallback = remember { Point(60.0092, 30.3578) }
    val pinBitmap: Bitmap? = remember {
        runCatching {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_pin) ?: return@runCatching null
            // Ensure we have a reasonable size for map markers
            drawable.toBitmap(width = 96, height = 96, config = Bitmap.Config.ARGB_8888)
        }.getOrNull()
    }
    // Stable marker collection to prevent blinking
    var markersCollection: MapObjectCollection? by remember { mutableStateOf(null) }
    var markersKey: String by remember { mutableStateOf("") }

    // Load last known location once we have permission (used for centering and nearby search)
    LaunchedEffect(Unit) {
        val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            @Suppress("MissingPermission")
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) currentLocation = Point(loc.latitude, loc.longitude)
            }
        }
    }

    // Auto-refresh nearby requests every 5 seconds WITHOUT moving the camera
    LaunchedEffect(currentLocation) {
        while (true) {
            val p = currentLocation ?: fallback
            // Silent refresh (no loading flicker)
            onRefresh(p.latitude, p.longitude)
            delay(5000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Поиск заданий") },
                actions = {
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Close, contentDescription = "Выйти") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { mapView },
                update = {
                    val map = mapView.mapWindow.map
                    if (!mapInitialized) {
                        val target = currentLocation ?: fallback
                        map.move(CameraPosition(target, 14.5f, 0f, 0f))
                        mapInitialized = true
                    }

                    val hasLocationPermission =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                    if (hasLocationPermission) {
                        if (userLocationLayer == null) {
                            val mapKit = MapKitFactory.getInstance()
                            userLocationLayer = mapKit.createUserLocationLayer(mapView.mapWindow)
                        }
                        userLocationLayer?.isVisible = true
                        val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                        @Suppress("MissingPermission")
                        fused.lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) currentLocation = Point(loc.latitude, loc.longitude)
                        }
                    } else {
                        userLocationLayer?.isVisible = false
                    }

                    val icon = pinBitmap?.let { ImageProvider.fromBitmap(it) }
                    if (markersCollection == null) {
                        markersCollection = map.mapObjects.addCollection()
                    }
                    // Update markers only if the set changed (avoids frequent clear/re-add)
                    val newKey = state.requests.map { it.id }.sorted().joinToString("|")
                    if (newKey != markersKey) {
                        markersKey = newKey
                        markersCollection?.clear()
                        state.requests.forEach { req ->
                            val point = Point(req.location.latitude, req.location.longitude)
                            val placemark = if (icon != null) {
                                markersCollection!!.addPlacemark(point, icon, IconStyle().apply { scale = 1.0f })
                            } else {
                                markersCollection!!.addPlacemark(point)
                            }
                            placemark.addTapListener(MapObjectTapListener { _, _ ->
                                onSelect(req)
                                true
                            })
                        }
                    }
                }
            )

            Button(
                onClick = {
                    val p = currentLocation ?: fallback
                    onRefresh(p.latitude, p.longitude)
                },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
            ) {
                Text("Обновить")
            }

            state.error?.let {
                LaunchedEffect(it) { snackbarHostState.showSnackbar(it) }
            }
        }
    }

    if (state.selected != null) {
        ModalBottomSheet(
            onDismissRequest = { onSelect(null) },
            sheetState = sheetState
        ) {
            VolunteerRequestSheet(
                item = state.selected,
                onAccept = onAcceptSelected,
                onClose = { onSelect(null) }
            )
        }
    }
}

@Composable
private fun VolunteerRequestSheet(item: RequestSummaryDto, onAccept: () -> Unit, onClose: () -> Unit) {
    val wardShort = item.wardFirstName?.trim()?.takeIf { it.isNotEmpty() } ?: "Подопечный"
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Заявка от $wardShort", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Закрыть") }
        }
        Spacer(Modifier.height(8.dp))
        Text(item.description, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Text("${"%.2f".format(item.distanceKm)} км от вас", color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) {
            Text("Помогу")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun VolunteerAcceptConfirmScreen(
    accepting: Boolean,
    error: String?,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Подтверждение", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Подтвердите, что вы готовы помочь. Только после этого подопечный увидит, что волонтёр нашёлся, и получит ваши контакты.",
            color = Color.Gray
        )
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onConfirm,
            enabled = !accepting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (accepting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Отправка…")
                }
            } else {
                Text("Да, я готов")
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack, enabled = !accepting, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}

@Composable
private fun VolunteerRequestDetailsScreen(state: VolunteerUiState, onBackToMap: () -> Unit) {
    val details = state.acceptedDetails
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Контакты и детали", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (state.isLoading && details == null) {
            CircularProgressIndicator()
            return
        }
        if (details == null) {
            Text("Не удалось загрузить данные заявки", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBackToMap) { Text("Назад") }
            return
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                val wardFull = details.wardFullName?.trim()?.takeIf { it.isNotEmpty() }
                    ?: PersonNameFormat.fullFormal(
                        details.wardFirstName.orEmpty(),
                        details.wardPatronymic,
                        details.wardLastName.orEmpty()
                    ).ifBlank { null }
                wardFull?.let {
                    Text("Подопечный", color = Color.Gray)
                    Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                }
                Text(details.description ?: "", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                Text("Телефон подопечного", fontWeight = FontWeight.Medium)
                Text(details.contactPhone ?: "Недоступно", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Статус: ${details.status}", color = Color.Gray)
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onBackToMap, modifier = Modifier.fillMaxWidth()) { Text("Назад к карте") }
    }
}

@Composable
private fun AuthScreen(
    state: AuthUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onPatronymicChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRegisterRoleChosen: (String) -> Unit,
    onBackFromRegisterDetails: () -> Unit,
    onToggleMode: (Boolean) -> Unit,
    onSubmit: () -> Unit
) {
    when {
        state.isRegisterMode && state.registerStep == AuthRegisterStep.CHOOSE_ROLE -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Добровичок", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Выберите роль", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { onRegisterRoleChosen("WARD") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Я хочу получать оперативную помощь", style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onRegisterRoleChosen("VOLUNTEER") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Я волонтёр и хочу помогать", style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onToggleMode(false) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("У меня уже есть аккаунт")
                }
            }
        }
        else -> {
            val showRegisterFields = state.isRegisterMode && state.registerStep == AuthRegisterStep.ENTER_DETAILS
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Добровичок", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        showRegisterFields -> "Регистрация"
                        else -> "Вход"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
                if (showRegisterFields) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (state.role == "VOLUNTEER") "Роль: волонтёр" else "Роль: подопечный",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                Spacer(Modifier.height(16.dp))
                if (showRegisterFields) {
                    OutlinedTextField(
                        value = state.firstName,
                        onValueChange = onFirstNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Имя") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.lastName,
                        onValueChange = onLastNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Фамилия") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.patronymic,
                        onValueChange = onPatronymicChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Отчество (необязательно)") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = onPhoneChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Телефон") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Пароль") },
                    supportingText = if (showRegisterFields) {
                        { Text("Не меньше 8 символов, цифра и заглавная буква", style = MaterialTheme.typography.bodySmall) }
                    } else null
                )
                state.error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    enabled = !state.isLoading,
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showRegisterFields) "Зарегистрироваться" else "Войти")
                }
                if (showRegisterFields) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onBackFromRegisterDetails,
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Назад к выбору роли")
                    }
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onToggleMode(!state.isRegisterMode) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isRegisterMode) "У меня уже есть аккаунт" else "Создать новый аккаунт")
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(session: UserSession?, onCreate: () -> Unit, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Добровичок", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            session?.let { PersonNameFormat.volunteerForWard(it.firstName, it.lastName).ifBlank { it.fullName } }
                ?: "Помощь рядом",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
            Text("Создать заявку")
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Выйти")
        }
    }
}

@Composable
private fun StepOneScreen(
    state: WardUiState,
    onCategory: (HelpCategory) -> Unit,
    onUrgency: (Urgency) -> Unit,
    onNext: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Что Вам нужно?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        HelpCategory.entries.forEach { category ->
            SelectCard(
                title = category.title,
                selected = state.category == category,
                tint = if (category == HelpCategory.TECH) Color(0xFFDDF8DA) else Color(0xFFF6F6F6),
                onClick = { onCategory(category) }
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Когда нужно помочь?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Urgency.entries.forEach { urgency ->
            SelectCard(
                title = urgency.title,
                selected = state.urgency == urgency,
                tint = Color(0xFFFFF2E5),
                onClick = { onUrgency(urgency) }
            )
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Далее") }
    }
}

@Composable
private fun AddressScreen(
    state: WardUiState,
    onAddressPointChange: (Double, Double) -> Unit,
    onAddressTextChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    var mapInitialized by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            @Suppress("MissingPermission")
            fused.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val point = Point(location.latitude, location.longitude)
                    mapView.mapWindow.map.move(CameraPosition(point, 16f, 0f, 0f))
                    onAddressPointChange(location.latitude, location.longitude)
                }
            }
        }
    }

    DisposableEffect(mapView) {
        mapView.onStart()
        onDispose { mapView.onStop() }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            @Suppress("MissingPermission")
            fused.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    mapView.mapWindow.map.move(CameraPosition(Point(location.latitude, location.longitude), 16f, 0f, 0f))
                    onAddressPointChange(location.latitude, location.longitude)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Проверьте Ваш адрес", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Передвиньте карту: маркер в центре должен стоять на вашем доме",
            color = Color.Gray
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { mapView },
                update = {
                    if (!mapInitialized) {
                        it.mapWindow.map.move(CameraPosition(Point(state.latitude, state.longitude), 16f, 0f, 0f))
                        mapInitialized = true
                    }
                }
            )
            Text(
                "",
                modifier = Modifier.align(Alignment.Center)
            )
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = ru.dobrovichek.android.R.drawable.ic_pin),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(state.address, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                val center = mapView.mapWindow.map.cameraPosition.target
                onAddressPointChange(center.latitude, center.longitude)
                resolveAddress(context, center.latitude, center.longitude)?.let(onAddressTextChange)
                onNext()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Далее") }
    }
}

private fun resolveAddress(context: android.content.Context, latitude: Double, longitude: Double): String? {
    return runCatching {
        val geocoder = android.location.Geocoder(context, java.util.Locale("ru", "RU"))
        val results = geocoder.getFromLocation(latitude, longitude, 1) ?: return null
        val a = results.firstOrNull() ?: return null
        // Prefer a short, readable line for seniors
        listOfNotNull(a.thoroughfare, a.subThoroughfare)
            .joinToString(" ")
            .ifBlank { a.getAddressLine(0) }
            ?.let { "Адрес: $it" }
    }.getOrNull()
}

@Composable
private fun ConfirmScreen(
    state: WardUiState,
    onApartmentChange: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onCreate: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Все верно?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Проверьте заявку", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        SelectCard(title = state.category.title, selected = true, tint = Color(0xFFDDF8DA), onClick = {})
        SelectCard(title = state.urgency.title, selected = true, tint = Color(0xFFFFF2E5), onClick = {})
        Spacer(Modifier.height(12.dp))
        Text("Адрес", color = Color.Gray)
        Text(state.address, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = state.apartment,
            onValueChange = onApartmentChange,
            label = { Text("Квартира / подъезд *") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.comment,
            onValueChange = onCommentChange,
            label = { Text("Примечание для волонтёра") },
            modifier = Modifier.fillMaxWidth().height(160.dp)
        )
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.weight(1f))
        Button(
            enabled = !state.isLoading && state.apartment.isNotBlank(),
            onClick = onCreate,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp))
            } else {
                Text("Создать заявку")
            }
        }
    }
}

@Composable
private fun SearchingScreen(state: WardUiState, onStartSearch: () -> Unit, onCancel: () -> Unit) {
    var seconds by remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            seconds++
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { onStartSearch() }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Поиск волонтера...", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Пожалуйста, подождите пока мы найдем того, кто сможет Вам помочь")
        Spacer(Modifier.height(12.dp))
        val mm = (seconds / 60).toString().padStart(2, '0')
        val ss = (seconds % 60).toString().padStart(2, '0')
        Text("$mm:$ss", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator()
        Spacer(Modifier.height(48.dp))
        Button(enabled = !state.isLoading, onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Отменить заявку")
        }
    }
}

@Composable
private fun VolunteerFoundScreen(
    state: WardUiState,
    volunteerName: String?,
    volunteerPhone: String?,
    loadingContact: Boolean,
    contactError: String?,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val nameOk = !volunteerName.isNullOrBlank()
    val phoneOk = !volunteerPhone.isNullOrBlank()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Волонтер найден и уже спешит к Вам!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                when {
                    loadingContact -> {
                        Text("Загружаем контакты волонтёра…", color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        CircularProgressIndicator(Modifier.size(36.dp))
                    }
                    contactError != null -> {
                        Text(contactError, color = MaterialTheme.colorScheme.error)
                    }
                    else -> {
                        Text("Волонтёр", color = Color.Gray)
                        Text(
                            if (nameOk) volunteerName!! else "Имя не указано в профиле",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Телефон", color = Color.Gray)
                        if (phoneOk) {
                            Text(volunteerPhone!!, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${volunteerPhone}")
                                    }
                                    context.startActivity(dialIntent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Позвонить")
                            }
                        } else {
                            Text("Не указан в профиле", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("${state.category.title}, ${state.address}", color = Color.Gray)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(enabled = !state.isLoading, onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Отменить заявку")
        }
    }
}

@Composable
private fun SelectCard(
    title: String,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Color(0xFF3CB371) else Color(0xFFE0E0E0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(tint, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = if (selected) "✓  $title" else title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = borderColor
        )
    }
}
