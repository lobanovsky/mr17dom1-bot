import handlers.ReceiptStep

data class ReceiptState(
    var step: ReceiptStep = ReceiptStep.NONE,
    var month: String = "01",
    var roomType: RoomType = RoomType.FLAT,
    var number: Int = 1,
)

enum class RoomType(val description: String) {
    FLAT("🏡 Квартира"),
    PARKING_SPACE("🅿️ Машиноместо");


    //convert to object function
    companion object {
        fun fromDescription(text: String): RoomType? =
            entries.firstOrNull { it.description == text }

        fun textToType(text: String): RoomType {
            return when (text) {
                FLAT.description -> FLAT
                PARKING_SPACE.description -> PARKING_SPACE
                else -> throw IllegalArgumentException("Unknown room type: $text")
            }

        }
    }
}