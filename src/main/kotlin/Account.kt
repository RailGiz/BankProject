import java.math.BigDecimal
import java.time.LocalDate

data class Account(
    val accountNumber: String,
    val balance: BigDecimal,
    val accountType: String,
    val maturityDate: LocalDate? = null
)
