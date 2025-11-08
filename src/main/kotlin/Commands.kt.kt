import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton


fun Dispatcher.commands(
    carCommandName: String,
) {

    command(carCommandName) {
        val chatId = message.chat.id
        val keyboard = KeyboardReplyMarkup(
            keyboard = listOf(
                listOf(KeyboardButton("🚗 Распознать номер"))
            ),
            resizeKeyboard = true, // адаптировать под экран
            oneTimeKeyboard = false // не скрывать после нажатия
        )

        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = "Привет! Я помогу узнать информацию по номеру автомобиля.\n" +
                    "Нажми кнопку ниже, чтобы начать 👇",
            replyMarkup = keyboard
        )
    }
}