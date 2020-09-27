package org.fossasia.badgemagic.data.badge

import java.util.UUID
import org.fossasia.badgemagic.data.DataToSend

data class DeviceID(val serviceID: UUID, val characteristicsID: UUID, val convert: (DataToSend) -> List<ByteArray>)
