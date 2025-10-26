import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.jvm.java

inline fun <reified T> T.logger(): Logger =
    LoggerFactory.getLogger(if (T::class.isCompanion) T::class.java.enclosingClass else T::class.java)
