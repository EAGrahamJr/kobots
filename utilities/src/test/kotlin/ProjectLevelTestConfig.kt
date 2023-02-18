/*
 * Copyright 2022-2023 by E. A. Graham, Jr.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import com.diozero.sbc.DeviceFactoryHelper.DEVICE_FACTORY_PROP
import io.kotest.core.config.AbstractProjectConfig

/**
 * All tests will use this
 */
class ProjectLevelTestConfig : AbstractProjectConfig() {
    init {
        displayFullTestPath = true
        System.setProperty(DEVICE_FACTORY_PROP, "com.diozero.internal.provider.mock.MockDeviceFactory")
    }
}

fun List<Int>.bytes() = map { it.toByte() }
