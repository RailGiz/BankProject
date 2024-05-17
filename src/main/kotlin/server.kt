import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.sql.Connection
import java.sql.DriverManager

object Server {
    private val serverSocket = ServerSocket(8080)
    private val connection: Connection = DriverManager.getConnection(
        "jdbc:mysql://localhost:3306/Bank", "root", "C37F31A388E2F99561BCEB136A3F534D596B6D77B5BBE7236FC17A6E86361ABD"
    )

    fun start() {
        println("Сервер запущен и ожидает подключения...")

        while (true) {
            val clientSocket = serverSocket.accept()
            handleClient(clientSocket)
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use {
            val reader = BufferedReader(InputStreamReader(it.getInputStream()))
            val writer = PrintWriter(it.getOutputStream(), true)

            val request = reader.readLine().split(" ")
            if (request[0] == "REGISTER") {
                val username = request[1]
                val password = request[2]

                // Добавление нового пользователя в базу данных
                val statement = connection.createStatement()
                val updateCount = statement.executeUpdate(
                    "INSERT INTO Users (username, password) VALUES ('$username', '$password')"
                )

                if (updateCount > 0) {
                    writer.println("Успешная регистрация")
                } else {
                    writer.println("Ошибка регистрации")
                }
            } else {
                val username = request[0]
                val password = request[1]

                // Проверка логина и пароля, используя базу данных
                val statement = connection.createStatement()
                val resultSet = statement.executeQuery(
                    "SELECT * FROM Users WHERE username = '$username' AND password = '$password'"
                )

                if (resultSet.next()) {
                    writer.println("Успешный вход")
                    sendAccounts(username, writer)
                } else {
                    writer.println("Неверный логин или пароль")
                }
            }
        }
    }

    private fun sendAccounts(username: String, writer: PrintWriter) {
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT * FROM Accounts WHERE user_id = (SELECT id FROM Users WHERE username = '$username')"
        )

        while (resultSet.next()) {
            val accountNumber = resultSet.getString("account_number")
            val balance = resultSet.getBigDecimal("balance")
            val accountType = resultSet.getString("account_type")
            val maturityDate = resultSet.getDate("maturity_date")?.toString()

            writer.println("$accountNumber $balance $accountType $maturityDate")
        }

        writer.println("END_OF_DATA")
    }
}

fun main() {
    Server.start()
}
