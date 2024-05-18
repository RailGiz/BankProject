import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.sql.Connection
import java.sql.DriverManager
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

            var request: List<String>? = null
            do {
                request = reader.readLine()?.split(" ")
                if (request != null) {
                    if (request[0] == "REGISTER") {
                        val username = request[1]
                        val password = request[2]

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
            } while (request != null)
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


    private fun sendAccounts(username: String, writer: PrintWriter) {
        try {
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(
                """
                SELECT account_number, balance, account_type, maturity_date 
                FROM Accounts 
                WHERE user_id = (SELECT id FROM Users WHERE username = '$username')
                """
            )

            while (resultSet.next()) {
                val accountNumber = resultSet.getString("account_number")
                val balance = resultSet.getBigDecimal("balance")
                val accountType = resultSet.getString("account_type")
                val maturityDate = resultSet.getDate("maturity_date")?.toString()

                writer.println("$accountNumber $balance $accountType $maturityDate")
            }

            writer.println("END_OF_DATA")
        } catch (e: Exception) {
            println("Ошибка при отправке данных аккаунта: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun restartServer() {
        println("Перезапуск сервера через 5 секунд...")
        Thread.sleep(5000)
        start()
    }
}

fun main() {
    Server.start()
}