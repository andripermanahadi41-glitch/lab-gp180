package com.example.model

enum class ConnectionProtocol(val label: String, val badgeText: String) {
    NONE("Belum Terhubung", "DISCONNECTED"),
    BLUETOOTH_LE("Bluetooth Nirkabel (BLE)", "BLE WIRELESS"),
    USB_MIDI("USB MIDI Berwayar", "USB WIRED"),
    SIMULATION("Simulasi Hardware GP-180", "VIRTUAL RIG")
}

enum class MidiDirection {
    TX, RX
}

data class MidiLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String,
    val direction: MidiDirection,
    val hexBytes: String,
    val description: String
)

data class FxParam(
    val key: String,
    val value: Int,
    val min: Int = 0,
    val max: Int = 100,
    val unit: String = ""
)

data class FxModule(
    val id: String,
    val name: String,
    val category: String,
    val isEnabled: Boolean,
    val cc: Int,
    val params: Map<String, Int>
)

data class PresetRig(
    val id: String,
    val name: String,
    val songReference: String = "",
    val soundPrompt: String = "",
    val patchNumber: Int = 0,
    val modules: List<FxModule>,
    val fs1Title: String = "Toggle BST",
    val fs1Cc: Int = 81,
    val fs2Title: String = "Toggle DLY",
    val fs2Cc: Int = 82,
    val fs3Title: String = "Solo Boost (+3dB)",
    val fs3Cc: Int = 83
) {
    val patchDisplay: String
        get() {
            val bank = (patchNumber / 3) + 1
            val letter = when (patchNumber % 3) {
                0 -> "A"
                1 -> "B"
                else -> "C"
            }
            return "%02d-%s".format(bank, letter)
        }
}
