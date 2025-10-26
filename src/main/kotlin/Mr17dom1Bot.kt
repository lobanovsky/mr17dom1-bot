import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.entities.BotCommand

fun main() {
    val token = System.getenv("TELEGRAM_BOT_TOKEN") ?: error("Переменная окружения TELEGRAM_BOT_TOKEN не задана!")
    val dbName = System.getenv("DB_NAME") ?: "./data/nations-bot.db"


    val subscribeCommandName = "subscribe"
    val unsubscribeCommandName = "unsubscribe"
    val statusCommandName = "status"
    val subscribesCommandName = "subs"
    val performancesCommandName = "perfs"

    val bot = bot {
        this.token = token
        logger().info("mr17dom1-bot is running...")

        dispatch {
//            subscriptionCommands(subscribeCommandName, unsubscribeCommandName, statusCommandName, subscribesCommandName)
//            perfCommands(performancesCommandName)
        }
    }

    // Устанавливаем список команд, чтобы они отображались в Telegram
    bot.setMyCommands(
        listOf(
            BotCommand(subscribeCommandName, "Подписаться на уведомления"),
            BotCommand(unsubscribeCommandName, "Отписаться от уведомлений"),
            BotCommand(statusCommandName, "Проверить статус подписки"),
            BotCommand(performancesCommandName, "Список спектаклей"),
            BotCommand(subscribesCommandName, "Получить список подписчиков"),
        )
    )

    bot.startPolling()
}