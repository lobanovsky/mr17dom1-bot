package handlers

import ReceiptState
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup

fun Dispatcher.registerResetHandlers(
    waitingForPlate: MutableSet<Long>,
    receiptStates: MutableMap<Long, ReceiptState>,
    keyboardMain: KeyboardReplyMarkup
) {

    message {
        if (message.text == "🔄 Сброс") {
            val chatId = message.chat.id
            waitingForPlate.remove(chatId)
            receiptStates.remove(chatId)
            bot.sendMessage(ChatId.fromId(chatId), "🔄 Состояние сброшено! Выберите действие:", replyMarkup = keyboardMain)
        }
    }
}
