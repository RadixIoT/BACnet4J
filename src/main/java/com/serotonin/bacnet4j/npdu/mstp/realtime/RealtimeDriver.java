/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2025 Radix IoT LLC. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * When signing a commercial license with Radix IoT LLC,
 * the following extension to GPL is made. A special exception to the GPL is
 * included to allow you to distribute a combined work that includes BAcnet4J
 * without being obliged to provide the source code for any proprietary components.
 *
 * See www.radixiot.com for commercial license options.
 */

package com.serotonin.bacnet4j.npdu.mstp.realtime;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IOCTL JNA Wrapper To handle IOCTL calls to proprietary
 * Realtime MS/TP Driver
 *
 * @author Terry Packer
 */
public class RealtimeDriver {
    static final Logger LOG = LoggerFactory.getLogger(RealtimeDriver.class);

    private final File driver;
    private final File configProgram;

    public RealtimeDriver(File driver, File configProgram) {
        this.driver = driver;
        this.configProgram = configProgram;
    }

    /**
     * @param portId
     * @param baud
     * @param thisStation
     * @throws InterruptedException
     * @throws IOException
     */
    public void configure(String portId, int baud, byte thisStation, int maxMaster, int maxInfoFrames, int usageTimeout)
            throws InterruptedException, IOException {
        //TODO Redirect output to LOGs
        //TODO Create a setuid wrapper to call insmod to load the driver if not running as root

        //modprobe the driver
        ProcessBuilder pb = new ProcessBuilder("insmod",
                driver.getAbsolutePath());
        pb.redirectError(Redirect.INHERIT);
        pb.redirectOutput(Redirect.INHERIT);
        Process process = pb.start();
        process.waitFor();
        process.destroy();

        //Configure the driver for the port
        pb = new ProcessBuilder(configProgram.getAbsolutePath(),
                "-d" + portId,
                "-b" + baud,
                "-t" + thisStation,
                "-m" + maxMaster,
                "-f" + maxInfoFrames,
                "-u" + usageTimeout);
        pb.redirectError(Redirect.INHERIT);
        pb.redirectOutput(Redirect.INHERIT);
        process = pb.start();
        process.waitFor();
        process.destroy();
    }
}
