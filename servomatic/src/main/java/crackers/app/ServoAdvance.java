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

package crackers.app;

import com.diozero.api.ServoDevice;
import com.diozero.api.ServoTrim;
import com.diozero.devices.PCA9685;

/**
 * This is weird...
 */
public class ServoAdvance {
    public static void main(String[] args) throws Exception {
        try (PCA9685 servoController = new PCA9685()) {
            ServoDevice servo = new ServoDevice.Builder(0)
                    // does not work
                    //  .setTrim(ServoTrim.TOWERPRO_SG90)
                    //  .setTrim(newServoTrim(1500,901))
                    //  .setTrim(newServoTrim(1450,901))

                    // works
//                    .setTrim(ServoTrim.DEFAULT)
                    .setTrim(new ServoTrim(1450, 900))
                    .setFrequency(servoController.getBoardPwmFrequency())
                    .setDeviceFactory(servoController)
                    .build();

            servo.setAngle(0f);
            Thread.sleep(1000);
            System.out.println("Starting sweep");
            for (int i = 0; i <= 180; i++) {
                boolean done = false;
                float current = 0f;
                for (int j = 0; j < 5; j++) {
                    servo.setAngle(i);
                    Thread.sleep(5);
                    current = servo.getAngle();
                    done = servo.getAngle() == i;
                    if (done) break;
                }
                if (!done) {
                    System.out.printf("Failed to set angle %d - current is %.02f%n", i, current);
                }
            }
            System.out.println("Sweep complete - rolling back");
            for (int i = 180; i >= 0; i--) {
                servo.setAngle(i);
                Thread.sleep(5);
            }
            System.out.println("Rollback complete");
        }
    }
}
