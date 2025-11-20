import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

fun Dispatcher.commands(
    carCommandName: String,
    waitingForPlate: MutableSet<Long>,
    receiptStates: MutableMap<Long, ReceiptState>
) {

    // Команда старта
    command(carCommandName) {
        val chatId = message.chat.id
        val keyboard = KeyboardReplyMarkup(
            keyboard = listOf(
                listOf(KeyboardButton("🚗 Распознать номер")),
                listOf(KeyboardButton("📄 Скачать квитанцию")),
                listOf(KeyboardButton("🔄 Сброс"))
            ),
            resizeKeyboard = true,
            oneTimeKeyboard = false
        )

        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = "Привет! Я помогу узнать информацию по номеру автомобиля и скачать квитанции.\n\nВыберите действие 👇",
            replyMarkup = keyboard
        )
    }

    // ----------------------------
    // 🔄 Команда сброса состояний
    // ----------------------------
    command("reset") {
        val chatId = message.chat.id

        waitingForPlate.remove(chatId)
        receiptStates.remove(chatId)

        val keyboard = KeyboardReplyMarkup(
            keyboard = listOf(
                listOf(KeyboardButton("🚗 Распознать номер")),
                listOf(KeyboardButton("📄 Скачать квитанцию")),
                listOf(KeyboardButton("🔄 Сброс"))
            ),
            resizeKeyboard = true,
            oneTimeKeyboard = false
        )

        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = "🔄 Состояние сброшено!\nВыберите действие:",
            replyMarkup = keyboard
        )
    }
}
