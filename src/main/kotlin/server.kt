import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.math.BigDecimal
import kotlin.concurrent.thread

object Server {
    private lateinit var serverSocket: ServerSocket
    private lateinit var connection: Connection

    fun start() {
        try {
            serverSocket = ServerSocket(8080)
            connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/Bank", "root", "C37F31A388E2F99561BCEB136A3F534D596B6D77B5BBE7236FC17A6E86361ABD"
            )
            println("Сервер запущен и ожидает подключения...")

            while (true) {
                val clientSocket = serverSocket.accept()
                thread {
                    handleClient(clientSocket)
                }
            }
        } catch (e: Exception) {
            println("Ошибка сервера: ${e.message}")
            e.printStackTrace()
            restartServer()
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val request = line?.split(" ") ?: continue

                when (request[0]) {
                    "REGISTER" -> handleRegister(request, writer)
                    "LOGIN" -> handleLogin(request, writer)
                    "ACCOUNTS" -> handleGetAccounts(request, writer)
                    "TRANSFER" -> handleTransfer(request, writer)
                }
            }
        } catch (e: Exception) {
            println("Ошибка при обработке клиента: ${e.message}")
            e.printStackTrace()
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                println("Ошибка при закрытии сокета: ${e.message}")
            }
        }
    }


    private fun handleRegister(request: List<String>, writer: PrintWriter) {
        val username = request[1]
        val password = hashPassword(request[2])

        val statement = connection.prepareStatement(
            "INSERT INTO Users (username, password) VALUES (?, ?)"
        )
        statement.setString(1, username)
        statement.setString(2, password)

        val updateCount = statement.executeUpdate()

        if (updateCount > 0) {
            writer.println("Успешная регистрация")
        } else {
            writer.println("Ошибка регистрации")
        }
    }

    private fun handleLogin(request: List<String>, writer: PrintWriter) {
        val username = request[1]
        val password = request[2]

        val statement = connection.prepareStatement(
            "SELECT password FROM Users WHERE username = ?"
        )
        statement.setString(1, username)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            val storedPassword = resultSet.getString("password")
            if (checkPassword(password, storedPassword)) {
                writer.println("Успешный вход")
                sendAccounts(username, writer)
            } else {
                writer.println("Неверный логин или пароль")
            }
        } else {
            writer.println("Неверный логин или пароль")
        }
    }

    private fun handleGetAccounts(request: List<String>, writer: PrintWriter) {
        val username = request[1]
        sendAccounts(username, writer)
    }

    private fun handleTransfer(request: List<String>, writer: PrintWriter) {
        val fromAccount = request[1]
        val toAccount = request[2]
        val amount = BigDecimal(request[3])

        connection.autoCommit = false

        try {
            val withdrawStatement = connection.prepareStatement(
                "UPDATE Accounts SET balance = balance - ? WHERE account_number = ? AND balance >= ?"
            )
            withdrawStatement.setBigDecimal(1, amount)
            withdrawStatement.setString(2, fromAccount)
            withdrawStatement.setBigDecimal(3, amount)
            val withdrawUpdateCount = withdrawStatement.executeUpdate()

            if (withdrawUpdateCount == 0) {
                writer.println("Недостаточно средств или неверный номер счета")
                connection.rollback()
                return
            }

            val depositStatement = connection.prepareStatement(
                "UPDATE Accounts SET balance = balance + ? WHERE account_number = ?"
            )
            depositStatement.setBigDecimal(1, amount)
            depositStatement.setString(2, toAccount)
            val depositUpdateCount = depositStatement.executeUpdate()

            if (depositUpdateCount == 0) {
                writer.println("Неверный номер счета получателя")
                connection.rollback()
                return
            }

            val transactionStatement = connection.prepareStatement(
                "INSERT INTO Transactions (from_account, to_account, amount) VALUES (?, ?, ?)"
            )
            transactionStatement.setString(1, fromAccount)
            transactionStatement.setString(2, toAccount)
            transactionStatement.setBigDecimal(3, amount)
            transactionStatement.executeUpdate()

            connection.commit()
            writer.println("Перевод успешен")
        } catch (e: Exception) {
            connection.rollback()
            writer.println("Ошибка перевода: ${e.message}")
            e.printStackTrace()
        } finally {
            connection.autoCommit = true
        }
    }

    private fun sendAccounts(username: String, writer: PrintWriter) {
        val statement = connection.prepareStatement(
            "SELECT account_number, balance, account_type, maturity_date " +
                    "FROM Accounts WHERE user_id = (SELECT id FROM Users WHERE username = ?)"
        )
        statement.setString(1, username)
        val resultSet = statement.executeQuery()

        while (resultSet.next()) {
            val accountNumber = resultSet.getString("account_number")
            val balance = resultSet.getBigDecimal("balance")
            val accountType = resultSet.getString("account_type")
            val maturityDate = resultSet.getDate("maturity_date")?.toString()

            writer.println("$accountNumber $balance $accountType $maturityDate")
        }

        writer.println("END_OF_DATA")
    }

    private fun restartServer() {
        println("Перезапуск сервера через 5 секунд...")
        Thread.sleep(5000)
        start()
    }

    private fun hashPassword(password: String): String {
        // Реализуйте хеширование пароля здесь, например, с использованием BCrypt
        return password // Замените на реальную хеш-функцию
    }

    private fun checkPassword(password: String, hashedPassword: String): Boolean {
        // Реализуйте проверку пароля здесь, например, с использованием BCrypt
        return password == hashedPassword // Замените на реальную проверку хеша
    }
}

fun main() {
    Server.start()
}
