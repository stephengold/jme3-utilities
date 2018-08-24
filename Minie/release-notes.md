# release notes for the Minie library and related tests

## Version 0.2.5 released on 24 August 2018

 + Bugfix: PhysicsDumper prints incorrect number of vehicles.
 + Bugfix for JME issue #867 contributed by Riccardo Balbo.
 + Privatized numerous protected fields.
 + Removed 3 PhysicsSpace constructors.
 + Enhanced PhysicsDumper to handle app states and print the joint list and
   (non-identity) orientation for each rigid body.
 + Added BulletAppState.getBroadphaseType().
 + Added validation of method arguments.
 + Changed BulletAppState.setWorldMax() and .setWorldMin() to avoid aliasing.

## Version 0.2.4 released on 22 August 2018

 + Renamed MinieVersion.getVersionShort() to versionShort().
 + Used MyAsset to create debug materials.
 + In BulletDebugAppState, only render viewports that are enabled.
 + Based on version 2.7 of the jme3-utilities-heart library.

## Version 0.2.3 released on 17 August 2018

+ Renamed ray-test flag accessors in PhysicsSpace class. (API change)
+ Added maxSubSteps() method to the PhysicsSpace class.
+ Include more detail when dumping a physics space.
+ Based on version 2.6 of the jme3-utilities-heart library.

## Version 0.2.2 released on 24 July 2018

+ Enhanced PhysicsDescriber to describe axes of cone shapes.
+ Based on version 2.5 of the jme3-utilities-heart library.
+ Remove an obsolete TODO comment.

## Version 0.2.1 released on 19 February 2018

+ Changed BulletDebugAppState to accept an array of viewports to add scenes to,
  instead of creating its own viewport.
+ Added getAxis() method to the ConeCollisionShape class.
+ Allow uniform scaling of capsule, cylinder, and sphere shapes.

## Version 0.2.0 released on 2 February 2018

+ Added axisIndex(), describe(), describeType(), halfExtents(), height(),
  radius(), setHalfExtents(), setHeight(), and setRadius() methods to the
  MyShape utility class.
+ Copied source files from jme3-bullet library and corrected many minor issues.

## Version 0.1.2 released on 26 January 2018

This was the initial baseline release, based largely on code formerly
included in the jme3-utilities-heart, jme3-utilities-debug, and
jme3-utilities-x libraries.