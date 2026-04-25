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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ru.dobrovichek.android.data.AuthRepository
import ru.dobrovichek.android.data.RequestRepository
import ru.dobrovichek.android.data.UserSession

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
    val apartment: String = "106",
    val comment: String = "",
    val createdRequestId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class WardViewModel(
    private val repository: RequestRepository
) : ViewModel() {
    private val _state = MutableStateFlow(WardUiState())
    val state: StateFlow<WardUiState> = _state.asStateFlow()

    fun chooseCategory(category: HelpCategory) = _state.update { it.copy(category = category) }
    fun chooseUrgency(urgency: Urgency) = _state.update { it.copy(urgency = urgency) }
    fun updateApartment(value: String) = _state.update { it.copy(apartment = value) }
    fun updateComment(value: String) = _state.update { it.copy(comment = value) }

    fun createRequest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.createRequest(
                    category = state.value.category.title,
                    urgency = state.value.urgency.title,
                    address = state.value.address,
                    apartment = state.value.apartment,
                    comment = state.value.comment
                )
            }.onSuccess { requestId ->
                _state.update { it.copy(createdRequestId = requestId, isLoading = false) }
                onSuccess()
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message ?: "Ошибка сети") }
            }
        }
    }

    fun cancelRequest(onCancelled: () -> Unit) {
        val requestId = state.value.createdRequestId ?: return
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

data class AuthUiState(
    val fullName: String = "",
    val phone: String = "",
    val password: String = "",
    val role: String = "WARD",
    val isRegisterMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val session: UserSession? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState(session = authRepository.currentSession()))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun setRegisterMode(enabled: Boolean) = _state.update { it.copy(isRegisterMode = enabled, error = null) }
    fun updateFullName(value: String) = _state.update { it.copy(fullName = value) }
    fun updatePhone(value: String) = _state.update { it.copy(phone = value) }
    fun updatePassword(value: String) = _state.update { it.copy(password = value) }
    fun updateRole(value: String) = _state.update { it.copy(role = value) }

    fun submit() {
        val validationError = validateAuthInput(state.value)
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val authAction: suspend () -> UserSession = if (state.value.isRegisterMode) {
                {
                    authRepository.register(
                        fullName = state.value.fullName.trim(),
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

    private fun validateAuthInput(state: AuthUiState): String? {
        if (state.isRegisterMode && state.fullName.isBlank()) {
            return "Введите ФИО"
        }
        if (!state.phone.matches(Regex("^[0-9+() -]{7,20}$"))) {
            return "Введите корректный телефон (от 7 символов, можно +7...)"
        }
        if (state.password.length < 3) {
            return "Пароль должен быть не короче 3 символов"
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

class AppViewModelFactory(
    private val authRepository: AuthRepository,
    private val requestRepository: RequestRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepository) as T
        }
        if (modelClass.isAssignableFrom(WardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WardViewModel(requestRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun WardApp(authRepository: AuthRepository, requestRepository: RequestRepository) {
    val factory = AppViewModelFactory(authRepository, requestRepository)
    val authVm: AuthViewModel = viewModel(factory = factory)
    val wardVm: WardViewModel = viewModel(factory = factory)
    val authState by authVm.state.collectAsStateWithLifecycle()
    if (authState.session == null) {
        AuthScreen(
            state = authState,
            onFullNameChange = authVm::updateFullName,
            onPhoneChange = authVm::updatePhone,
            onPasswordChange = authVm::updatePassword,
            onRoleChange = authVm::updateRole,
            onToggleMode = authVm::setRegisterMode,
            onSubmit = authVm::submit
        )
    } else {
        val wardState by wardVm.state.collectAsStateWithLifecycle()
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "home") {
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
                    onApartmentChange = wardVm::updateApartment,
                    onNext = { navController.navigate("confirm") }
                )
            }
            composable("confirm") {
                ConfirmScreen(
                    state = wardState,
                    onCommentChange = wardVm::updateComment,
                    onCreate = {
                        wardVm.createRequest { navController.navigate("searching") }
                    }
                )
            }
            composable("searching") {
                SearchingScreen(
                    state = wardState,
                    onReady = { navController.navigate("found") },
                    onCancel = { wardVm.cancelRequest { navController.navigate("home") } }
                )
            }
            composable("found") {
                VolunteerFoundScreen(
                    state = wardState,
                    onCancel = { wardVm.cancelRequest { navController.navigate("home") } }
                )
            }
        }
    }
}

@Composable
private fun AuthScreen(
    state: AuthUiState,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onToggleMode: (Boolean) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Добровичок", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(if (state.isRegisterMode) "Регистрация" else "Вход", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        if (state.isRegisterMode) {
            OutlinedTextField(
                value = state.fullName,
                onValueChange = onFullNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ФИО") }
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
            label = { Text("Пароль") }
        )
        if (state.isRegisterMode) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onRoleChange("WARD") },
                    modifier = Modifier.weight(1f)
                ) { Text(if (state.role == "WARD") "✓ Подопечный" else "Подопечный") }
                Button(
                    onClick = { onRoleChange("VOLUNTEER") },
                    modifier = Modifier.weight(1f)
                ) { Text(if (state.role == "VOLUNTEER") "✓ Волонтёр" else "Волонтёр") }
            }
        }
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
            Text(if (state.isRegisterMode) "Зарегистрироваться" else "Войти")
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onToggleMode(!state.isRegisterMode) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.isRegisterMode) "У меня уже есть аккаунт" else "Создать новый аккаунт")
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
            session?.let { "${it.fullName} (${it.role})" } ?: "Помощь рядом",
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
private fun AddressScreen(state: WardUiState, onApartmentChange: (String) -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Проверьте Ваш адрес:", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(state.address, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = state.apartment,
            onValueChange = onApartmentChange,
            label = { Text("Номер квартиры") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Далее") }
    }
}

@Composable
private fun ConfirmScreen(
    state: WardUiState,
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
        Text("По адресу", color = Color.Gray)
        Text("${state.address}, кв. ${state.apartment}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.comment,
            onValueChange = onCommentChange,
            label = { Text("Комментарий к заявке") },
            modifier = Modifier.fillMaxWidth().height(160.dp)
        )
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.weight(1f))
        Button(
            enabled = !state.isLoading,
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
private fun SearchingScreen(state: WardUiState, onReady: () -> Unit, onCancel: () -> Unit) {
    var seconds by remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (seconds < 8) {
            delay(1000)
            seconds++
        }
        onReady()
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Поиск волонтера...", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Пожалуйста, подождите пока мы найдем того, кто сможет Вам помочь")
        Spacer(Modifier.height(12.dp))
        Text("00:${seconds.toString().padStart(2, '0')}", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator()
        Spacer(Modifier.height(48.dp))
        Button(enabled = !state.isLoading, onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Отменить заявку")
        }
    }
}

@Composable
private fun VolunteerFoundScreen(state: WardUiState, onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Волонтер найден и уже спешит к Вам!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Александр", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
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
