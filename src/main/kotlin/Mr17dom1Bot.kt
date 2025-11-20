import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun main() {
    val token = System.getenv("TELEGRAM_BOT_TOKEN") ?: error("Переменная окружения TELEGRAM_BOT_TOKEN не задана!")

    val apiHost = System.getenv("HOUSEKPR_HOST") ?: "http://example.com"
    val apiEmail = System.getenv("HOUSEKPR_EMAIL") ?: "e.lobanovsky@ya.ru"
    val apiPassword = System.getenv("HOUSEKPR_PASSWORD") ?: "w4H&FrDo5U"
    val api = HousekprApi(apiHost, apiEmail, apiPassword)

    //Храним состояния пользователей
    val waitingForPlate = mutableSetOf<Long>() // для авто
    val receiptStates = mutableMapOf<Long, ReceiptState>() // для квитанций

    //Клавиатура с кнопками
    val keyboardMain = KeyboardReplyMarkup(
        keyboard = listOf(
            listOf(KeyboardButton("🚗 Распознать номер")),
            listOf(KeyboardButton("📄 Скачать квитанцию"))
        ),
        resizeKeyboard = true,
        oneTimeKeyboard = false
    )

    val botScope = CoroutineScope(Dispatchers.IO)

    val bot = bot {
        this.token = token
        val telegramApi = TelegramApi(token)
        logger().info("mr17dom1-bot запущен")


        dispatch {

            // Обработка сообщений от пользователя
            message {
                val chatId = message.chat.id
                val text = message.text ?: return@message

                // ---- Обработка авто ----
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

                    text == "📄 Скачать квитанцию" -> {
                        receiptStates[chatId] = ReceiptState(step = ReceiptStep.SELECT_MONTH)
                        botScope.launch {
                            val months = api.getAvailableMonths()
                            val keyboardMonths = KeyboardReplyMarkup(
                                keyboard = months.map { listOf(KeyboardButton(it)) },
                                resizeKeyboard = true,
                                oneTimeKeyboard = true
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
                                        listOf(KeyboardButton("KV")),
                                        listOf(KeyboardButton("MM"))
                                    ),
                                    resizeKeyboard = true,
                                    oneTimeKeyboard = true
                                )
                                bot.sendMessage(ChatId.fromId(chatId), "Выберите тип: KV — квартира, MM — машиноместо", replyMarkup = keyboardType)
                            }

                            ReceiptStep.SELECT_TYPE -> {
                                if (text != "KV" && text != "MM") return@message
                                state.type = text
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

                                val (year, month) = state.month!!.split("-")
                                bot.sendMessage(ChatId.fromId(chatId), "📥 Скачиваем квитанцию...", replyMarkup = keyboardMain)

                                botScope.launch {
                                    val pdfFile = api.downloadReceiptPdf(year, month, state.type!!, number)
                                    if (pdfFile != null) {
                                        telegramApi.sendDocument(
                                            chatId = chatId,
                                            file = pdfFile,
                                            caption = "Квитанция ${state.type} №$number за $month.$year"
                                        )
                                        pdfFile.delete()
                                    } else {
                                        bot.sendMessage(ChatId.fromId(chatId), "❌ Не удалось скачать квитанцию", replyMarkup = keyboardMain)
                                    }
                                }

                                receiptStates.remove(chatId)
                            }

                            else -> {}
                        }
                    }

                    else -> {
                        bot.sendMessage(ChatId.fromId(chatId), "Выберите действие:", replyMarkup = keyboardMain)
                    }
                }
            }
        }
    }
    bot.startPolling()
}

fun safe(value: String?, default: String = "—"): String = value?.takeIf { it.isNotBlank() } ?: default