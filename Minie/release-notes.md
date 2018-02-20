# release notes for the Minie library and related tests

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