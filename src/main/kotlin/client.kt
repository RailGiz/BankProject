import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.math.BigDecimal
import java.time.LocalDate


object Client {
    private lateinit var socket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: PrintWriter

    init {
        connect()
    }

    private fun connect() {
        try {
            socket = Socket("localhost", 8080)
            reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            writer = PrintWriter(socket.getOutputStream(), true)
        } catch (e: Exception) {
            println("Ошибка при подключении к серверу: ${e.message}")
            e.printStackTrace()
        }
    }

    fun login(username: String, password: String): String {
        return try {
            writer.println("$username $password")
            val response = reader.readLine()
            println("Ответ сервера на запрос входа: $response")
            response
        } catch (e: Exception) {
            println("Произошла ошибка при входе: ${e.message}")
            e.printStackTrace()
            "Ошибка"
        }
    }

    fun getAccounts(username: String): List<Account> {
        return try {
            writer.println(username)
            val accounts = mutableListOf<Account>()
            var line = reader.readLine()
            while (line != null && line != "END_OF_DATA") {
                val accountData = line.split(" ")
                if (accountData.size >= 3) {
                    val account = Account(
                        accountNumber = accountData[0],
                        balance = BigDecimal(accountData[1]),
                        accountType = accountData[2],
                        maturityDate = if (accountData.size > 3 && accountData[3] != "null") LocalDate.parse(accountData[3]) else null
                    )
                    accounts.add(account)
                }
                line = reader.readLine()
            }
            accounts
        } catch (e: Exception) {
            println("Произошла ошибка при получении аккаунтов: ${e.message}")
            e.printStackTrace()
            listOf()
        }
    }

    fun register(username: String, password: String): String {
        return try {
            writer.println("REGISTER $username $password")
            val response = reader.readLine()
            println("Ответ сервера на запрос регистрации: $response")
            response
        } catch (e: Exception) {
            println("Произошла ошибка при регистрации: ${e.message}")
            e.printStackTrace()
            "Ошибка"
        }
    }
}

fun main() {
    // Вход в систему
    val loginResponse = Client.login("user1", "password1")
    if (loginResponse != "Ошибка") {
        // Получение аккаунтов
        val accounts = Client.getAccounts("user1")
        if (accounts.isEmpty()) {
            println("Не получено ни одного аккаунта")
        } else {
            for (account in accounts) {
                println("Номер счета: ${account.accountNumber}, Баланс: ${account.balance}, Тип счета: ${account.accountType}, Дата погашения: ${account.maturityDate}")
            }
        }
    } else {
        println("Не удалось войти в систему")
    }
}