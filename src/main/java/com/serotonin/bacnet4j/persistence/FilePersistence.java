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

package com.serotonin.bacnet4j.persistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class FilePersistence implements IPersistence {
    private final File file;
    private final Properties props;

    public FilePersistence(final File file) throws IOException {
        this.file = file;

        props = new Properties();
        try {
            try (FileReader in = new FileReader(file)) {
                props.load(in);
            }
        } catch (@SuppressWarnings("unused") final FileNotFoundException e) {
            // no op.
        }
    }

    @Override
    public void save(final String key, final String value) {
        props.setProperty(key, value);
        store();
    }

    @Override
    public String load(final String key) {
        return props.getProperty(key);
    }

    @Override
    public void remove(final String key) {
        props.remove(key);
        store();
    }

    private void store() {
        try {
            try (FileWriter out = new FileWriter(file)) {
                props.store(out, "");
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public File[] getFiles() {
        return new File[] {file};
    }
}
