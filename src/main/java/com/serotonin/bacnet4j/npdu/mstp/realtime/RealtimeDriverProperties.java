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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * TODO Describe what properties are required in the config file
 *
 * Class to hold the IOCTL Constants for the Realtime Driver
 *
 * @author Terry Packer
 */
public class RealtimeDriverProperties {

    private final Map<String, Integer> constants;

    public RealtimeDriverProperties(InputStream propertiesStream) throws IOException {
        Properties properties = new Properties();
        properties.load(propertiesStream);
        constants = new HashMap<>();
        properties.forEach((key, value) -> {
            constants.put((String) key, Integer.decode((String) value));
        });


    }

    public int getValue(String key) throws NumberFormatException {
        return constants.get(key);
    }

}
