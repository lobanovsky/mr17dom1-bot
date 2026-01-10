package handlers

import ReceiptState
import RoomType
import api.HousekprApi
import api.TelegramApi
import api.toTempFile
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import logger

fun Dispatcher.registerReceiptHandlers(
    houseApi: HousekprApi,
    telegramApi: TelegramApi,
    receiptStates: MutableMap<Long, ReceiptState>,
    botScope: CoroutineScope,
    keyboardMain: KeyboardReplyMarkup
) {
    message {
        val chatId = message.chat.id
        val text = message.text ?: return@message

        when {
            text == "📄 Скачать квитанцию" -> {
                receiptStates[chatId] = ReceiptState(step = ReceiptStep.SELECT_MONTH)
                botScope.launch {
                    val months = houseApi.getAvailableMonths()
                    if (months.isEmpty()) {
                        bot.sendMessage(ChatId.fromId(chatId), "❌ Нет загруженных квитанций.\nОбратитесь в ТСН.", replyMarkup = keyboardMain)
                        receiptStates.remove(chatId)
                        return@launch
                    }
                    val keyboardMonths = KeyboardReplyMarkup(
                        keyboard = months.map { listOf(KeyboardButton(it)) }, resizeKeyboard = true, oneTimeKeyboard = true
                    )
                    bot.sendMessage(ChatId.fromId(chatId), "Выберите месяц:", replyMarkup = keyboardMonths)
                }
            }

            receiptStates.containsKey(chatId) -> {
                val state = receiptStates[chatId]!!

                when (state.step) {
                    ReceiptStep.SELECT_MONTH -> {
                        state.month = text
                        state.step = ReceiptStep.SELECT_TYPE

                        val keyboardType = KeyboardReplyMarkup(
                            keyboard = listOf(
                                listOf(KeyboardButton(RoomType.FLAT.description)), listOf(KeyboardButton(RoomType.PARKING_SPACE.description))
                            ), resizeKeyboard = true, oneTimeKeyboard = true
                        )
                        bot.sendMessage(ChatId.fromId(chatId), "Выберите квартиру или машиноместо", replyMarkup = keyboardType)
                    }

                    ReceiptStep.SELECT_TYPE -> {
                        if (text != RoomType.FLAT.description && text != RoomType.PARKING_SPACE.description) return@message
                        state.roomType = RoomType.textToType(text)
                        state.step = ReceiptStep.SELECT_NUMBER

                        bot.sendMessage(ChatId.fromId(chatId), "Введите номер квартиры или машиноместа (1–144):")
                    }

                    ReceiptStep.SELECT_NUMBER -> {
                        val number = text.toIntOrNull()
                        if (number == null || number !in 1..144) {
                            bot.sendMessage(ChatId.fromId(chatId), "Неверный номер. Введите число от 1 до 144:")
                            return@message
                        }
                        state.number = number

                        val (year, month) = state.month.split("-")
                        bot.sendMessage(ChatId.fromId(chatId), "📥 Скачиваем квитанцию...", replyMarkup = keyboardMain)

                        botScope.launch {
                            val pdfData = houseApi.downloadReceiptPdf(year, month, state.roomType, number)

                            if (pdfData != null) {
                                val pdfFile = pdfData.toTempFile()

                                try {
                                    telegramApi.sendDocument(chatId = chatId, file = pdfFile, caption = "ЖКУ + Кап.ремонт. ${state.roomType.description} №$number за $year.$month")
                                } catch (e: Exception) {
                                    logger().info("❌ Ошибка отправки PDF в Telegram: ${e.message}")
                                    bot.sendMessage(ChatId.fromId(chatId), "❌ Не удалось отправить файл в Telegram.\nПопробуйте позже.", replyMarkup = keyboardMain)
                                } finally {
                                    pdfFile.delete() // удаляем файл
                                }

                                pdfFile.delete() // удаляем файл
                            } else {
                                bot.sendMessage(ChatId.fromId(chatId), "❌ Не удалось скачать квитанцию", replyMarkup = keyboardMain)
                            }
                        }

                        receiptStates.remove(chatId)
                    }

                    else -> {}
                }
            }
        }
    }
}

enum class ReceiptStep {
    NONE,
    SELECT_MONTH,
    SELECT_TYPE,
    SELECT_NUMBER
}
