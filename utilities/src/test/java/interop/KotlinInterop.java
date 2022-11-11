package interop;

import com.diozero.api.GpioPullUpDown;
import com.diozero.sbc.DeviceFactoryHelper;
import crackers.kobots.utilities.DebouncedButton;

import java.time.Duration;

/**
 * Kotlin interop by compilation.
 */
public class KotlinInterop {
    void button() {
        DebouncedButton b1 = new DebouncedButton(18, Duration.ofMillis(50), GpioPullUpDown.PULL_UP);
        DebouncedButton b2 = new DebouncedButton(18, Duration.ofMillis(50), GpioPullUpDown.PULL_UP, true);
        DebouncedButton b3 = new DebouncedButton(18, Duration.ofMillis(50), GpioPullUpDown.PULL_UP, false, DeviceFactoryHelper.getNativeDeviceFactory());
    }
}
