# release notes for the Minie library and related tests

## Version 0.3.1 released on TBD

 + Fixed JME issue #896 and added a test for it.
 + Disabled getMargin() and setMargin() for capsule and sphere shapes.
 + Initialized the scale and margin of compound shapes.
 + Removed various methods and arguments.
 + Added TestSetMargin to the test project.

## Version 0.3.0 released on 23 September 2018

 + Fixed JME issue #740.
 + Standardized the design of constructors and accessors to reduce aliasing
   of vectors and quaternions and enable the use of caller-allocated storage.
 + Implemented a more practical approach to filtering debug objects.
 + Simplified PhysicsCollisionEvent by eliminating event types.
 + Renamed 2 PhysicsJoint methods that misspelled "bodies".
 + Removed many needless fields, methods, and constructors.
 + Made the VehicleTuning class JmeCloneable and Savable.
 + Addressed the possibility of multiple physics controls added to the
   same Spatial.
 + Replaced 6 parameters of VehicleWheel with a VehicleTuning reference.
 + Eviscerated 5 cloneForSpatial() methods.
 + Based on version 2.10 of the jme3-utilities-heart library.

## Version 0.2.10 released on 12 September 2018

 + Fixed JME issue #898.
 + Require collision margin > 0 .
 + Changed default collision margin from 0 to 0.04 .
 + Disabled setMargin() for SphereCollisionShape.
 + Don't allow dynamic bodies to have heightfield or plane shapes.
 + Publicized loggers.
 + Added massForStatic constant in PhysicsRigidBody.
 + Added 2 tests.
 + Privatized the HeightfieldCollisionShape.createShape() method.

## Version 0.2.9 released on 9 September 2018

 + Removed PhysicsCollisionEventFactory.
 + Removed HeightfieldCollisionShape.createJmeMesh(),
   VehicleWheel.getGroundObject(), and a constructor for PhysicsGhostObject.
 + Privatized various methods.
 + Fixed JME issue #894.
 + Implemented a cleaner fix for JME issue #889.
 + Deal with scale changes in physics-debug controls.
 + Decided that physics-debug controls should implement neither JmeCloneable
   nor Savable.
 + Added validation of method arguments.
 + Finalized various fields.
 + Created the jme3utilities.minie.test package.

## Version 0.2.8 released on 3 September 2018

 + Removed some unnecessary methods.
 + Reduced the scope of many methods.
 + Renamed getMass() to mass() in MyControlP.
 + Fixed JME issue #889.
 + Added validation of method arguments, plus some assertions.
 + Based on version 2.8 of the jme3-utilities-heart library.

## Version 0.2.7 released on 1 September 2018

 + Don't setLocalScale() on spatials controlled by debug controls; this is
   related to JME issue #887.
 + Handle ignoreTransforms in GhostControl and RigidBodyControl.
 + Describe rigid bodies and RigidBodyControls similarly.
 + Describe shape scaling and spatial scaling similarly.
 + Describe the half extents of box shapes.

## Version 0.2.6 released on 31 August 2018

 + Fixed JME issues 883 and 887.
 + Ensured that debugViewPorts[] gets initialized in BulletAppState.
 + Changed AbstractPhysicsControl to handle ignoreTransform.
 + Changed DebugAppStateFilter interface to consider only Savable objects.
 + Added validation of method arguments, plus some assertions.
 + Reduced the scope of many fields and methods.
 + Finalized some fields.
 + Removed some unused fields and methods.
 + Added jme3-bullet-native runtime dependency to POM.
 + Replaced iterators with enhanced loops (for readability).
 + Standardized logging.

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