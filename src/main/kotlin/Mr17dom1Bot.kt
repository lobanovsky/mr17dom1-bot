import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import kotlinx.coroutines.runBlocking

fun main() {
    val token = System.getenv("TELEGRAM_BOT_TOKEN") ?: error("Переменная окружения TELEGRAM_BOT_TOKEN не задана!")

    val apiHost = System.getenv("HOUSEKPR_HOST") ?: "http://example.com"
    val apiEmail = System.getenv("HOUSEKPR_EMAIL") ?: "e.lobanovsky@ya.ru"
    val apiPassword = System.getenv("HOUSEKPR_PASSWORD") ?: "w4H&FrDo5U"
    val api = HousekprApi(apiHost, apiEmail, apiPassword)

    // Храним состояние пользователя (ожидаем ввод номера или нет)
    val waitingForPlate = mutableSetOf<Long>()

    val bot = bot {
        this.token = token
        logger().info("mr17dom1-bot car recognizer is running...")

        dispatch {
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
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = "🔍 Ищу информацию по номеру: $text..."
                        )

                        // ⚡️ Вызов API в корутине (runBlocking для простоты)
                        runBlocking {
                            val info = api.getOverview(text.lowercase())

                            if (info != null) {
                                val message = """
                                    🚘 ${safe(info.carDescription)}
                                    🔢 ${safe(info.carNumber)}
                                    👤 ${safe(info.ownerName)}
                                    🏠 ${safe(info.ownerRooms)}
                                    📞 ${safe(info.phoneLabel)}: ${safe(info.phoneNumber)}
                                """.trimIndent()

                                bot.sendMessage(ChatId.fromId(chatId), message)
                            } else {
                                bot.sendMessage(ChatId.fromId(chatId), "❌ Не удалось получить данные по номеру $text.")
                            }
                        }
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

fun safe(value: String?, default: String = "—"): String = value?.takeIf { it.isNotBlank() } ?: default
