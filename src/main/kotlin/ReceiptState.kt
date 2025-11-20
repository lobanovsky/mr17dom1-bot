data class ReceiptState(
    var step: ReceiptStep = ReceiptStep.NONE,
    var month: String? = null,
    var type: String? = null,
    var number: Int? = null
)