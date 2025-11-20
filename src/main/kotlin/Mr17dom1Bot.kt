import api.HousekprApi
import api.TelegramApi
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.entities.BotCommand
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import commands.commands
import handlers.registerCarHandlers
import handlers.registerReceiptHandlers
import handlers.registerResetHandlers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


fun main() {
    val token = System.getenv("TELEGRAM_BOT_TOKEN") ?: error("Переменная окружения TELEGRAM_BOT_TOKEN не задана!")

    //house api
    val apiHost = System.getenv("HOUSEKPR_HOST") ?: "http://localhost:8088"
    val apiEmail = System.getenv("HOUSEKPR_EMAIL") ?: "e.lobanovsky@ya.ru"
    val apiPassword = System.getenv("HOUSEKPR_PASSWORD") ?: "w4H&FrDo5U"
    val houseApi = HousekprApi(apiHost, apiEmail, apiPassword)

    //telegram api
    val telegramApi = TelegramApi(token)

    //Храним состояния пользователей
    val waitingForPlate = mutableSetOf<Long>() // для авто
    val receiptStates = mutableMapOf<Long, ReceiptState>() // для квитанций

    //Клавиатура с кнопками
    val keyboard = KeyboardReplyMarkup(
        keyboard = listOf(
            listOf(KeyboardButton("🚗 Распознать номер")),
            listOf(KeyboardButton("📄 Скачать квитанцию")),
        ),
        resizeKeyboard = true,
        oneTimeKeyboard = false
    )

    val resetCommandName = "reset"

    val botScope = CoroutineScope(Dispatchers.IO)

    val bot = bot {
        this.token = token
        logger().info("mr17dom1-bot запущен")

        dispatch {
            registerCarHandlers(houseApi, waitingForPlate, botScope, keyboard)
            registerReceiptHandlers(houseApi, telegramApi, receiptStates, botScope, keyboard)
            registerResetHandlers(waitingForPlate, receiptStates, keyboard)

            commands(resetCommandName, waitingForPlate, receiptStates, keyboard)
        }
    }

    // Устанавливаем список команд, чтобы они отображались в Telegram
    bot.setMyCommands(
        listOf(
            BotCommand(resetCommandName, "Сбросить состояние"),
        )
    )

    bot.startPolling()
}
