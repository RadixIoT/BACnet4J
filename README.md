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
        <name>Infinite Automation Snapshot Repository</name>
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
        <name>Infinite Automation Release Repository</name>
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
*Version 6.1.0*

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

- Add support for Relatime MS/TP linux realtime driver to handle token passing timing
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
