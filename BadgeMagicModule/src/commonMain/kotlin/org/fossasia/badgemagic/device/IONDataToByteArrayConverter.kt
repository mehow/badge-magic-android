package org.fossasia.badgemagic.device

import org.fossasia.badgemagic.data.DataToSend
import org.fossasia.badgemagic.data.Message
import org.fossasia.badgemagic.data.Mode
import org.fossasia.badgemagic.data.Speed
import org.fossasia.badgemagic.utils.ByteArrayUtils
import kotlin.experimental.or

object IONDataToByteArrayConverter {
    private const val PAGE_BYTE_SIZE = 66
    private const val PACKET_BYTE_SIZE = 14

    fun convert(data: DataToSend): List<ByteArray> {
        val hexStrings = ArrayList<String>()
        hexStrings.add("8E44FF") // clear existing messages
        hexStrings.addAll(
                data.messages.mapIndexed { messageIdx, message ->
                    val pages = message.hexStrings.joinToString("")
                            .chunked(IONDataToByteArrayConverter.PAGE_BYTE_SIZE * 2)
                            .map {
                                StringBuilder()
                                        .append(it)
                                        .append("0".repeat(2 * PAGE_BYTE_SIZE - it.length))
                                        .toString()
                            }
                            .map { transpose(it) }
                    ArrayList<String>().apply {
                        add(StringBuilder()
                                .append("8A")
                                .append("0").append(messageIdx + 1) // messageIndex < 8
                                .append(getMoveMode(message))
                                .append(getScrollSpeed(message))
                                .append("0").append(pages.size.toString(16))
                                .append("31") // brightness
                                .append("00") // font type
                                .append("FF")
                                .toString())
                        addAll(
                                pages.flatMap {
                                    it.chunked(PACKET_BYTE_SIZE * 2)
                                            .mapIndexed { packetIdx, packet ->
                                                StringBuilder()
                                                        .append("8B")
                                                        .append("0").append(messageIdx + 1) // messageIdx < 8
                                                        .append("05") // 5 packets
                                                        .append("0").append(packetIdx) // idx < 5
                                                        .append("0").append((packet.length / 2).toString(16)) // len <= 14 bytes
                                                        .append(packet)
                                                        .append("0".repeat(2 * PACKET_BYTE_SIZE - packet.length))
                                                        .append("FF")
                                                        .toString()
                                            }
                                })
                        add(StringBuilder().append("820").append(messageIdx + 1).append("01FF").toString())
                        add(StringBuilder().append("810").append(messageIdx + 1).append("0100012100000000000000000000000000FF").toString())
                    }
                }.flatten())
        hexStrings.add("8505FF")
        return hexStrings.map { msg -> ByteArrayUtils.hexStringToByteArray(msg) }
    }

    private fun getMoveMode(message: Message): String {
        return when {
            message.flash -> "15"
            else -> when (message.mode) {
                Mode.FIXED -> "10"
                Mode.RIGHT -> "11"
                Mode.LEFT -> "12"
                else -> "10"
            }
        }
    }

    private fun getScrollSpeed(message: Message): String {
        return when (message.speed) {
            Speed.THREE -> "21"
            Speed.TWO -> "22"
            Speed.ONE -> "23"
            else -> "23"
        }
    }

    private fun transpose(page: String): String {
        val b = StringBuilder()
        for (i in 0 until 11) {
            for (j in 0 until 6) {
                b.append(page[22 * j + 2 * i]).append(page[22 * j + 2 * i + 1])
            }
        }
        return b.toString()
    }
}