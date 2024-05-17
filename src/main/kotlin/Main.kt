import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Вход в банковское приложение", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Логин") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onLogin(username, password) }) {
            Text("Войти")
        }
    }
}

@Composable
fun HomePage(username: String) {
    var accounts by remember { mutableStateOf(listOf<Account>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = username) {
        println("Запуск загрузки данных...") // Печать перед началом загрузки
        accounts = Client.getAccounts(username)
        println("Данные загружены: $accounts") // Печать после загрузки данных
        isLoading = false
        println("Загрузка завершена") // Печать после завершения загрузки
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Добро пожаловать, $username", style = MaterialTheme.typography.h5)

        if (isLoading) {
            CircularProgressIndicator() // Индикатор загрузки
            Spacer(modifier = Modifier.height(8.dp))
            Text("Загрузка данных...") // Сообщение о загрузке
        } else {
            if (accounts.isEmpty()) {
                Text("У вас пока нет счетов. Вы можете создать новый счет.")
            } else {
                accounts.forEach { account ->
                    Text(text = "Номер счета: ${account.accountNumber}")
                    Text(text = "Баланс: ${account.balance}")
                    Text(text = "Тип счета: ${account.accountType}")
                    if (account.accountType == "FIXED") {
                        Text(text = "Дата погашения: ${account.maturityDate}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(onRegister: (String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Регистрация в банковском приложении", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Логин") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onRegister(username, password) }) {
            Text("Зарегистрироваться")
        }
    }
}

@Composable
fun App() {
    var loggedInUser by remember { mutableStateOf<String?>(null) }
    var isRegistering by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    if (loggedInUser == null) {
        if (isRegistering) {
            RegisterScreen { username, password ->
                coroutineScope.launch {
                    val registerResult = Client.register(username, password)
                    if (registerResult == "Успешная регистрация") {
                        isRegistering = false
                        errorMessage = null
                    } else {
                        errorMessage = registerResult
                    }
                }
            }
        } else {
            LoginScreen { username, password ->
                coroutineScope.launch {
                    val loginResult = Client.login(username, password)
                    if (loginResult == "Успешный вход") {
                        loggedInUser = username
                        errorMessage = null
                    } else {
                        errorMessage = loginResult
                    }
                }
            }
            Button(onClick = { isRegistering = true }) {
                Text("Регистрация")
            }
        }
        errorMessage?.let {
            Text(it, color = MaterialTheme.colors.error)
        }
    } else {
        HomePage(loggedInUser!!)
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
