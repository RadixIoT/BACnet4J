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

package com.serotonin.bacnet4j.obj;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.mixin.CommandableMixin;
import com.serotonin.bacnet4j.obj.mixin.CovReportingMixin;
import com.serotonin.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.serotonin.bacnet4j.obj.mixin.ObjectIdAndNameMixin;
import com.serotonin.bacnet4j.obj.mixin.PropertyListMixin;
import com.serotonin.bacnet4j.obj.mixin.event.EventReportingMixin;
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
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

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
    private EventReportingMixin eventReportingMixin;
    private CovReportingMixin changeOfValueMixin;

    // Configuration
    private boolean deletable;

    public BACnetObject(LocalDevice localDevice, ObjectType type, int instanceNumber) {
        this(localDevice, type, instanceNumber, null);
    }

    public BACnetObject(LocalDevice localDevice, ObjectType type, int instanceNumber, String name) {
        this(localDevice, new ObjectIdentifier(type, instanceNumber), name);
    }

    public BACnetObject(LocalDevice localDevice, ObjectIdentifier id) {
        this(localDevice, id, null);
    }

    public BACnetObject(LocalDevice localDevice, ObjectIdentifier id, String name) {
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
        CharacterString name = get(PropertyIdentifier.objectName);
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

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    //
    //
    // Object notifications
    //
    public final void initialize() {
        // Notify the mixins
        for (AbstractMixin mixin : mixins) {
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
    public final void terminate() {
        // Notify the mixins
        for (AbstractMixin mixin : mixins) {
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
    public void addListener(BACnetObjectListener l) {
        listeners.add(l);
    }

    public void removeListener(BACnetObjectListener l) {
        listeners.remove(l);
    }

    //
    //
    // Mixins
    //
    protected final void addMixin(AbstractMixin mixin) {
        addMixin(mixins.size(), mixin);
    }

    protected final void addMixin(int index, AbstractMixin mixin) {
        mixins.add(index, mixin);

        if (mixin instanceof HasStatusFlagsMixin m)
            hasStatusFlagsMixin = m;
        else if (mixin instanceof CommandableMixin m)
            commandableMixin = m;
        else if (mixin instanceof EventReportingMixin m)
            eventReportingMixin = m;
        else if (mixin instanceof CovReportingMixin m)
            changeOfValueMixin = m;
    }

    public void setOverridden(boolean b) {
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
    protected void _supportCommandable(Encodable relinquishDefault) {
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
    public void acknowledgeAlarm(UnsignedInteger acknowledgingProcessIdentifier, EventState eventStateAcknowledged,
            TimeStamp timeStamp, CharacterString acknowledgmentSource, TimeStamp timeOfAcknowledgment)
            throws BACnetServiceException {
        if (eventReportingMixin == null)
            throw new BACnetServiceException(ErrorClass.object, ErrorCode.noAlarmConfigured);
        eventReportingMixin.acknowledgeAlarm(acknowledgingProcessIdentifier, eventStateAcknowledged, timeStamp,
                acknowledgmentSource, timeOfAcknowledgment);
    }

    //
    // COVs
    protected void _supportCovReporting(Real covIncrement, UnsignedInteger covPeriod) {
        addMixin(new CovReportingMixin(this, covIncrement, covPeriod));
    }

    public AlarmSummary getAlarmSummary() {
        if (eventReportingMixin != null)
            return eventReportingMixin.getAlarmSummary();
        return null;
    }

    public EventSummary getEventSummary() {
        if (eventReportingMixin != null)
            return eventReportingMixin.getEventSummary();
        return null;
    }

    public EnrollmentSummary getEnrollmentSummary(AcknowledgmentFilter acknowledgmentFilter,
            RecipientProcess enrollmentFilter, EventStateFilter eventStateFilter, EventType eventTypeFilter,
            PriorityFilter priorityFilter, UnsignedInteger notificationClassFilter) {
        if (eventReportingMixin != null)
            return eventReportingMixin.getEnrollmentSummary(acknowledgmentFilter, enrollmentFilter,
                    eventStateFilter, eventTypeFilter, priorityFilter, notificationClassFilter);
        return null;
    }

    //
    // COV
    public void addCovSubscription(Address from, UnsignedInteger subscriberProcessIdentifier,
            Boolean issueConfirmedNotifications, UnsignedInteger lifetime,
            PropertyReference monitoredPropertyIdentifier, Real covIncrement) throws BACnetServiceException {
        if (changeOfValueMixin == null)
            throw new BACnetServiceException(ErrorClass.object, ErrorCode.optionalFunctionalityNotSupported);
        changeOfValueMixin.addCovSubscription(from, subscriberProcessIdentifier, issueConfirmedNotifications, lifetime,
                monitoredPropertyIdentifier, covIncrement);
    }

    public void removeCovSubscription(Address from, UnsignedInteger subscriberProcessIdentifier,
            PropertyReference monitoredPropertyIdentifier) {
        if (changeOfValueMixin != null)
            changeOfValueMixin.removeCovSubscription(from, subscriberProcessIdentifier, monitoredPropertyIdentifier);
    }

    //
    // Persistence
    public String getPersistenceKey(PropertyIdentifier pid) {
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
    public <T extends Encodable> T get(PropertyIdentifier pid) {
        return (T) properties.get(pid);
    }

    /**
     * Reads the given property. All mixins and the object are notified with beforeReadProperty prior to getting the
     * value from the internal map.
     *
     * @throws BACnetServiceException if the object objected to the read
     */
    @SuppressWarnings("unchecked")
    public final <T extends Encodable> T readProperty(PropertyIdentifier pid) throws BACnetServiceException {
        // Give the mixins notice that the property is being read.
        for (AbstractMixin mixin : mixins)
            mixin.beforeReadProperty(pid);
        beforeReadProperty(pid);

        return (T) get(pid);
    }

    /**
     * Reads the given property. All mixins and the object are notified with beforeReadProperty prior to getting the
     * value from the internal map. Will not return null.
     *
     * @throws BACnetServiceException if the object objected to the read, or if the property was not found (was null).
     */
    public final Encodable readPropertyRequired(PropertyIdentifier pid) throws BACnetServiceException {
        Encodable p = readProperty(pid);
        if (p == null)
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.unknownProperty);
        return p;
    }

    /**
     * Reads the given one-based index of the array for the given pid. All mixins and the object are notified with
     * beforeReadProperty prior to getting the value from the internal map.
     *
     * @throws BACnetServiceException if the object objected to the read
     */
    public final Encodable readProperty(PropertyIdentifier pid, UnsignedInteger propertyArrayIndex)
            throws BACnetServiceException {
        Encodable result = readProperty(pid);
        if (propertyArrayIndex == null)
            return result;

        if (!(result instanceof BACnetArray<?> array))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.propertyIsNotAnArray);

        int index = propertyArrayIndex.intValue();
        if (index == 0)
            return new UnsignedInteger(array.getCount());

        if (index > array.size())
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidArrayIndex);

        return array.getBase1(index);
    }

    public Set<PropertyIdentifier> getPropertyIds() {
        return properties.keySet();
    }

    /**
     * Reads the given one-based index of the array for the given pid. All mixins and the object are notified with
     * beforeReadProperty prior to getting the value from the internal map. Will not return null.
     *
     * @throws BACnetServiceException if the object objected to the read, or if the property was not found (was null).
     */
    public final Encodable readPropertyRequired(PropertyIdentifier pid, UnsignedInteger propertyArrayIndex)
            throws BACnetServiceException {
        Encodable p = readProperty(pid, propertyArrayIndex);
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
     */
    protected void set(PropertyIdentifier pid, Encodable value) {
        properties.put(pid, value);
    }

    /**
     * Convenience method for writing a property with full validation, handling, and post-write notifications.
     */
    public BACnetObject writeProperty(ValueSource valueSource, PropertyIdentifier pid, Encodable value)
            throws BACnetServiceException {
        return writeProperty(valueSource, new PropertyValue(pid, value));
    }

    /**
     * Convenience method for writing a property with full validation, handling, and post-write notifications.
     */
    public BACnetObject writeProperty(ValueSource valueSource, PropertyIdentifier pid, int indexBase1, Encodable value)
            throws BACnetServiceException {
        return writeProperty(valueSource, new PropertyValue(pid, new UnsignedInteger(indexBase1), value, null));
    }

    /**
     * Entry point for writing a property via services. Provides validation, write handling, and post-write
     * notifications via the object itself and mixins.
     */
    @SuppressWarnings("unchecked")
    public BACnetObject writeProperty(ValueSource valueSource, PropertyValue value) throws BACnetServiceException {
        PropertyIdentifier pid = value.getPropertyIdentifier();
        UnsignedInteger pin = value.getPropertyArrayIndex();
        Encodable valueToWrite = value.getValue();

        if (PropertyIdentifier.objectType.equals(pid))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
        if (PropertyIdentifier.priorityArray.equals(pid))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);

        ObjectPropertyTypeDefinition def = ObjectProperties.getObjectPropertyTypeDefinition(objectType, pid);

        // Per addendum 135-2016br-2: a relinquish (NULL value with a priority) targeting a non-commandable property
        // whose declared datatype does not include NULL shall not fail. The property is not changed and the write
        // is reported successful. Applied before any mixin validation because mixins routinely cast the incoming
        // value to the declared type without checking, which would otherwise throw before this rule can be
        // evaluated. Commandable properties are excluded — for those, NULL-with-priority is a real relinquish
        // handled by CommandableMixin.
        if (value.getValue() instanceof Null && value.getPriority() != null && def != null
                && !(ObjectProperties.isCommandable(objectType, pid) && supportsCommandable())
                && !def.getPropertyTypeDefinition().getClazz().isAssignableFrom(value.getValue().getClass())) {
            return this;
        }

        // Validation - run through the mixins
        boolean handled = false;
        for (AbstractMixin mixin : mixins) {
            handled = mixin.validateProperty(valueSource, value);
            if (handled)
                break;
        }
        if (!handled)
            handled = validateProperty(valueSource, value);

        if (!handled) {
            // Validate the value to be written.
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
                    PropertyTypeDefinition pdef = def.getPropertyTypeDefinition();

                    // Validate against the property definition.
                    if (pdef.isArray()) {
                        // The property to write is an array, so the given value needs to be a sequence.
                        if (!(valueToWrite instanceof SequenceOf))
                            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);

                        SequenceOf<Encodable> seq = (SequenceOf<Encodable>) valueToWrite;

                        if (pdef.getArrayLength() > 0) {
                            // And the length needs to match the definition if not n
                            if (seq.getCount() != pdef.getArrayLength())
                                throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
                        }

                        // And the types of the elements need to match the definition.
                        for (Encodable e : seq) {
                            if (!pdef.getClazz().isAssignableFrom(e.getClass()))
                                throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
                        }

                        // Turn the value into an array.
                        valueToWrite = new BACnetArray<>(seq.getValues());
                    } else if (pdef.isList()) {
                        // The property to write is a list, so the given value needs to be a sequence.
                        if (!(valueToWrite instanceof SequenceOf))
                            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);

                        SequenceOf<Encodable> seq = (SequenceOf<Encodable>) valueToWrite;

                        // And the types of the elements need to match the definition.
                        for (Encodable e : seq) {
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
                Encodable prop = properties.get(pid);
                if (prop == null) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.unknownProperty);
                }
                // Note that only arrays can be written by index. Lists need to be manipulated with the appropriate
                // service, e.g. AddListElement.
                if (!(prop instanceof BACnetArray<?> arr)) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.propertyIsNotAnArray);
                }

                if (pin.intValue() == 0) {
                    if (value.getValue() instanceof UnsignedInteger) {
                        // Resizing the array is not handled here because specific cases need to define what value to
                        // use as a default when an array is being expanded. Objects or mixins that support resizing
                        // handle the write before this. Per 15.9.1.3.1 (addendum 135-2020ci-6), a write that would
                        // resize the array to a size that is not supported returns INVALID_ARRAY_SIZE.
                        throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidArraySize);
                    }
                    // Can only write an unsigned integer to the zero-index.
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidArrayIndex);
                } else {
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
        }

        // Writing
        handled = false;
        for (AbstractMixin mixin : mixins) {
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
                BACnetArray<Encodable> arr = (BACnetArray<Encodable>) properties.get(pid);
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
     */
    public BACnetObject writePropertyInternal(PropertyIdentifier pid, Encodable value) {
        Encodable oldValue = properties.get(pid);
        set(pid, value);

        // After writing.
        for (AbstractMixin mixin : mixins)
            mixin.afterWriteProperty(pid, oldValue, value);
        afterWriteProperty(pid, oldValue, value);

        if (!Objects.equals(value, oldValue)) {
            // Notify listeners
            listeners.forEach(l -> l.propertyChange(pid, oldValue, value));
        }

        return this;
    }

    /**
     * Allows the object itself to validate the property before being written.
     *
     * @param valueSource the value source
     * @param value       the value to validate
     * @return true if no more validation should occur, including the generic data type validation.
     * @throws BACnetServiceException to abort the write.
     */
    protected boolean validateProperty(ValueSource valueSource, PropertyValue value) throws BACnetServiceException {
        return false;
    }

    /**
     * Allows notification to the object itself of a property write, in the same manner as it works for mixins.
     *
     * @param pid      the property identifier
     * @param oldValue the old value
     * @param newValue the new value
     */
    protected void afterWriteProperty(PropertyIdentifier pid, Encodable oldValue, Encodable newValue) {
        // no op
    }

    /**
     * Allows notification to the object itself before a property read.
     *
     * @param pid the property identifier
     */
    protected void beforeReadProperty(PropertyIdentifier pid) throws BACnetServiceException {
        // no op
    }

    //
    //
    // Other
    //

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        BACnetObject that = (BACnetObject) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
