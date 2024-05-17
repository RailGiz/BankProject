import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.math.BigDecimal
import java.time.LocalDate

object Client {
    private fun createSocket(): Socket {
        return Socket("localhost", 8080)
    }

    fun login(username: String, password: String): String {
        return try {
            createSocket().use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                writer.println("$username $password")
                val response = reader.readLine()
                println("Ответ сервера на запрос входа: $response")
                response
            }
        } catch (e: Exception) {
            println("Произошла ошибка при входе: ${e.message}")
            "Ошибка"
        }
    }

    fun getAccounts(username: String): List<Account> {
        return try {
            createSocket().use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                writer.println(username)
                val accounts = mutableListOf<Account>()
                var line = reader.readLine()
                while (line != null) {
                    if (line == "END_OF_DATA") {
                        break
                    }
                    println("Получена строка данных аккаунта: $line")
                    val accountData = line.split(" ")
                    if (accountData.size < 3) {
                        println("Неправильный формат данных аккаунта: $line")
                    } else {
                        val account = Account(
                            accountNumber = accountData[0],
                            balance = BigDecimal(accountData[1]),
                            accountType = accountData[2],
                            maturityDate = if (accountData.size > 3 && accountData[3] != "null") LocalDate.parse(accountData[3]) else null
                        )
                        accounts.add(account)
                        println("Добавлен аккаунт: $account")
                    }
                    line = reader.readLine()
                }
                println("Всего аккаунтов получено: ${accounts.size}")
                accounts
            }
        } catch (e: Exception) {
            println("Произошла ошибка при получении аккаунтов: ${e.message}")
            listOf()
        }
    }

    fun register(username: String, password: String): String {
        return try {
            createSocket().use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                writer.println("REGISTER $username $password")
                val response = reader.readLine()
                println("Ответ сервера на запрос регистрации: $response")
                response
            }
        } catch (e: Exception) {
            println("Произошла ошибка при регистрации: ${e.message}")
            "Ошибка"
        }
    }
}

fun main() {
    // Вход в систему
    val loginResponse = Client.login("testreg", "testreg")
    if (loginResponse != "Ошибка") {
        // Получение аккаунтов
        val accounts = Client.getAccounts("testreg")
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
