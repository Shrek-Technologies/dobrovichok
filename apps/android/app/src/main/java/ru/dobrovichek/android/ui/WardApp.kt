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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.dobrovichek.android.data.RequestRepository

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

class WardViewModelFactory(
    private val repository: RequestRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun WardApp(repository: RequestRepository) {
    val navController = rememberNavController()
    val vm: WardViewModel = viewModel(factory = WardViewModelFactory(repository))
    val state by vm.state.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onCreate = { navController.navigate("step1") })
        }
        composable("step1") {
            StepOneScreen(
                state = state,
                onCategory = vm::chooseCategory,
                onUrgency = vm::chooseUrgency,
                onNext = { navController.navigate("address") }
            )
        }
        composable("address") {
            AddressScreen(
                state = state,
                onApartmentChange = vm::updateApartment,
                onNext = { navController.navigate("confirm") }
            )
        }
        composable("confirm") {
            ConfirmScreen(
                state = state,
                onCommentChange = vm::updateComment,
                onCreate = {
                    vm.createRequest { navController.navigate("searching") }
                }
            )
        }
        composable("searching") {
            SearchingScreen(
                state = state,
                onReady = { navController.navigate("found") },
                onCancel = { vm.cancelRequest { navController.navigate("home") } }
            )
        }
        composable("found") {
            VolunteerFoundScreen(
                state = state,
                onCancel = { vm.cancelRequest { navController.navigate("home") } }
            )
        }
    }
}

@Composable
private fun HomeScreen(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Добровичок", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Помощь рядом", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
            Text("Создать заявку")
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
