BACnet4J
========

BACnet4J is a pure Java implementation of the BACnet specification. Originally developed for supervisory use, it now
includes support for many objects and so may be suitable for embedded use as well. Protocols supported include IPv4,
IPv6, and MS/TP. This library supports protocol Version 1 Revision 19.

A discussion forum for this package can be found at https://forum.mango-os.com/category/12/bacnet4j-general-discussion.

**Commercial licenses are available by contacting: sales@radixiot.com**

A public Maven Repository is now available with the latest builds add this to your pom.xml

```xml

<repositories>
    <repository>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
        <id>ias-snapshots</id>
        <name>RadixIoT Snapshot Repository</name>
        <url>https://maven.mangoautomation.net/repository/ias-snapshot/</url>
    </repository>
    <repository>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>ias-releases</id>
        <name>RadixIoT Release Repository</name>
        <url>https://maven.mangoautomation.net/repository/ias-release/</url>
    </repository>
</repositories>
```

The maven coordinates for BACnet4J 5.0+ are**

```xml

<dependency>
    <groupId>com.infiniteautomation</groupId>
    <artifactId>bacnet4j</artifactId>
    <version>x.x.x</version>
</dependency>
```

The dependency information is BACnet4J pre 5.0 is:

```xml

<dependency>
    <groupId>com.serotonin</groupId>
    <artifactId>bacnet4j</artifactId>
    <version>x.x.x</version>
</dependency>
```

Releases
========
*Version 7.0.0*

This version contains many breaking changes. Please review carefully.

- BACnet objects are no longer added to the local device in their constructors because of initialization problems.
  Instead, objects need to be explicitly added to the local device after instantiation. Many examples can be found in
  the unit tests, but e.g.:

```
// Old code
var av = new AnalogValueObject(localDevice, ...);
// New code
var av = localDevice.addObject(new AnalogValueObject(localDevice, ...));
```

- Introduction of NetworkPortObject and subclasses.
- Addition of many structures, enumeration, and properties defined in the 2024 specification
- Serialization bug fixes
- Removal of deprecated code
- Introduction of BACnet Secure Connection implementation as SC node connecting to Hub
- Fixes of various spec compliance issues, including data types and spec type implementations. The use of Unsigned32
  instead of UnsignedInteger. This will cause a break with existing client code that expects to be able to do the
  following:

```
UnsignedInteger ui = trendLog.get(PropertyIdentifier.recordCount); // Immediate ClassCastException
trendLogMult.writeProperty(PropertyIdentifier.recordCount, new UnsignedInteger(100)); // Possibly delayed ClassCastException
```

- The device communicate control option DISABLE has been deprecated as per 135-2016bi-2
- Default vendor renamed to Radix IoT LLC
- Protocol revision increased to 20
- Many renamings on multiple levels (class, method, field, enum, etc.) to make BACnet4J compliant with MS/TP
  nomenclature changes
- Names of some engineering units have been changed to align with the spec
- CharacterStringObject was renamed to CharacterStringValueObject
- LifeSafetyPoint and LifeSafetyZone now handle `trackingValue` and `reliability` properly according to out of service
  rules.
- Changes for compliance with addendum 135-2016bi-3 regarding extremely large logs
- Time form construction of DateTime choice has been deprecated. They should no longer be created by client code
- `LifeSafetyOperationRequest` returns error code specified in addendum bt-2
- The method `ExceptionListener.receivedThrowable` has been removed
- The method `DeviceEventListener.whoAmIReceived` has been added
- The method `DeviceEventListener.youAreReceived` has been added
- Code that allowed an unconfigured device to find an unused device id has been removed
- `LocalDevice.terminate()` now terminates the transport before shutting down the executor, so BACnet/SC connections
  close cleanly.
- The `StreamAccess` class has been made abstract, and two concrete subclasses created: `FileStreamAccess`, and
  `InMemeoryStreamAccess`. `FileStreamAccess` can be used as a drop in replacement for existing references to
  `StreamAccess`
- Many non-breaking changes that bring BACnet4J up to compliance with protocol revision 30
- The return value of `TagData.getTotalLength` has been changed from `int` to `long`. Fields that were previously public
  have been changed to private and getters created.
- Malformed confirmed requests are now answered with a Reject of `invalidDataEncoding` rather than an Error of
  `services / operationalProblem`, per 135-2024 18.9. Client code catching `ErrorAPDUException` for undecodable requests
  will need to catch `RejectAPDUException`.
- Decoding of primitive values is stricter. A tag whose declared length is outside the range its datatype can be encoded
  in, or which exceeds the data present, is rejected. A value encoded as the wrong datatype no longer decodes to a
  nonsense value, so a device that sends one will now fail the read, and in a ReadPropertyMultiple response a single
  such value fails the whole response.
- `intValue()` and `longValue()` on `UnsignedInteger`, `SignedInteger` and `Enumerated` now clamp to the range of the
  return type rather than truncating to its low order bits, so a value too large no longer arrives as a negative number.
  Use `bigIntegerValue()` where the exact value is required.
- `equals` and `hashCode` on `UnsignedInteger`, `SignedInteger` and `Enumerated` now compare by value rather than by
  internal representation, so `new UnsignedInteger(14)` equals `new UnsignedInteger(14L)`. The hash codes of these
  types, including `PropertyIdentifier` and `ObjectType`, have changed, which changes the iteration order of hash based
  collections keyed by them.
- Fixes: corrupt tag lengths no longer raise `ArithmeticException` or `NegativeArraySizeException` while decoding;
  `Enumerated` values that are a multiple of 2^32 no longer encode with an incorrect length; and a zero length
  `SignedInteger` no longer raises `NumberFormatException`.
- Segmentation handling has been rewritten to follow clause 5.4 as amended by addendum 135-2020ch-1. Sequence numbers
  are now the modulo 256 values the standard defines, so messages of more than 256 segments are sent and received
  correctly; previously such a transfer stalled at the wrap and timed out with no abort sent. Segments received out of
  order are now discarded and negatively acknowledged, as the standard requires, rather than being buffered and
  reordered. Negative segment acknowledgements are also now sent once too many duplicates arrive within a window, and
  are acted upon when received. After a segment timeout the segments of the current window are retransmitted rather than
  the first segment of the message.
- A segmented message whose proposed window size is outside the range 1 to 127 is now aborted with
  `windowSizeOutOfRange` instead of stalling, and `Transport.setSegWindow` clamps its argument to that range.
- Confirmed requests received at a broadcast or multicast address are now discarded without a response.
- Abort reasons have been corrected. A response that cannot be segmented because the client will not accept one is now
  `segmentationNotSupported` rather than `bufferOverflow`, and segment acknowledgements are now sent with
  data-expecting-reply false.
- `RemoteDevice.getMaxSegmentsAccepted()` has been added, and `Transport.send` has two new overloads that accept it.
  Where the peer's `Max_Segments_Accepted` is known, a request needing more segments than that is now failed locally
  rather than attempted. The property is not carried by I-Am and is read only by
  `DiscoveryUtils.getExtendedDeviceInformation`, so for a device discovered by broadcast it is unknown and such a
  request is attempted as before, which is what clause 5.4.4.1 requires. The existing `send` overloads are unchanged, so
  this is not a breaking change.
- The Device object's Max_Segments_Accepted property now defaults to 4096 rather than 2147483647, and the transport
  enforces it. This value is intended to be unrealistically large while still not being irresponsibly so. A segmented
  message longer than the property allows is aborted with `bufferOverflow` rather than assembled, closing an attack by
  which a peer could exhaust the heap. The limit is read from the property on each use, so a value written by client
  code takes effect immediately, and the 'max-segments-accepted' field of outgoing confirmed requests is now derived
  from it rather than being fixed at 'more than 64'. Clause 12.11.20 defines the property as the number of segments a
  device will accept and says nothing about how many it will send, so it does not bound outgoing messages.
- The Device object properties Max_Segments_Accepted, Segmentation_Supported and Max_APDU_Length_Accepted are no longer
  writable by remote devices, which now receive an error of `writeAccessDenied`. They declare capabilities that the
  transport enforces, and a writable Max_Segments_Accepted would have let a peer raise the limit described above and
  then send a message of any size. Table 12-13 gives the three conformance codes of O, R and R, none of which is W, so
  refusing the writes is conformant. Local code can still change them with `writePropertyInternal`.

*Version 6.2.0*

- Methods `LocalDevice.addRemoteDevice(RemoteDevice)` and `LocalDevice.removeRemoteDevice(int)` have been added to allow
  callers to manually manage the remote device cache, which is useful when a device address is known in advance rather
  than discovered via broadcast, in particular when devices cannot communicate via broadcast (i.e. WhoIs and IAm).
  Devices added this way are cached with NEVER_EXPIRE and have their extended properties fetched automatically via
  DiscoveryUtils.getExtendedDeviceInformation if the required properties are not already present in the remote device.
- LocalDevice scheduler methods: Made generic (Future) to eliminate unchecked cast warnings at call sites.

*Version 6.1.0*

- Created a new foreign device registration process that is failure-tolerant, allowing user code to determine the amount
  of time to delay before retrying a registration request after a failure. This differs from the original in that
  initial registration failures needed to be handled differently from re-registration failures.
- Previously upon a final timeout for a segmented request or response, BACnet4J would issue a segment NAK to the peer
  device. This was deemed to be inappropriate, and so was removed.
- Issues regarding the use of BACnet4J as a BBMD and initializing it with a wildcard bind address (i.e. 0.0.0.0) have
  been fixed.
- The means by which multistate text can be altered has been expanded. There are now four ways: 1) the state text
  array can be rewritten entirely, 2) the number of states can be changed, 3) the size of the array can be changed by
  writing a new value to the array's 0 index, and 4) by writing new individual state text values.
- A request to an object for all properties will now also return proprietary properties.
- The timeout for the termination of a local device can now be specified in user code.
- Bug fixes and dependency upgrades.
- The way in which new character encodings for `CharacterString`s can be added has been simplified. Thanks to
  https://github.com/balbusm for this submission.

*Version 6.0.2*

- Previously, when an IAm was received, calls to `DeviceEventListener.iAmReceived` would be called in the transport
  thread if the source remote device had already been cached. All calls are now made from a thread from a pool. This
  prevents errors where requests are made from the transport thread and simplifies client code.
- All event management objects can now handle behaviours such as alarm/event acknowledgement, instead of only intrinsic
  alarms having this ability.

*Version 6.0.1*

- Allow NULL values for daily schedule, exception schedule and schedule default
- Fix scheduling issues when TimeValue sequences are not in chronological order
- Fix schedule object using incorrect time format to trigger next update
- Add new CacheUpdate option to the startRemoteDeviceDiscovery() method of LocalDevice

*Version 6.0.0*

- fix DeviceObjectTest.timeSynchronization test to pass
- IAmRequest no longer automatically gets the extended device information this must now be done by adding an
  `IAmRequestListener` or using `DiscoveryUtils.getExtendedDeviceInformation(d1, rd);` The extended device info no
  longer retrieved is:
    * PropertyIdentifier.protocolServicesSupported
    * PropertyIdentifier.objectName
    * PropertyIdentifier.protocolVersion
    * PropertyIdentifier.vendorIdentifier
    * PropertyIdentifier.modelName
    * PropertyIdentifier.maxSegmentsAccepted
- Only update a cached device's address if the NPCI data has the source specifier flag set
- Allow overriding the ScheduledExecutorService used by the LocalDevice

*Version 5.0.2*

- Relax restriction on reading values that are invalid by only validating values when they are written to our device

*Version 5.0.1*

- Fix Door Status values for none=5, closing=6, opening=7, safetyLocked=8 limitedOpened=9

*Version 5.0.0*

- Fully BTL Certifiable
- Support up to 255 segments when sending a request and response
- Bugfix to allow setReuseAddress to work correctly when using BACnetIP
- Bugfix to ensure the propertyArrayIndex is correctly returned when reading via callback in RequestUtils
- Change to Maven Eclipse Project
- Modify RequestUtil.readProperties() to attempt to request sequenced values if they are too large and would cause a
  segmentationNotSupported response
- Modify RequestUtilsreadProperties() to optionally allow returning null values

*Version 4.1.7*

- Add support for Realtime MS/TP Linux realtime driver to handle token passing timing
- Change Vendor ID to 865 Infinite Automation Systems, Inc.

*Version 4.1.6*

- change http to https in JCenter Bintray repo in pom.xml

*Version 4.1.5*

- Reduce PropertyUtils.requestPropertiesFromDevice timeout log message to info as this message can be generated quite
  often

*Version 4.1.4*

- Fix for wrong loop condition on getting id for local device
- Fixes for reading elements of priority array

*Version 3.2.4*

- Fixing bug in SerialPortWrapper where stop bits and data bits were reversed.

*Version 3.2.3

- Removed restriction on binding LocalDevice to 0.0.0.0
- Added code to ensure DefaultTransport thread can't die from a bad expire() call
- Added code to ServiceFutureImpl to allow using timeouts
- Using timeouts in DefaultTransport for ServiceFutures

*Version 3.2 release notes*

- Added BBMD support
- Much enhanced support for acting as a foreign device
- Improved test framework

*Version 3.0 release notes*

- The ANT build system has been replaced with Gradle
- Dependencies have been removed. BACnet4J now operates without any external libs
- Support for IPv6 added
- Ad hoc test code has begun to be replaced with JUnit tests
- Blocking request calls have been replaced with non-blocking promises/callbacks
- Added implementations of many objects, including analog value, binary output, binary value, calendar, multistate
  value, notification classes, and schedules.
- Added intrinsic alarming for implemented objects
- Added COV reporting
- Many bug fixes and minor enhancements

*Version 2.0 release notes*

The networking package of this product has been pretty much entirely rewritten to support MS/TP. These changes implied
many changes to the LocalDevice public interface, so if you were using version 1.x you will need to port some code to
upgrade.

License
=======

This software is licensed under GPL. Commercial licensers can pay an upgrade fee to use this new version (2.x and later)
commercially. Please contact Infinite Automation Systems Inc for more information on
licensing: https://infiniteautomation.com/bacnet4j-open-source-bacnet-library/
