/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * When signing a commercial license with Infinite Automation Software,
 * the following extension to GPL is made. A special exception to the GPL is
 * included to allow you to distribute a combined work that includes BAcnet4J
 * without being obliged to provide the source code for any proprietary components.
 *
 * See www.infiniteautomation.com for commercial license options.
 *
 * @author Matthew Lohbihler
 */
package com.serotonin.bacnet4j.obj;

import java.util.Map;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

/**
 * Mixins allow different objects to share functionality that is otherwise common between them. Functionality that is
 * specific to a given object type should still be coded into the class for the type itself.
 *
 * @author Matthew
 */
public class AbstractMixin {
    private final BACnetObject bo;

    public AbstractMixin(final BACnetObject bo) {
        this.bo = bo;
    }

    //
    //
    // Utility methods for subclasses to be able to access object properties.
    //
    protected final Map<PropertyIdentifier, Encodable> properties() {
        return bo.properties;
    }

    // Adds the property to the properties map, and updates the property list as required.
    protected final void addProperty(final PropertyIdentifier pid, final Encodable value, final boolean overwrite) {
        if (overwrite || !properties().containsKey(pid)) {
            properties().put(pid, value);
        }
    }

    protected final void writePropertyInternal(final PropertyIdentifier pid, final Encodable value) {
        bo.writePropertyInternal(pid, value);
    }

    protected final void set(final PropertyIdentifier pid, final Encodable value) {
        bo.set(pid, value);
    }

    protected final <T extends Encodable> T get(final PropertyIdentifier pid) {
        return bo.get(pid);
    }

    protected final <T extends Encodable> T get(final PropertyIdentifier pid, final T defaultValue) {
        final T value = get(pid);
        return value == null ? defaultValue : value;
    }

    protected final LocalDevice getLocalDevice() {
        return bo.getLocalDevice();
    }

    protected final ObjectIdentifier getId() {
        return bo.getId();
    }

    //
    //
    // Methods for subclasses to override as needed.
    //
    /**
     * Allow the mixin a chance to perform actions before the property is read.
     *
     * @param pid
     * @throws BACnetServiceException
     *             if for some reason the read should be prevented.
     */
    protected void beforeReadProperty(final PropertyIdentifier pid) throws BACnetServiceException {
        // no op
    }

    /**
     * Allow the mixin to override the property validation.
     *
     * @param valueSource
     * @param value
     * @return true if the validation was handled, false otherwise.
     * @throws BACnetServiceException
     *             if the property was so bad that an exception had to be thrown
     */
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        return false;
    }

    /**
     * Allow the mixin to override the property write.
     *
     * @param valueSource
     * @param value
     * @return true if the write was handled, false otherwise.
     * @throws BACnetServiceException
     */
    protected boolean writeProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        return false;
    }

    /**
     * @param pid
     * @param oldValue
     * @param newValue
     */
    protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        // no op
    }

    /**
     * Provides notice to the mixin that the object should initialize itself.
     */
    protected void initialize() {
        // no op
    }

    /**
     * Provides notice to the mixin that the object was removed from the device.
     */
    protected void terminate() {
        // no op
    }
}
