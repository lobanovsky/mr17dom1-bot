package handlers

import api.HousekprApi
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun Dispatcher.registerCarHandlers(
    api: HousekprApi,
    waitingForPlate: MutableSet<Long>,
    botScope: CoroutineScope,
    keyboardMain: KeyboardReplyMarkup
) {
    message {
        val chatId = message.chat.id
        val text = message.text ?: return@message

        when {
            text == "🚗 Распознать номер" -> {
                bot.sendMessage(ChatId.fromId(chatId), "Введите номер автомобиля (например, A123BC777):", replyMarkup = keyboardMain)
                waitingForPlate.add(chatId)
            }

            waitingForPlate.contains(chatId) -> {
                bot.sendMessage(ChatId.fromId(chatId), "🔍 Ищу информацию по номеру: $text...", replyMarkup = keyboardMain)

                botScope.launch {
                    val info = api.getOverview(text.lowercase())
                    val message = if (info != null) {
                        """
                                🚘 ${safe(info.carDescription)}
                                🔢 ${safe(info.carNumber)}
                                👤 ${safe(info.ownerName)}
                                🏠 ${safe(info.ownerRooms)}
                                📞 ${safe(info.phoneLabel)}: ${safe(info.phoneNumber)}
                                """.trimIndent()
                    } else {
                        "❌ Не удалось получить данные по номеру $text."
                    }
                    bot.sendMessage(ChatId.fromId(chatId), message, replyMarkup = keyboardMain)
                }
                waitingForPlate.remove(chatId)
            }
        }
    }
}

fun safe(value: String?, default: String = "—"): String = value?.takeIf { it.isNotBlank() } ?: default
