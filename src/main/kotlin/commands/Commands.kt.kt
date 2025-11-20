package commands

import ReceiptState
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup

fun Dispatcher.commands(
    resetCommandName: String,
    waitingForPlate: MutableSet<Long>,
    receiptStates: MutableMap<Long, ReceiptState>,
    keyboard: KeyboardReplyMarkup
) {

    command(resetCommandName) {
        val chatId = message.chat.id

        waitingForPlate.remove(chatId)
        receiptStates.remove(chatId)

        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = "Состояние сброшено!\nВыберите действие:",
            replyMarkup = keyboard
        )
    }
}
