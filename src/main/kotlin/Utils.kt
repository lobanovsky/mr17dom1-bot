import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.slf4j.Logger

suspend fun sendWithRetry(
    logger: Logger,
    operationName: String = "operation",
    retries: Int = 3,
    delayMs: Long = 1500,
    block: suspend () -> Unit
) {
    require(retries >= 1)

    repeat(retries) { attempt ->
        val attemptNumber = attempt + 1

        try {
            logger.info("▶️ $operationName — попытка $attemptNumber из $retries")
            block()
            logger.info("✅ $operationName — успешно с $attemptNumber попытки")
            return
        } catch (e: CancellationException) {
            logger.warn("⏹ $operationName — отменено (попытка $attemptNumber)")
            throw e
        } catch (e: Exception) {
            logger.warn(
                "⚠️ $operationName — ошибка на попытке $attemptNumber из $retries: ${e.message}"
            )

            if (attemptNumber == retries) {
                logger.error("❌ $operationName — все попытки исчерпаны", e)
                throw e
            }

            delay(delayMs)
        }
    }
}
