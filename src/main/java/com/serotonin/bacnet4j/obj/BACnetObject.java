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

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.mixin.CommandableMixin;
import com.serotonin.bacnet4j.obj.mixin.CovReportingMixin;
import com.serotonin.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.serotonin.bacnet4j.obj.mixin.ObjectIdAndNameMixin;
import com.serotonin.bacnet4j.obj.mixin.PropertyListMixin;
import com.serotonin.bacnet4j.obj.mixin.event.IntrinsicReportingMixin;
import com.serotonin.bacnet4j.service.acknowledgement.GetAlarmSummaryAck.AlarmSummary;
import com.serotonin.bacnet4j.service.acknowledgement.GetEnrollmentSummaryAck.EnrollmentSummary;
import com.serotonin.bacnet4j.service.acknowledgement.GetEventInformationAck.EventSummary;
import com.serotonin.bacnet4j.service.confirmed.GetEnrollmentSummaryRequest.AcknowledgmentFilter;
import com.serotonin.bacnet4j.service.confirmed.GetEnrollmentSummaryRequest.EventStateFilter;
import com.serotonin.bacnet4j.service.confirmed.GetEnrollmentSummaryRequest.PriorityFilter;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.RecipientProcess;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Matthew
 */
public class BACnetObject {
    static final Logger LOG = LoggerFactory.getLogger(BACnetObject.class);

    private final LocalDevice localDevice;
    private final ObjectType objectType;
    protected final Map<PropertyIdentifier, Encodable> properties = new ConcurrentHashMap<>();
    private final List<BACnetObjectListener> listeners = new CopyOnWriteArrayList<>();

    // Mixins
    private final List<AbstractMixin> mixins = new ArrayList<>();
    private CommandableMixin commandableMixin;
    private HasStatusFlagsMixin hasStatusFlagsMixin;
    private IntrinsicReportingMixin intrinsicReportingMixin;
    private CovReportingMixin changeOfValueMixin;

    // Configuration
    private boolean deletable;

    public BACnetObject(final LocalDevice localDevice, final ObjectType type, final int instanceNumber) {
        this(localDevice, type, instanceNumber, null);
    }

    public BACnetObject(final LocalDevice localDevice, final ObjectType type, final int instanceNumber,
            final String name) {
        this(localDevice, new ObjectIdentifier(type, instanceNumber), name);
    }

    public BACnetObject(final LocalDevice localDevice, final ObjectIdentifier id) {
        this(localDevice, id, null);
    }

    public BACnetObject(final LocalDevice localDevice, final ObjectIdentifier id, final String name) {
        Objects.requireNonNull(localDevice);
        Objects.requireNonNull(id);

        this.localDevice = localDevice;
        objectType = id.getObjectType();

        properties.put(PropertyIdentifier.objectIdentifier, id);
        properties.put(PropertyIdentifier.objectName, new CharacterString(name == null ? id.toString() : name));
        properties.put(PropertyIdentifier.objectType, objectType);

        // All objects have a property list.
        addMixin(new PropertyListMixin(this));
        addMixin(new ObjectIdAndNameMixin(this));
    }

    //
    //
    // Convenience methods
    //
    public ObjectIdentifier getId() {
        return get(PropertyIdentifier.objectIdentifier);
    }

    public int getInstanceId() {
        return getId().getInstanceNumber();
    }

    public String getObjectName() {
        final CharacterString name = get(PropertyIdentifier.objectName);
        if (name == null)
            return null;
        return name.getValue();
    }

    public LocalDevice getLocalDevice() {
        return localDevice;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(final boolean deletable) {
        this.deletable = deletable;
    }

    //
    //
    // Object notifications
    //
    final public void initialize() {
        // Notify the mixins
        for (final AbstractMixin mixin : mixins) {
            mixin.initialize();
        }
        initializeImpl();
    }

    protected void initializeImpl() {
        // no op, override as required
    }

    /**
     * Called when the object is removed from the device.
     */
    final public void terminate() {
        // Notify the mixins
        for (final AbstractMixin mixin : mixins) {
            mixin.terminate();
        }
        terminateImpl();
    }

    protected void terminateImpl() {
        // no op, override as required
    }

    //
    //
    // Listeners
    //
    public void addListener(final BACnetObjectListener l) {
        listeners.add(l);
    }

    public void removeListener(final BACnetObjectListener l) {
        listeners.remove(l);
    }

    //
    //
    // Mixins
    //

    /**
     * Add Mixin to known location in the list of mixins, usually first position=0
     * @param position
     * @param mixin
     */
    protected void addMixin(final int position, final AbstractMixin mixin) {
        mixins.add(position, mixin);

        if (mixin instanceof HasStatusFlagsMixin)
            hasStatusFlagsMixin = (HasStatusFlagsMixin) mixin;
        else if (mixin instanceof CommandableMixin)
            commandableMixin = (CommandableMixin) mixin;
        else if (mixin instanceof IntrinsicReportingMixin)
            intrinsicReportingMixin = (IntrinsicReportingMixin) mixin;
        else if (mixin instanceof CovReportingMixin)
            changeOfValueMixin = (CovReportingMixin) mixin;
    }

    /**
     * Add mixin to the end of the mixin list
     * @param mixin
     */
    protected final void addMixin(final AbstractMixin mixin) {
        addMixin(mixins.size(), mixin);
    }

    public void setOverridden(final boolean b) {
        if (hasStatusFlagsMixin != null)
            hasStatusFlagsMixin.setOverridden(b);
        if (commandableMixin != null)
            commandableMixin.setOverridden(b);
    }

    public boolean isOverridden() {
        if (hasStatusFlagsMixin != null)
            return hasStatusFlagsMixin.isOverridden();
        if (commandableMixin != null)
            return commandableMixin.isOverridden();
        return false;
    }

    //
    // Commandable
    protected void _supportCommandable(final Encodable relinquishDefault) {
        if (commandableMixin != null)
            commandableMixin.supportCommandable(relinquishDefault);
    }

    public boolean supportsCommandable() {
        if (commandableMixin != null)
            return commandableMixin.supportsCommandable();
        return false;
    }

    protected void _supportValueSource() {
        if (commandableMixin != null)
            commandableMixin.supportValueSource();
    }

    public boolean supportsValueSource() {
        if (commandableMixin != null)
            return commandableMixin.supportsValueSource();
        return false;
    }

    protected void _supportWritable() {
        if (commandableMixin != null)
            commandableMixin.supportWritable();
    }

    public boolean supportsWritable() {
        if (commandableMixin != null)
            return commandableMixin.supportsWritable();
        return false;
    }

    //
    // Intrinsic reporting
    public void acknowledgeAlarm(final UnsignedInteger acknowledgingProcessIdentifier,
            final EventState eventStateAcknowledged, final TimeStamp timeStamp,
            final CharacterString acknowledgmentSource, final TimeStamp timeOfAcknowledgment)
            throws BACnetServiceException {
        if (intrinsicReportingMixin == null)
            throw new BACnetServiceException(ErrorClass.object, ErrorCode.noAlarmConfigured);
        intrinsicReportingMixin.acknowledgeAlarm(acknowledgingProcessIdentifier, eventStateAcknowledged, timeStamp,
                acknowledgmentSource, timeOfAcknowledgment);
    }

    //
    // COVs
    protected void _supportCovReporting(final Real covIncrement, final UnsignedInteger covPeriod) {
        addMixin(new CovReportingMixin(this, covIncrement, covPeriod));
    }

    public AlarmSummary getAlarmSummary() {
        if (intrinsicReportingMixin != null)
            return intrinsicReportingMixin.getAlarmSummary();
        return null;
    }

    public EventSummary getEventSummary() {
        if (intrinsicReportingMixin != null)
            return intrinsicReportingMixin.getEventSummary();
        return null;
    }

    public EnrollmentSummary getEnrollmentSummary(final AcknowledgmentFilter acknowledgmentFilter,
            final RecipientProcess enrollmentFilter, final EventStateFilter eventStateFilter,
            final EventType eventTypeFilter, final PriorityFilter priorityFilter,
            final UnsignedInteger notificationClassFilter) {
        if (intrinsicReportingMixin != null)
            return intrinsicReportingMixin.getEnrollmentSummary(acknowledgmentFilter, enrollmentFilter,
                    eventStateFilter, eventTypeFilter, priorityFilter, notificationClassFilter);
        return null;
    }

    //
    // COV
    public void addCovSubscription(final Address from, final UnsignedInteger subscriberProcessIdentifier,
            final Boolean issueConfirmedNotifications, final UnsignedInteger lifetime,
            final PropertyReference monitoredPropertyIdentifier, final Real covIncrement)
            throws BACnetServiceException {
        if (changeOfValueMixin == null)
            throw new BACnetServiceException(ErrorClass.object, ErrorCode.optionalFunctionalityNotSupported);
        changeOfValueMixin.addCovSubscription(from, subscriberProcessIdentifier, issueConfirmedNotifications, lifetime,
                monitoredPropertyIdentifier, covIncrement);
    }

    public void removeCovSubscription(final Address from, final UnsignedInteger subscriberProcessIdentifier,
            final PropertyReference monitoredPropertyIdentifier) {
        if (changeOfValueMixin != null)
            changeOfValueMixin.removeCovSubscription(from, subscriberProcessIdentifier, monitoredPropertyIdentifier);
    }

    //
    // Persistence
    public String getPersistenceKey(final PropertyIdentifier pid) {
        return objectType.toString() + "." + getInstanceId() + "." + pid;
    }

    //
    //
    // Get property
    //
    /**
     * This method should only be used internally. Services should use the getProperty method. This method circumvents
     * all mixins and retrieves the property directly from the internal map.
     */
    @SuppressWarnings("unchecked")
    public <T extends Encodable> T get(final PropertyIdentifier pid) {
        return (T) properties.get(pid);
    }

    /**
     * Reads the given property. All mixins and the object are notified with beforeReadProperty prior to getting the
     * value from the internal map.
     *
     * @param pid
     * @return
     * @throws BACnetServiceException
     *             if the object objected to the read
     */
    @SuppressWarnings("unchecked")
    public final <T extends Encodable> T readProperty(final PropertyIdentifier pid) throws BACnetServiceException {
        // Give the mixins notice that the property is being read.
        for (final AbstractMixin mixin : mixins)
            mixin.beforeReadProperty(pid);
        beforeReadProperty(pid);

        return (T) get(pid);
    }

    /**
     * Reads the given property. All mixins and the object are notified with beforeReadProperty prior to getting the
     * value from the internal map. Will not return null.
     *
     * @param pid
     * @return
     * @throws BACnetServiceException
     *             if the object objected to the read, or if the property was not found (was null).
     */
    public final Encodable readPropertyRequired(final PropertyIdentifier pid) throws BACnetServiceException {
        final Encodable p = readProperty(pid);
        if (p == null)
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.unknownProperty);
        return p;
    }

    /**
     * Reads the given one-based index of the array for the given pid. All mixins and the object are notified with
     * beforeReadProperty prior to getting the value from the internal map.
     *
     * @param pid
     * @param propertyArrayIndex
     * @return
     * @throws BACnetServiceException
     *             if the object objected to the read
     */
    public final Encodable readProperty(final PropertyIdentifier pid, final UnsignedInteger propertyArrayIndex)
            throws BACnetServiceException {
        final Encodable result = readProperty(pid);
        if (propertyArrayIndex == null)
            return result;

        if (!(result instanceof BACnetArray<?>))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.propertyIsNotAnArray);

        final SequenceOf<?> array = (SequenceOf<?>) result;
        final int index = propertyArrayIndex.intValue();
        if (index == 0)
            return new UnsignedInteger(array.getCount());

        if (index > array.size())
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidArrayIndex);

        return array.getBase1(index);
    }

    /**
     * Reads the given one-based index of the array for the given pid. All mixins and the object are notified with
     * beforeReadProperty prior to getting the value from the internal map. Will not return null.
     *
     * @param pid
     * @param propertyArrayIndex
     * @return
     * @throws BACnetServiceException
     *             if the object objected to the read, or if the property was not found (was null).
     */
    public final Encodable readPropertyRequired(final PropertyIdentifier pid, final UnsignedInteger propertyArrayIndex)
            throws BACnetServiceException {
        final Encodable p = readProperty(pid, propertyArrayIndex);
        if (p == null)
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.unknownProperty);
        return p;
    }

    //
    //
    // Set property
    //
    /**
     * Write a property with no notifications. Circumvents the object and all mixins for validations, handling, and
     * post-write notifications.
     *
     * @param pid
     * @param value
     */
    protected void set(final PropertyIdentifier pid, final Encodable value) {
        properties.put(pid, value);
    }

    /**
     * Convenience method for writing a property with full validation, handling, and post-write notifications.
     */
    public BACnetObject writeProperty(final ValueSource valueSource, final PropertyIdentifier pid,
            final Encodable value) throws BACnetServiceException {
        return writeProperty(valueSource, new PropertyValue(pid, value));
    }

    /**
     * Convenience method for writing a property with full validation, handling, and post-write notifications.
     */
    public BACnetObject writeProperty(final ValueSource valueSource, final PropertyIdentifier pid, final int indexBase1,
            final Encodable value) throws BACnetServiceException {
        return writeProperty(valueSource, new PropertyValue(pid, new UnsignedInteger(indexBase1), value, null));
    }

    /**
     * Entry point for writing a property via services. Provides validation, write handling, and post-write
     * notifications via the object itself and mixins.
     *
     * @param value
     * @return
     * @throws BACnetServiceException
     */
    @SuppressWarnings("unchecked")
    public BACnetObject writeProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        final PropertyIdentifier pid = value.getPropertyIdentifier();
        final UnsignedInteger pin = value.getPropertyArrayIndex();
        Encodable valueToWrite = value.getValue();

        if (PropertyIdentifier.objectType.equals(pid))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
        if (PropertyIdentifier.priorityArray.equals(pid))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);

        // Validation - run through the mixins
        boolean handled = false;
        for (final AbstractMixin mixin : mixins) {
            handled = mixin.validateProperty(valueSource, value);
            if (handled)
                break;
        }
        if (!handled)
            handled = validateProperty(valueSource, value);

        if (!handled) {
            // Validate the value to be written. First get the definition for the property.
            final ObjectPropertyTypeDefinition def = ObjectProperties.getObjectPropertyTypeDefinition(objectType, pid);

            if (pin == null) {
                // Not writing to an array index.
                if (def == null) {
                    if (pid.intValue() < 512) {
                        // There should be a definition for any property with an id in the ASHRAE range.
                        throw new BACnetServiceException(ErrorClass.property, ErrorCode.unknownProperty);
                    }
                    if (value.getValue().getClass() == SequenceOf.class) {
                        // If the value to write is a collection, then disallow the write because we can't tell if it
                        // is supposed to be a list or an array.
                        throw new BACnetServiceException(ErrorClass.property, ErrorCode.datatypeNotSupported);
                    }
                } else {
                    final PropertyTypeDefinition pdef = def.getPropertyTypeDefinition();

                    // Validate against the property definition.
                    if (pdef.isArray()) {
                        // The property to write is an array, so the given value needs to be a sequence.
                        if (!(valueToWrite instanceof SequenceOf))
                            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);

                        final SequenceOf<Encodable> seq = (SequenceOf<Encodable>) valueToWrite;

                        if (pdef.getArrayLength() > 0) {
                            // And the length needs to match the definition if not n
                            if (seq.getCount() != pdef.getArrayLength())
                                throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
                        }

                        // And the types of the elements need to match the definition.
                        for (final Encodable e : seq) {
                            if (!pdef.getClazz().isAssignableFrom(e.getClass()))
                                throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
                        }

                        // Turn the value into an array.
                        valueToWrite = new BACnetArray<>(seq.getValues());
                    } else if (pdef.isList()) {
                        // The property to write is a list, so the given value needs to be a sequence.
                        if (!(valueToWrite instanceof SequenceOf))
                            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);

                        final SequenceOf<Encodable> seq = (SequenceOf<Encodable>) valueToWrite;

                        // And the types of the elements need to match the definition.
                        for (final Encodable e : seq) {
                            if (!pdef.getClazz().isAssignableFrom(e.getClass()))
                                throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
                        }
                    } else {
                        // The property to write is a scalar. Validate the type.
                        if (!pdef.getClazz().isAssignableFrom(value.getValue().getClass()))
                            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
                    }
                }
            } else {
                // Writing to an array index.
                final Encodable prop = properties.get(pid);
                if (prop == null) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.unknownProperty);
                }
                if (!(prop instanceof BACnetArray)) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.propertyIsNotAnArray);
                }

                final BACnetArray<?> arr = (BACnetArray<?>) prop;
                if (def == null) {
                    // No property definition available, but we can check that the data type to write matches that
                    // of any existing elements.
                    if (arr.getCount() > 0) {
                        if (arr.getBase1(1).getClass() != value.getValue().getClass()) {
                            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
                        }
                    }
                } else {
                    if (!def.getPropertyTypeDefinition().getClazz().isAssignableFrom(value.getValue().getClass()))
                        throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
                }

                // Index check.
                if (pin.intValue() < 1 || pin.intValue() > arr.getCount()) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidArrayIndex);
                }
            }
        }

        // Writing
        handled = false;
        for (final AbstractMixin mixin : mixins) {
            handled = mixin.writeProperty(valueSource, value);
            if (handled)
                break;
        }
        if (!handled) {
            // Set the property
            if (pin == null) {
                // Set the value of a property
                writePropertyInternal(pid, valueToWrite);
            } else {
                // Set the value in an array.
                final BACnetArray<Encodable> arr = (BACnetArray<Encodable>) properties.get(pid);
                arr.setBase1(pin.intValue(), valueToWrite);
                writePropertyInternal(pid, arr);
            }
        }

        return this;
    }

    /**
     * Entry point for changing a property circumventing object/mixin validation and write handling. Used primarily for
     * object configuration and property writes from mixins themselves, but can also be used by client code to set
     * object properties. Calls mixin "after write" methods and fires COV subscriptions.
     *
     * @param pid
     * @param value
     * @return
     */
    public BACnetObject writePropertyInternal(final PropertyIdentifier pid, final Encodable value) {
        final Encodable oldValue = properties.get(pid);
        set(pid, value);

        // After writing.
        for (final AbstractMixin mixin : mixins)
            mixin.afterWriteProperty(pid, oldValue, value);
        afterWriteProperty(pid, oldValue, value);

        if (!Objects.equals(value, oldValue)) {
            // Notify listeners
            for (final BACnetObjectListener l : listeners)
                l.propertyChange(pid, oldValue, value);
        }

        return this;
    }

    /**
     * Allows the object itself to validate the property before being written.
     *
     * @param valueSource
     * @param value
     * @return true if no more validation should occur, including the generic data type validation.
     * @throws BACnetServiceException
     *             to abort the write.
     */
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        return false;
    }

    /**
     * Allows notification to the object itself of a property write, in the same manner as it works for mixins.
     *
     * @param pid
     * @param oldValue
     * @param newValue
     */
    protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        // no op
    }

    /**
     * Allows notification to the object itself before a property read.
     *
     * @param pid
     * @throws BACnetServiceException
     */
    protected void beforeReadProperty(final PropertyIdentifier pid) throws BACnetServiceException {
        // no op
    }

    //
    //
    // Other
    //

    @Override
    public int hashCode() {
        final ObjectIdentifier id = getId();
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (id == null ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        final ObjectIdentifier id = getId();
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final BACnetObject other = (BACnetObject) obj;
        if (id == null) {
            if (other.getId() != null)
                return false;
        } else if (!id.equals(other.getId()))
            return false;
        return true;
    }
}
