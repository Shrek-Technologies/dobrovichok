@file:OptIn(ExperimentalMaterial3Api::class)

package ru.dobrovichek.android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import retrofit2.HttpException
import ru.dobrovichek.android.data.AuthRepository
import ru.dobrovichek.android.data.PushRegistration
import ru.dobrovichek.android.data.RequestRepository
import ru.dobrovichek.android.data.SessionStore
import ru.dobrovichek.android.data.UserSession
import ru.dobrovichek.android.util.PersonNameFormat
import ru.dobrovichek.android.util.UserFacingErrors
import ru.dobrovichek.android.data.RequestSummaryDto
import ru.dobrovichek.android.data.RequestResponseDto
import ru.dobrovichek.android.data.UserRepository
import ru.dobrovichek.android.data.VolunteerRatingHttpException
import ru.dobrovichek.android.data.VolunteerProfileDto
import ru.dobrovichek.android.data.VolunteerRequestHistoryItemDto
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
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import ru.dobrovichek.android.R
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.drawable.toBitmap
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.SpanStyle
import ru.dobrovichek.android.ui.theme.DobrovichekColors
import ru.dobrovichek.android.ui.theme.DobrovichekCardShape
import ru.dobrovichek.android.ui.theme.DobrovichekWardBackground
import ru.dobrovichek.android.ui.theme.GradientPrimaryButton
import ru.dobrovichek.android.ui.theme.SoftOrangeButton

private fun HelpCategory.palette(): Pair<Color, Color> = when (this) {
    HelpCategory.DELIVERY -> DobrovichekColors.OrangeSoft to Color(0xFFFF9E67)
    HelpCategory.TECH -> DobrovichekColors.MintSoft to DobrovichekColors.MintBorder
    HelpCategory.COMPANION -> DobrovichekColors.SkySoft to DobrovichekColors.SkyBorder
    HelpCategory.OTHER -> Color(0xFFF3F4F6) to Color(0xFFADB5BD)
}

private fun HelpCategory.subtitle(): String = when (this) {
    HelpCategory.DELIVERY -> "Продукты, лекарства и другое"
    HelpCategory.TECH -> "Телефон, интернет, техника"
    HelpCategory.COMPANION -> "Прогулка, визиты, сопровождение"
    HelpCategory.OTHER -> "Иная помощь"
}

private fun Urgency.accentBorder(): Color = when (this) {
    Urgency.ASAP -> DobrovichekColors.OrangeCoral
    Urgency.LATER -> DobrovichekColors.BluePrimary
}

private fun Urgency.rowBackground(): Color = when (this) {
    Urgency.ASAP -> DobrovichekColors.OrangeSoft
    Urgency.LATER -> Color(0xFFECEFF1)
}

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
    val preferredTime: String = "",
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
    private var foundWatchJob: Job? = null

    fun chooseCategory(category: HelpCategory) = _state.update { it.copy(category = category) }
    fun chooseUrgency(urgency: Urgency) = _state.update {
        it.copy(
            urgency = urgency,
            preferredTime = if (urgency == Urgency.ASAP) "" else it.preferredTime
        )
    }

    fun updatePreferredTime(value: String) = _state.update { it.copy(preferredTime = value) }
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
                    preferredTime = state.value.preferredTime.takeIf { state.value.urgency == Urgency.LATER },
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
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = UserFacingErrors.networkOrHttp(error, "Не удалось создать заявку")
                    )
                }
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
        foundWatchJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.cancelRequest(requestId) }
                .onSuccess {
                    _state.update { WardUiState() }
                    onCancelled()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = UserFacingErrors.networkOrHttp(error, "Не удалось отменить заявку")
                        )
                    }
                }
        }
    }

    fun completeHelp(onSuccess: () -> Unit) {
        val requestId = state.value.createdRequestId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.completeRequest(requestId) }
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = UserFacingErrors.networkOrHttp(error, "Не удалось закрыть заявку")
                        )
                    }
                }
        }
    }

    fun startFoundWatcher(
        onCancelled: () -> Unit,
        onVolunteerReleased: () -> Unit,
        onCompleted: (volunteerId: String, requestId: String) -> Unit
    ) {
        val requestId = state.value.createdRequestId ?: return
        foundWatchJob?.cancel()
        foundWatchJob = viewModelScope.launch {
            while (isActive) {
                runCatching { repository.getRequest(requestId) }
                    .onSuccess { req ->
                        when {
                            req.status.equals("CANCELLED", ignoreCase = true) -> {
                                _state.update { WardUiState() }
                                onCancelled()
                                return@launch
                            }
                            req.status.equals("COMPLETED", ignoreCase = true) -> {
                                val vid = req.volunteerId?.takeIf { it.isNotBlank() }
                                    ?: state.value.assignedVolunteerId?.takeIf { it.isNotBlank() }
                                if (vid != null) {
                                    onCompleted(vid, requestId)
                                }
                                return@launch
                            }
                            req.status.equals("CREATED", ignoreCase = true) &&
                                req.volunteerId.isNullOrBlank() -> {
                                _state.update { it.copy(assignedVolunteerId = null) }
                                onVolunteerReleased()
                                return@launch
                            }
                        }
                    }
                delay(3000)
            }
        }
    }

    fun stopFoundWatcher() {
        foundWatchJob?.cancel()
        foundWatchJob = null
    }

    fun clearWardRequestFlow() {
        searchJob?.cancel()
        foundWatchJob?.cancel()
        _state.update { WardUiState() }
    }

    fun hydrateActiveWardRequest(request: RequestResponseDto) {
        val loc = request.location
        _state.update { s ->
            s.copy(
                createdRequestId = request.id,
                assignedVolunteerId = request.volunteerId?.takeIf { it.isNotBlank() },
                latitude = loc?.latitude ?: s.latitude,
                longitude = loc?.longitude ?: s.longitude
            )
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
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = UserFacingErrors.networkOrHttp(e, "Не удалось загрузить данные волонтёра")
                        )
                    }
                }
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
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
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
        viewModelScope.launch {
            runCatching { userRepository.unregisterDevice() }
            authRepository.logout()
            _state.update { AuthUiState() }
        }
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
        if (error is HttpException) {
            when (error.code()) {
                400 -> return "Некорректные данные: проверьте телефон и пароль"
                401 -> return "Неверный телефон или пароль"
                409 -> return "Пользователь с таким телефоном уже существует"
                in 500..599 -> return UserFacingErrors.SERVICE_UNAVAILABLE
            }
        }
        return UserFacingErrors.networkOrHttp(error, "Не удалось выполнить вход. Попробуйте позже.")
    }
}

data class VolunteerUiState(
    val isLoading: Boolean = false,
    val acceptingRequest: Boolean = false,
    val error: String? = null,
    val requests: List<RequestSummaryDto> = emptyList(),
    val nearbyEpoch: Int = 0,
    val selected: RequestSummaryDto? = null,
    val acceptedRequestId: String? = null,
    val acceptedDetails: RequestResponseDto? = null
)

class VolunteerViewModel(
    private val repository: RequestRepository
) : ViewModel() {
    private val _state = MutableStateFlow(VolunteerUiState())
    val state: StateFlow<VolunteerUiState> = _state.asStateFlow()
    private var detailsWatchJob: Job? = null

    fun onVolunteerMapShown() {
        _state.update { it.copy(nearbyEpoch = it.nearbyEpoch + 1) }
    }

    fun refreshNearby(latitude: Double, longitude: Double, showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _state.update { it.copy(isLoading = true, error = null) }
            } else {
                _state.update { it.copy(error = null) }
            }
            runCatching { repository.findNearby(latitude, longitude, radiusKm = 1.0) }
                .onSuccess { items ->
                    _state.update { s ->
                        val newIds = items.map { it.id }.sorted().joinToString("|")
                        val oldIds = s.requests.map { it.id }.sorted().joinToString("|")
                        val bump = if (newIds != oldIds) 1 else 0
                        s.copy(isLoading = false, requests = items, nearbyEpoch = s.nearbyEpoch + bump)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = UserFacingErrors.networkOrHttp(error, "Не удалось обновить карту")
                        )
                    }
                }
        }
    }

    fun select(item: RequestSummaryDto?) {
        _state.update { it.copy(selected = item, error = null) }
    }

    /** Принятие заявки на сервере — только после экрана «Подтвердить», не здесь. */
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
                        it.copy(
                            acceptingRequest = false,
                            error = UserFacingErrors.networkOrHttp(error, "Не удалось принять заявку")
                        )
                    }
                }
        }
    }

    fun loadAcceptedDetails(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.getRequest(requestId) }
                .onSuccess { details -> _state.update { it.copy(isLoading = false, acceptedDetails = details) } }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = UserFacingErrors.networkOrHttp(error, "Не удалось загрузить заявку")
                        )
                    }
                }
        }
    }

    fun abandonAcceptedRequest(requestId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.abandonVolunteer(requestId) }
                .onSuccess {
                    stopDetailsWatcher()
                    _state.update {
                        it.copy(isLoading = false, acceptedRequestId = null, acceptedDetails = null)
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = UserFacingErrors.networkOrHttp(error, "Не удалось отказаться от заявки")
                        )
                    }
                }
        }
    }

    fun startDetailsWatcher(
        requestId: String,
        myVolunteerId: String,
        onRequestCancelled: () -> Unit,
        onReleased: () -> Unit,
        onCompleted: () -> Unit
    ) {
        detailsWatchJob?.cancel()
        detailsWatchJob = viewModelScope.launch {
            while (isActive) {
                runCatching { repository.getRequest(requestId) }
                    .onSuccess { req ->
                        when {
                            req.status.equals("CANCELLED", ignoreCase = true) -> {
                                _state.update {
                                    it.copy(acceptedRequestId = null, acceptedDetails = null)
                                }
                                onRequestCancelled()
                                return@launch
                            }
                            req.status.equals("COMPLETED", ignoreCase = true) -> {
                                _state.update {
                                    it.copy(acceptedRequestId = null, acceptedDetails = null)
                                }
                                onCompleted()
                                return@launch
                            }
                            req.status.equals("CREATED", ignoreCase = true) &&
                                req.volunteerId.isNullOrBlank() -> {
                                _state.update {
                                    it.copy(acceptedRequestId = null, acceptedDetails = null)
                                }
                                onReleased()
                                return@launch
                            }
                            req.status.equals("ACCEPTED", ignoreCase = true) &&
                                !req.volunteerId.isNullOrBlank() &&
                                req.volunteerId != myVolunteerId -> {
                                _state.update {
                                    it.copy(acceptedRequestId = null, acceptedDetails = null)
                                }
                                onReleased()
                                return@launch
                            }
                        }
                    }
                delay(3000)
            }
        }
    }

    fun stopDetailsWatcher() {
        detailsWatchJob?.cancel()
        detailsWatchJob = null
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
            return AuthViewModel(authRepository, userRepository) as T
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
        val context = LocalContext.current
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }
        LaunchedEffect(session.userId) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            withContext(Dispatchers.IO) {
                userRepository.syncMyProfileAfterAuth(session)
                runCatching { PushRegistration.syncToBackend(userRepository) }
            }
        }
        val wardState by wardVm.state.collectAsStateWithLifecycle()
        val volunteerVm: VolunteerViewModel = viewModel(factory = factory)
        val volunteerState by volunteerVm.state.collectAsStateWithLifecycle()
        val navController = rememberNavController()
        val navigateWardHome: () -> Unit = {
            navController.navigate("home") {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
        NavHost(navController = navController, startDestination = "resume") {
            composable("resume") {
                Box(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                LaunchedEffect(session.userId, session.role) {
                    val dest = try {
                        when (session.role) {
                            "VOLUNTEER" -> {
                                val active = requestRepository.getActiveRequestOrNull()
                                if (active != null) "volunteer_details/${active.id}" else "volunteer_map"
                            }
                            else -> {
                                val active = requestRepository.getActiveRequestOrNull()
                                when {
                                    active == null -> "home"
                                    active.status.equals("CANCELLED", ignoreCase = true) -> "home"
                                    active.status.equals("COMPLETED", ignoreCase = true) -> "home"
                                    active.status.equals("ACCEPTED", ignoreCase = true) &&
                                        !active.volunteerId.isNullOrBlank() -> {
                                        wardVm.hydrateActiveWardRequest(active)
                                        "found"
                                    }
                                    active.status.equals("CREATED", ignoreCase = true) -> {
                                        wardVm.hydrateActiveWardRequest(active)
                                        "searching"
                                    }
                                    active.status.equals("ACCEPTED", ignoreCase = true) -> {
                                        wardVm.hydrateActiveWardRequest(active)
                                        "searching"
                                    }
                                    else -> "home"
                                }
                            }
                        }
                    } catch (_: Exception) {
                        if (session.role == "VOLUNTEER") "volunteer_map" else "home"
                    }
                    navController.navigate(dest) {
                        popUpTo("resume") { inclusive = true }
                    }
                }
            }
            composable("volunteer_map") {
                LaunchedEffect(Unit) { volunteerVm.onVolunteerMapShown() }
                VolunteerMapScreen(
                    state = volunteerState,
                    onRefresh = { lat, lon -> volunteerVm.refreshNearby(lat, lon, showLoading = false) },
                    onSelect = volunteerVm::select,
                    onAcceptSelected = {
                        volunteerVm.startConfirmFlow { id -> navController.navigate("volunteer_confirm/$id") }
                    },
                    onOpenProfile = { navController.navigate("volunteer_profile") }
                )
            }
            composable("volunteer_profile") {
                VolunteerSelfProfileScreen(
                    volunteerId = session.userId,
                    userRepository = userRepository,
                    onClose = { navController.popBackStack() },
                    onLogout = authVm::logout
                )
            }
            composable("volunteer_confirm/{requestId}") { backStackEntry ->
                val requestId = backStackEntry.arguments?.getString("requestId") ?: return@composable
                val vState by volunteerVm.state.collectAsStateWithLifecycle()
                VolunteerAcceptConfirmScreen(
                    accepting = vState.acceptingRequest,
                    error = vState.error,
                    onBack = {
                        volunteerVm.onVolunteerMapShown()
                        navController.popBackStack()
                    },
                    onConfirm = {
                        volunteerVm.confirmAccept(requestId) {
                            navController.navigate("volunteer_details/$requestId")
                        }
                    }
                )
            }
            composable("volunteer_details/{requestId}") { backStackEntry ->
                val requestId = backStackEntry.arguments?.getString("requestId") ?: return@composable
                val vContext = LocalContext.current
                val vSession = authState.session!!
                LaunchedEffect(requestId) { volunteerVm.loadAcceptedDetails(requestId) }
                DisposableEffect(requestId, vSession.userId) {
                    volunteerVm.startDetailsWatcher(
                        requestId = requestId,
                        myVolunteerId = vSession.userId,
                        onRequestCancelled = {
                            Toast.makeText(vContext, "Заявка отменена.", Toast.LENGTH_SHORT).show()
                            volunteerVm.onVolunteerMapShown()
                            navController.popBackStack("volunteer_map", inclusive = false)
                        },
                        onReleased = {
                            Toast.makeText(vContext, "Заявка больше не назначена вам.", Toast.LENGTH_SHORT).show()
                            volunteerVm.onVolunteerMapShown()
                            navController.popBackStack("volunteer_map", inclusive = false)
                        },
                        onCompleted = {
                            navController.navigate("volunteer_help_done") {
                                popUpTo("volunteer_map") { inclusive = false }
                            }
                        }
                    )
                    onDispose { volunteerVm.stopDetailsWatcher() }
                }
                VolunteerRequestDetailsScreen(
                    state = volunteerState,
                    onAbandonHelp = {
                        volunteerVm.abandonAcceptedRequest(requestId) {
                            Toast.makeText(vContext, "Вы отказались от заявки.", Toast.LENGTH_SHORT).show()
                            volunteerVm.onVolunteerMapShown()
                            navController.popBackStack("volunteer_map", inclusive = false)
                        }
                    }
                )
            }
            composable("volunteer_help_done") {
                VolunteerHelpDoneScreen(
                    onFindMore = {
                        navController.navigate("volunteer_map") {
                            popUpTo("volunteer_map") { inclusive = true }
                        }
                    }
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
                    onPreferredTimeChange = wardVm::updatePreferredTime,
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
                val wContext = LocalContext.current
                SearchingScreen(
                    state = wardState,
                    onStartSearch = { wardVm.startSearching { navController.navigate("found") } },
                    onCancel = {
                        wardVm.cancelRequest {
                            Toast.makeText(wContext, "Заявка отменена.", Toast.LENGTH_SHORT).show()
                            navigateWardHome()
                        }
                    }
                )
            }
            composable("found") {
                val foundVm: WardFoundViewModel = viewModel(factory = factory)
                val fContext = LocalContext.current
                LaunchedEffect(wardState.assignedVolunteerId) {
                    foundVm.loadVolunteerName(wardState.assignedVolunteerId)
                }
                DisposableEffect(wardState.createdRequestId) {
                    wardVm.startFoundWatcher(
                        onCancelled = {
                            Toast.makeText(fContext, "Заявка отменена.", Toast.LENGTH_SHORT).show()
                            navigateWardHome()
                        },
                        onVolunteerReleased = {
                            navController.navigate("searching") {
                                popUpTo("found") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onCompleted = { volunteerId, requestId ->
                            navController.navigate("rate_volunteer/$volunteerId/$requestId") {
                                popUpTo("home") { inclusive = false }
                            }
                        }
                    )
                    onDispose { wardVm.stopFoundWatcher() }
                }
                val foundState by foundVm.state.collectAsStateWithLifecycle()
                VolunteerFoundScreen(
                    state = wardState,
                    volunteerName = foundState.volunteerName,
                    volunteerPhone = foundState.volunteerPhone,
                    loadingContact = foundState.isLoading,
                    contactError = foundState.error,
                    onCompleteHelp = {
                        wardVm.completeHelp {
                            val s = wardVm.state.value
                            val vid = s.assignedVolunteerId ?: return@completeHelp
                            val rid = s.createdRequestId ?: return@completeHelp
                            navController.navigate("rate_volunteer/$vid/$rid") {
                                popUpTo("home") { inclusive = false }
                            }
                        }
                    },
                    onCancel = {
                        wardVm.cancelRequest {
                            Toast.makeText(fContext, "Заявка отменена.", Toast.LENGTH_SHORT).show()
                            navigateWardHome()
                        }
                    }
                )
            }
            composable("rate_volunteer/{volunteerId}/{requestId}") { backStackEntry ->
                val volunteerId = backStackEntry.arguments?.getString("volunteerId") ?: return@composable
                val requestId = backStackEntry.arguments?.getString("requestId") ?: return@composable
                val rContext = LocalContext.current
                val scope = rememberCoroutineScope()
                RateVolunteerScreen(
                    userRepository = userRepository,
                    volunteerId = volunteerId,
                    onHome = {
                        wardVm.clearWardRequestFlow()
                        navigateWardHome()
                    },
                    onSubmitRating = { score ->
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    userRepository.submitVolunteerRating(volunteerId, requestId, score)
                                }
                            }.onSuccess {
                                Toast.makeText(rContext, "Спасибо за оценку!", Toast.LENGTH_SHORT).show()
                                wardVm.clearWardRequestFlow()
                                navigateWardHome()
                            }.onFailure { e ->
                                Toast.makeText(
                                    rContext,
                                    mapVolunteerRatingSubmitError(e),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
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
    onOpenProfile: () -> Unit
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var userLocationLayer: UserLocationLayer? by remember { mutableStateOf(null) }
    var currentLocation: Point? by remember { mutableStateOf(null) }
    var initialCameraFixApplied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    val fallback = remember { Point(60.0092, 30.3578) }
    val pinBitmap: Bitmap? = remember {
        runCatching {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_pin) ?: return@runCatching null
            drawable.toBitmap(width = 96, height = 96, config = Bitmap.Config.ARGB_8888)
        }.getOrNull()
    }

    var markersCollection: MapObjectCollection? by remember { mutableStateOf(null) }
    DisposableEffect(mapView) {
        mapView.onStart()
        onDispose {
            markersCollection?.clear()
            markersCollection = null
            mapView.onStop()
        }
    }
    val selectRequest = rememberUpdatedState(onSelect)

    val fusedClient = remember { com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context) }

    val requestsMarkerKey = state.requests
        .map { "${it.id}:${it.status}" }
        .sorted()
        .joinToString("|")

    LaunchedEffect(mapView, state.nearbyEpoch, requestsMarkerKey) {
        val map = mapView.mapWindow.map
        if (markersCollection == null) {
            markersCollection = map.mapObjects.addCollection()
        }
        val coll = markersCollection ?: return@LaunchedEffect
        coll.clear()
        val icon = pinBitmap?.let { ImageProvider.fromBitmap(it) }
        state.requests.forEach { req ->
            val point = Point(req.location.latitude, req.location.longitude)
            val placemark = if (icon != null) {
                coll.addPlacemark(point, icon, IconStyle().apply { scale = 1.5f })
            } else {
                coll.addPlacemark(point)
            }
            placemark.addTapListener(MapObjectTapListener { _, _ ->
                selectRequest.value(req)
                true
            })
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        delay(1000)
        fun hasLocationPermission(): Boolean {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasLocationPermission()) {
            mapView.mapWindow.map.move(CameraPosition(fallback, 14.5f, 0f, 0f))
            initialCameraFixApplied = true
            return@LaunchedEffect
        }
        @Suppress("MissingPermission")
        val loc = try {
            val last = fusedClient.lastLocation.await()
            last ?: fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()
        } catch (_: Exception) {
            null
        }
        if (loc != null) {
            val p = Point(loc.latitude, loc.longitude)
            currentLocation = p
            mapView.mapWindow.map.move(CameraPosition(p, 14.5f, 0f, 0f))
            initialCameraFixApplied = true
        } else {
            mapView.mapWindow.map.move(CameraPosition(fallback, 14.5f, 0f, 0f))
        }
    }

    LaunchedEffect(currentLocation) {
        val p = currentLocation ?: return@LaunchedEffect
        if (!initialCameraFixApplied) {
            mapView.mapWindow.map.move(CameraPosition(p, 14.5f, 0f, 0f))
            initialCameraFixApplied = true
        }
    }

    LaunchedEffect(currentLocation) {
        while (true) {
            val p = currentLocation ?: fallback
            onRefresh(p.latitude, p.longitude)
            delay(5000)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = DobrovichekColors.BackgroundCream.copy(alpha = 0.95f),
                shadowElevation = 2.dp
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = DobrovichekColors.OrangeCoral,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            ) {
                                append("Добро")
                            }
                            withStyle(
                                SpanStyle(
                                    color = DobrovichekColors.BluePrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            ) {
                                append("вичок")
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp, end = 52.dp),
                        maxLines = 1
                    )
                    IconButton(
                        onClick = onOpenProfile,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Профиль",
                            tint = DobrovichekColors.NavyText
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { mapView },
                update = {
                    val map = mapView.mapWindow.map

                    val hasLocationPermission =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                    if (hasLocationPermission) {
                        if (userLocationLayer == null) {
                            val mapKit = MapKitFactory.getInstance()
                            userLocationLayer = mapKit.createUserLocationLayer(mapView.mapWindow)
                        }
                        userLocationLayer?.isVisible = true
                        @Suppress("MissingPermission")
                        fusedClient.lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) currentLocation = Point(loc.latitude, loc.longitude)
                        }
                    } else {
                        userLocationLayer?.isVisible = false
                    }
                }
            )

            FloatingActionButton(
                onClick = {
                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (fine || coarse) {
                        @Suppress("MissingPermission")
                        fusedClient.lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) {
                                val pt = Point(loc.latitude, loc.longitude)
                                currentLocation = pt
                                mapView.mapWindow.map.move(CameraPosition(pt, 15f, 0f, 0f))
                            }
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Моё местоположение")
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
private fun VolunteerSelfProfileScreen(
    volunteerId: String,
    userRepository: UserRepository,
    onClose: () -> Unit,
    onLogout: () -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var profile by remember { mutableStateOf<VolunteerProfileDto?>(null) }
    var history by remember { mutableStateOf<List<VolunteerRequestHistoryItemDto>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(volunteerId) {
        loading = true
        loadError = null
        runCatching {
            withContext(Dispatchers.IO) {
                val p = userRepository.getVolunteerProfile(volunteerId)
                val h = userRepository.getVolunteerRequestHistory(volunteerId)
                p to h
            }
        }.onSuccess { (p, h) ->
            profile = p
            history = h.sortedByDescending { it.completedAt ?: it.updatedAt ?: "" }
            loading = false
        }.onFailure {
            loadError = UserFacingErrors.networkOrHttp(it, "Не удалось загрузить профиль")
            loading = false
        }
    }

    DobrovichekWardBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = DobrovichekColors.NavyText)
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = DobrovichekColors.BluePrimary)
                        }
                    }
                    loadError != null -> {
                        Column(Modifier.padding(24.dp)) {
                            Text(loadError!!, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    else -> {
                        val p = profile!!
                        val first = p.firstName?.trim()?.takeIf { it.isNotEmpty() }
                        val last = p.lastName?.trim()?.takeIf { it.isNotEmpty() }
                        val fallbackName = p.fullName?.trim()?.takeIf { it.isNotEmpty() }
                        val nameOneLine = listOfNotNull(first, last)
                            .joinToString(" ")
                            .ifBlank { fallbackName ?: "Волонтёр" }
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text(
                                nameOneLine,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = DobrovichekColors.NavyText,
                                maxLines = 2
                            )
                            Spacer(Modifier.height(6.dp))
                            val ratingVal = p.rating?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "—"
                            val ratingSuffix = p.ratingCount?.takeIf { it > 0 }?.let { ", $it оц." }.orEmpty()
                            Text(
                                "Рейтинг: $ratingVal$ratingSuffix",
                                style = MaterialTheme.typography.titleMedium,
                                color = DobrovichekColors.GreySecondary
                            )
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "История выполненных заявок:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = DobrovichekColors.NavyText
                            )
                            Spacer(Modifier.height(12.dp))
                            if (history.isEmpty()) {
                                Text(
                                    "Пока нет завершённых заявок.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = DobrovichekColors.GreySecondary
                                )
                            } else {
                                history.forEach { row ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp),
                                        shape = DobrovichekCardShape,
                                        colors = CardDefaults.cardColors(
                                            containerColor = DobrovichekColors.BackgroundCream.copy(alpha = 0.65f)
                                        ),
                                        border = BorderStroke(1.dp, DobrovichekColors.CardBorderSubtle)
                                    ) {
                                        Column(Modifier.padding(14.dp)) {
                                            Text(
                                                "Заявка",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = DobrovichekColors.GreySecondary
                                            )
                                            Text(
                                                row.requestId,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = DobrovichekColors.NavyText
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "Категория: ${row.category?.takeIf { it.isNotBlank() } ?: "—"}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = DobrovichekColors.NavyText
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "Адрес: ${row.address?.takeIf { it.isNotBlank() } ?: "—"}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = DobrovichekColors.NavyText
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                if (row.wardRating != null) {
                                                    "Оценка: ${row.wardRating}"
                                                } else {
                                                    "Не оценена"
                                                },
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = DobrovichekColors.NavyText
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                "Завершена: ${formatVolunteerHistoryInstant(row.completedAt)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DobrovichekColors.GreySecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            TextButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("Выйти", color = DobrovichekColors.NavyText)
            }
        }
    }
}

private fun mapVolunteerRatingSubmitError(e: Throwable): String {
    fun map409Haystack(haystack: String): String = when {
        haystack.contains("already exists", ignoreCase = true) ->
            "Вы уже оценили эту заявку"
        haystack.contains("Rating can be left only for completed", ignoreCase = true) ->
            "Оценку можно оставить только после завершения заявки."
        haystack.contains("does not belong to the specified volunteer", ignoreCase = true) ->
            "Эта заявка не относится к выбранному волонтёру."
        haystack.contains("not found or access denied", ignoreCase = true) ->
            "Заявка недоступна. Попробуйте позже."
        haystack.contains("Cannot reach request-service", ignoreCase = true) ->
            UserFacingErrors.SERVICE_UNAVAILABLE
        haystack.contains("not found", ignoreCase = true) ->
            "Заявка недоступна. Попробуйте позже."
        else -> "Не удалось сохранить оценку. Попробуйте позже."
    }
    return when (e) {
        is VolunteerRatingHttpException -> when (e.statusCode) {
            409 -> {
                val extracted = runCatching {
                    org.json.JSONObject(e.errorBody).optString("message", "")
                }.getOrElse { "" }
                map409Haystack("${e.errorBody}\n$extracted")
            }
            in 500..599 -> UserFacingErrors.SERVICE_UNAVAILABLE
            else -> "Не удалось сохранить оценку. Попробуйте позже."
        }
        is HttpException -> when (e.code()) {
            409 -> map409Haystack(e.response()?.errorBody()?.use { it.string() }.orEmpty())
            403 -> "Оценку могут оставить только подопечные по своей заявке."
            400 -> "Проверьте оценку и попробуйте снова."
            in 500..599 -> UserFacingErrors.SERVICE_UNAVAILABLE
            else -> UserFacingErrors.networkOrHttp(e, "Не удалось сохранить оценку. Попробуйте позже.")
        }
        else -> UserFacingErrors.networkOrHttp(e, "Не удалось сохранить оценку. Попробуйте позже.")
    }
}

private fun formatVolunteerHistoryInstant(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    val noFrac = iso.substringBefore('.')
    return noFrac.replace('T', ' ').take(16)
}

@Composable
private fun VolunteerAcceptConfirmScreen(
    accepting: Boolean,
    error: String?,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    DobrovichekWardBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Подтверждение",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DobrovichekColors.NavyText
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Подтвердите, что вы готовы помочь. Только после этого подопечный увидит, что волонтёр нашёлся, и получит ваши контакты.",
                style = MaterialTheme.typography.bodyLarge,
                color = DobrovichekColors.GreySecondary
            )
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(28.dp))
            GradientPrimaryButton(
                text = "Да, я готов",
                onClick = onConfirm,
                enabled = !accepting,
                loading = accepting
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onBack,
                enabled = !accepting,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = DobrovichekCardShape,
                border = BorderStroke(1.dp, DobrovichekColors.CardBorderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DobrovichekColors.NavyText)
            ) {
                Text("Назад")
            }
        }
    }
}

@Composable
private fun VolunteerRequestDetailsScreen(
    state: VolunteerUiState,
    onAbandonHelp: () -> Unit
) {
    val context = LocalContext.current
    val details = state.acceptedDetails
    DobrovichekWardBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Вы оказываете помощь",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DobrovichekColors.NavyText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Заявка и способ связи",
                style = MaterialTheme.typography.bodyMedium,
                color = DobrovichekColors.GreySecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            when {
                state.isLoading && details == null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = DobrovichekCardShape,
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Загружаем заявку…", color = DobrovichekColors.GreySecondary)
                            Spacer(Modifier.height(12.dp))
                            CircularProgressIndicator(color = DobrovichekColors.BluePrimary)
                        }
                    }
                }
                details == null -> {
                    Text("Не удалось загрузить данные заявки", color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    val wardFull = details.wardFullName?.trim()?.takeIf { it.isNotEmpty() }
                        ?: PersonNameFormat.fullFormal(
                            details.wardFirstName.orEmpty(),
                            details.wardPatronymic,
                            details.wardLastName.orEmpty()
                        ).ifBlank { null }
                    val phone = details.contactPhone?.trim()?.takeIf { it.isNotEmpty() }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = DobrovichekCardShape,
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Подопечный", style = MaterialTheme.typography.bodyMedium, color = DobrovichekColors.GreySecondary)
                            Text(
                                text = wardFull ?: "Имя не указано",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = DobrovichekColors.NavyText
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Телефон", style = MaterialTheme.typography.bodyMedium, color = DobrovichekColors.GreySecondary)
                            Text(
                                text = phone ?: "Недоступно",
                                style = MaterialTheme.typography.titleMedium,
                                color = DobrovichekColors.NavyText
                            )
                            if (phone != null) {
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:$phone")
                                        }
                                        context.startActivity(dialIntent)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = DobrovichekCardShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = DobrovichekColors.MintBorder)
                                ) {
                                    Text("Позвонить", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            HorizontalDivider(color = DobrovichekColors.CardBorderSubtle)
                            Spacer(Modifier.height(14.dp))
                            Text("О заявке", style = MaterialTheme.typography.bodyMedium, color = DobrovichekColors.GreySecondary)
                            Text(
                                details.description ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = DobrovichekColors.NavyText
                            )
                        }
                    }
                    state.error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(20.dp))
                    SoftOrangeButton(
                        text = "Не смогу помочь",
                        onClick = onAbandonHelp,
                        enabled = !state.isLoading
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun VolunteerHelpDoneScreen(onFindMore: () -> Unit) {
    DobrovichekWardBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Помощь оказана!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DobrovichekColors.NavyText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Спасибо, что помогаете людям рядом",
                style = MaterialTheme.typography.bodyLarge,
                color = DobrovichekColors.GreySecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(36.dp))
            GradientPrimaryButton(text = "Найти другие заявки", onClick = onFindMore)
        }
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
            DobrovichekWardBackground {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Добровичок",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = DobrovichekColors.NavyText
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Выберите роль", style = MaterialTheme.typography.titleLarge, color = DobrovichekColors.GreySecondary)
                    Spacer(Modifier.height(24.dp))
                    GradientPrimaryButton(
                        text = "Я хочу получать оперативную помощь",
                        onClick = { onRegisterRoleChosen("WARD") }
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onRegisterRoleChosen("VOLUNTEER") },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = DobrovichekCardShape,
                        border = BorderStroke(1.dp, DobrovichekColors.BluePrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DobrovichekColors.BluePrimary)
                    ) {
                        Text("Я волонтёр и хочу помогать", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(32.dp))
                    HorizontalDivider(color = DobrovichekColors.CardBorderSubtle)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onToggleMode(false) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = DobrovichekCardShape,
                        border = BorderStroke(1.dp, DobrovichekColors.CardBorderSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DobrovichekColors.NavyText)
                    ) {
                        Text("У меня уже есть аккаунт")
                    }
                }
            }
        }
        else -> {
            val showRegisterFields = state.isRegisterMode && state.registerStep == AuthRegisterStep.ENTER_DETAILS
            DobrovichekWardBackground {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(88.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Добровичок",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = DobrovichekColors.NavyText
                )
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
}

@Composable
private fun HomeScreen(session: UserSession?, onCreate: () -> Unit, onLogout: () -> Unit) {
    val wardHeaderName = session?.let {
        PersonNameFormat.volunteerForWard(it.firstName, it.lastName).ifBlank { it.fullName }
    } ?: "Помощь рядом"
    DobrovichekWardBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = wardHeaderName,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = DobrovichekColors.NavyText,
                    maxLines = 2
                )
                TextButton(onClick = onLogout) {
                    Text("Выйти", color = DobrovichekColors.NavyText)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = null,
                        modifier = Modifier
                            .height(120.dp)
                            .fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = DobrovichekColors.OrangeCoral, fontWeight = FontWeight.Bold, fontSize = 32.sp)) {
                                append("Добро")
                            }
                            withStyle(SpanStyle(color = DobrovichekColors.BluePrimary, fontWeight = FontWeight.Bold, fontSize = 32.sp)) {
                                append("вичок")
                            }
                        },
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(36.dp))
                    GradientPrimaryButton(text = "Создать заявку", onClick = onCreate)
                }
            }
        }
    }
}

@Composable
private fun StepOneScreen(
    state: WardUiState,
    onCategory: (HelpCategory) -> Unit,
    onUrgency: (Urgency) -> Unit,
    onPreferredTimeChange: (String) -> Unit,
    onNext: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(is24Hour = true)
    DobrovichekWardBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                "Что Вам нужно?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DobrovichekColors.NavyText
            )
            Text(
                "Выберите категорию помощи",
                style = MaterialTheme.typography.bodyMedium,
                color = DobrovichekColors.GreySecondary
            )
            Spacer(Modifier.height(14.dp))
            HelpCategory.entries.forEach { category ->
                val (bg, accent) = category.palette()
                SelectCard(
                    title = category.title,
                    subtitle = category.subtitle(),
                    selected = state.category == category,
                    backgroundColor = bg,
                    accentBorderColor = accent,
                    onClick = { onCategory(category) }
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Когда нужно помочь?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = DobrovichekColors.NavyText
            )
            Spacer(Modifier.height(10.dp))
            Urgency.entries.forEach { urgency ->
                SelectCard(
                    title = urgency.title,
                    subtitle = null,
                    selected = state.urgency == urgency,
                    backgroundColor = urgency.rowBackground(),
                    accentBorderColor = urgency.accentBorder(),
                    onClick = { onUrgency(urgency) }
                )
            }
            if (state.urgency == Urgency.LATER) {
                Spacer(Modifier.height(14.dp))
                Text("Удобное время", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = DobrovichekCardShape,
                    border = BorderStroke(1.dp, DobrovichekColors.BluePrimary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DobrovichekColors.BluePrimary)
                ) {
                    Text(
                        if (state.preferredTime.isNotBlank()) "Время: ${state.preferredTime}"
                        else "Выбрать время"
                    )
                }
            }
            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onPreferredTimeChange(
                                    "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                                )
                                showTimePicker = false
                            }
                        ) { Text("Готово") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
                    },
                    title = { Text("Удобное время") },
                    text = { TimePicker(state = timePickerState) }
                )
            }
            Spacer(Modifier.height(28.dp))
            val canContinue = state.urgency != Urgency.LATER || state.preferredTime.isNotBlank()
            GradientPrimaryButton(text = "Далее", onClick = onNext, enabled = canContinue)
            Spacer(Modifier.height(16.dp))
        }
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

    DobrovichekWardBackground {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                "Проверьте Ваш адрес",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DobrovichekColors.NavyText
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Передвинте карту так, чтобы маркер указывал на Ваш адрес",
                style = MaterialTheme.typography.bodyMedium,
                color = DobrovichekColors.GreySecondary
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 320.dp)
                    .clip(DobrovichekCardShape)
                    .border(1.dp, DobrovichekColors.CardBorderSubtle, DobrovichekCardShape)
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
                Image(
                    painter = painterResource(id = R.drawable.ic_pin),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(66.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(state.address, style = MaterialTheme.typography.bodyLarge, color = DobrovichekColors.NavyText)
            Spacer(Modifier.height(16.dp))
            GradientPrimaryButton(
                text = "Далее",
                onClick = {
                    val center = mapView.mapWindow.map.cameraPosition.target
                    onAddressPointChange(center.latitude, center.longitude)
                    resolveAddress(context, center.latitude, center.longitude)?.let(onAddressTextChange)
                    onNext()
                }
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun resolveAddress(context: android.content.Context, latitude: Double, longitude: Double): String? {
    return runCatching {
        val geocoder = android.location.Geocoder(context, java.util.Locale("ru", "RU"))
        val results = geocoder.getFromLocation(latitude, longitude, 1) ?: return null
        val a = results.firstOrNull() ?: return null
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
    val (catBg, catBorder) = state.category.palette()
    val urgSub = if (state.urgency == Urgency.LATER && state.preferredTime.isNotBlank()) {
        "Время: ${state.preferredTime}"
    } else null
    DobrovichekWardBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                "Всё верно?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DobrovichekColors.NavyText
            )
            Text(
                "Проверьте заявку",
                style = MaterialTheme.typography.titleMedium,
                color = DobrovichekColors.GreySecondary
            )
            Spacer(Modifier.height(16.dp))
            SelectCard(
                title = state.category.title,
                subtitle = state.category.subtitle(),
                selected = true,
                backgroundColor = catBg,
                accentBorderColor = catBorder,
                onClick = {}
            )
            SelectCard(
                title = state.urgency.title,
                subtitle = urgSub,
                selected = true,
                backgroundColor = state.urgency.rowBackground(),
                accentBorderColor = state.urgency.accentBorder(),
                onClick = {}
            )
            Spacer(Modifier.height(12.dp))
            Text("Адрес", color = DobrovichekColors.GreySecondary, style = MaterialTheme.typography.bodyMedium)
            Text(state.address, style = MaterialTheme.typography.titleLarge, color = DobrovichekColors.NavyText)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = state.apartment,
                onValueChange = onApartmentChange,
                label = { Text("Квартира *") },
                modifier = Modifier.fillMaxWidth(),
                shape = DobrovichekCardShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DobrovichekColors.BluePrimary,
                    unfocusedBorderColor = DobrovichekColors.CardBorderSubtle
                )
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.comment,
                onValueChange = onCommentChange,
                label = { Text("Комментарий для волонтёра") },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                shape = DobrovichekCardShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DobrovichekColors.BluePrimary,
                    unfocusedBorderColor = DobrovichekColors.CardBorderSubtle
                )
            )
            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(20.dp))
            GradientPrimaryButton(
                text = "Создать заявку",
                onClick = onCreate,
                enabled = !state.isLoading && state.apartment.isNotBlank(),
                loading = state.isLoading
            )
            Spacer(Modifier.height(16.dp))
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
    DobrovichekWardBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Поиск волонтёра…",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DobrovichekColors.NavyText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Пожалуйста, подождите — мы ищем того, кто сможет вам помочь",
                style = MaterialTheme.typography.bodyLarge,
                color = DobrovichekColors.GreySecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            val mm = (seconds / 60).toString().padStart(2, '0')
            val ss = (seconds % 60).toString().padStart(2, '0')
            Text(
                "$mm:$ss",
                style = MaterialTheme.typography.headlineSmall,
                color = DobrovichekColors.GreySecondary.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator(color = DobrovichekColors.BluePrimary)
            Spacer(Modifier.height(40.dp))
            SoftOrangeButton(
                text = "Отменить заявку",
                onClick = onCancel,
                enabled = !state.isLoading
            )
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
    onCompleteHelp: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val nameOk = !volunteerName.isNullOrBlank()
    val phoneOk = !volunteerPhone.isNullOrBlank()
    DobrovichekWardBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Волонтер найден!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DobrovichekColors.NavyText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = DobrovichekCardShape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    when {
                        loadingContact -> {
                            Text("Загружаем контакты…", color = DobrovichekColors.GreySecondary)
                            Spacer(Modifier.height(8.dp))
                            CircularProgressIndicator(
                                Modifier.size(36.dp),
                                color = DobrovichekColors.BluePrimary
                            )
                        }
                        contactError != null -> {
                            Text(contactError, color = MaterialTheme.colorScheme.error)
                        }
                        else -> {
                            Text("Волонтёр", color = DobrovichekColors.GreySecondary, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (nameOk) volunteerName!! else "Имя не указано в профиле",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = DobrovichekColors.NavyText
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("Телефон", color = DobrovichekColors.GreySecondary, style = MaterialTheme.typography.bodyMedium)
                            if (phoneOk) {
                                Text(volunteerPhone!!, style = MaterialTheme.typography.titleMedium, color = DobrovichekColors.NavyText)
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${volunteerPhone}")
                                        }
                                        context.startActivity(dialIntent)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = DobrovichekCardShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = DobrovichekColors.MintBorder)
                                ) {
                                    Text("Позвонить", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Text("Не указан в профиле", style = MaterialTheme.typography.bodyLarge, color = DobrovichekColors.GreySecondary)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "${state.category.title} · ${state.address}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DobrovichekColors.GreySecondary
                    )
                }
            }
            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(22.dp))
            GradientPrimaryButton(
                text = "Мне помогли! Закрыть заявку",
                onClick = onCompleteHelp,
                enabled = !state.isLoading
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                enabled = !state.isLoading,
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = DobrovichekCardShape,
                border = BorderStroke(1.dp, DobrovichekColors.CardBorderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DobrovichekColors.NavyText)
            ) {
                Text("Отменить заявку")
            }
        }
    }
}

@Composable
private fun RateVolunteerScreen(
    userRepository: UserRepository,
    volunteerId: String,
    onHome: () -> Unit,
    onSubmitRating: (Int) -> Unit
) {
    var volunteerLabel by remember { mutableStateOf<String?>(null) }
    var score by remember { mutableStateOf(0) }
    LaunchedEffect(volunteerId) {
        volunteerLabel = withContext(Dispatchers.IO) {
            userRepository.getVolunteerFirstNameForRating(volunteerId)
        }
    }
    val nameForText = volunteerLabel?.trim()?.takeIf { it.isNotEmpty() } ?: "волонтёра"
    DobrovichekWardBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Помощь оказана",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DobrovichekColors.NavyText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Пожалуйста, оцените волонтёра $nameForText.",
                style = MaterialTheme.typography.bodyLarge,
                color = DobrovichekColors.NavyText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in 1..5) {
                    Text(
                        "★",
                        modifier = Modifier
                            .clickable { score = i }
                            .padding(horizontal = 6.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (i <= score) Color(0xFFFFB74D) else Color(0xFFE0E0E0)
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            GradientPrimaryButton(
                text = "Оценить",
                onClick = { onSubmitRating(score) },
                enabled = score in 1..5
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = DobrovichekCardShape,
                border = BorderStroke(1.dp, DobrovichekColors.CardBorderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DobrovichekColors.NavyText)
            ) {
                Text("На главную")
            }
        }
    }
}

@Composable
private fun SelectCard(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    backgroundColor: Color,
    accentBorderColor: Color,
    onClick: () -> Unit
) {
    val borderColor = if (selected) accentBorderColor else DobrovichekColors.CardBorderSubtle
    val borderWidth = if (selected) 2.dp else 1.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(DobrovichekCardShape)
            .border(borderWidth, borderColor, DobrovichekCardShape)
            .background(backgroundColor, DobrovichekCardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = DobrovichekColors.NavyText
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = DobrovichekColors.GreySecondary
                )
            }
        }
        if (selected) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentBorderColor
            )
        }
    }
}
