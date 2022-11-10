package crackers.kobots.utilities

import com.diozero.api.PinInfo
import com.diozero.sbc.BoardPinInfo
import com.diozero.sbc.DeviceFactoryHelper

/**
 * Probably specific to the Raspbarry Pi.
 */
const val PI_HEADER = "J8"

fun BoardPinInfo.byName(name: String, header: String = PI_HEADER) =
    headers[header]?.values?.first {
        it.name == name
    } ?: throw NoSuchElementException("Header '$name' not found")

val defaultPins: BoardPinInfo by lazy { DeviceFactoryHelper.getNativeDeviceFactory().getBoardPinInfo() }

val piPins: Collection<PinInfo>? by lazy { defaultPins.headers[PI_HEADER]?.values }
