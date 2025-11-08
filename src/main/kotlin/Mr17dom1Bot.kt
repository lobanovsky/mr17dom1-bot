import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId

fun main() {
    val token = System.getenv("TELEGRAM_BOT_TOKEN") ?: error("Переменная окружения TELEGRAM_BOT_TOKEN не задана!")

    val carCommandName = "start"

    // Состояние: какие чаты ждут ввода номера авто
    val awaitingCarNumber = mutableSetOf<Long>()

//    val apiEmail = System.getenv("HOUSEKPR_EMAIL") ?: "e.lobanovsky@ya.ru"
//    val apiPassword = System.getenv("HOUSEKPR_PASSWORD") ?: "w4H&FrDo5U"
//    val api = HousekprApi()

    val bot = bot {
        this.token = token
        logger().info("mr17dom1-bot is running...")

        // Храним состояние пользователя (ожидаем ввод номера или нет)
        val waitingForPlate = mutableSetOf<Long>()

        dispatch {
            commands(carCommandName)

            // Обработка сообщений от пользователя
            message {
                val chatId = message.chat.id
                val text = message.text ?: return@message

                when {
                    text == "🚗 Распознать номер" -> {
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = "Введите номер автомобиля (например, A123BC777):"
                        )
                        waitingForPlate.add(chatId)
                    }

                    waitingForPlate.contains(chatId) -> {
                        // Здесь потом будет вызов API
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = "🔍 Ищу информацию по номеру: $text..."
                        )

                        // Имитация ответа от API
                        val fakeOwnerInfo = """
                        🚘 Номер: $text
                        👤 Собственник: Иванов Иван Иванович
                        📍 Регион: Москва
                        📅 Год выпуска: 2019
                    """.trimIndent()

                        bot.sendMessage(chatId = ChatId.fromId(chatId), text = fakeOwnerInfo)

                        waitingForPlate.remove(chatId)
                    }

                    else -> {
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = "Чтобы начать, нажми «🚗 Распознать номер»."
                        )
                    }
                }
            }
        }
    }
    bot.startPolling()
}
