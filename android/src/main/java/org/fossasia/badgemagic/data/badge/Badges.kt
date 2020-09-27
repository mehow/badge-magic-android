package org.fossasia.badgemagic.data.badge

import java.util.UUID
import org.fossasia.badgemagic.device.DataToByteArrayConverter
import org.fossasia.badgemagic.device.IONDataToByteArrayConverter

enum class Badges(val device: DeviceID) {
    ION(DeviceID(
            UUID.fromString("e68adc60-70cc-42f2-9502-2185a12a675a"),
            UUID.fromString("e68adc61-70cc-42f2-9502-2185a12a675a"),
            IONDataToByteArrayConverter::convert
    )),
    LSLED(DeviceID(
        UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb"),
        UUID.fromString("0000fee1-0000-1000-8000-00805f9b34fb"),
        DataToByteArrayConverter::convert
    )),
    VBLAB(DeviceID(
        UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"),
        UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"),
        DataToByteArrayConverter::convert
    ));
}
