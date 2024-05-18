import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, onRegister: () -> Unit) {
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
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = { onLogin(username, password) }, modifier = Modifier.weight(1f)) {
                Text("Войти")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onRegister, modifier = Modifier.weight(1f)) {
                Text("Регистрация")
            }
        }
    }
}

@Composable
fun RegisterScreen(onRegister: (String, String) -> Unit, onBack: () -> Unit) {
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
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = { onRegister(username, password) }, modifier = Modifier.weight(1f)) {
                Text("Зарегистрироваться")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Назад")
            }
        }
    }
}

@Composable
fun HomePage(username: String, onLogout: () -> Unit) {
    var accounts by remember { mutableStateOf(listOf<Account>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = username) {
        try {
            println("Запуск загрузки данных...") // Печать перед началом загрузки
            accounts = Client.getAccounts(username)
            println("Данные загружены: $accounts") // Печать после загрузки данных
            isLoading = false
            println("Загрузка завершена") // Печать после завершения загрузки
        } catch (e: Exception) {
            println("Произошла ошибка при загрузке данных: ${e.message}")
            e.printStackTrace()
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Добро пожаловать, $username", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator() // Индикатор загрузки
            Spacer(modifier = Modifier.height(8.dp))
            Text("Загрузка данных...") // Сообщение о загрузке
        } else {
            if (accounts.isEmpty()) {
                Text("У вас пока нет счетов. Вы можете создать новый счет.")
            } else {
                accounts.forEach { account ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Номер счета: ${account.accountNumber}")
                            Text(text = "Баланс: ${account.balance}")
                            Text(text = "Тип счета: ${account.accountType}")
                            if (account.accountType == "FIXED") {
                                Text(text = "Дата погашения: ${account.maturityDate}")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onLogout) {
            Text("Выйти")
        }

    }
}

@Composable
fun App() {
    var loggedInUser by remember { mutableStateOf<String?>(null) }
    var isRegistering by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    MaterialTheme {
        if (loggedInUser == null) {
            if (isRegistering) {
                RegisterScreen(
                    onRegister = { username, password ->
                        coroutineScope.launch {
                            val registerResult = Client.register(username, password)
                            if (registerResult == "Успешная регистрация") {
                                isRegistering = false
                                errorMessage = null
                            } else {
                                errorMessage = registerResult
                            }
                        }
                    },
                    onBack = { isRegistering = false }
                )
            } else {
                LoginScreen(
                    onLogin = { username, password ->
                        coroutineScope.launch {
                            val loginResult = Client.login(username, password)
                            if (loginResult == "Успешный вход") {
                                loggedInUser = username
                                errorMessage = null
                            } else {
                                errorMessage = loginResult
                            }
                        }
                    },
                    onRegister = { isRegistering = true }
                )
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colors.error, modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            HomePage(username = loggedInUser!!, onLogout = { loggedInUser = null })
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}