# Release notes for the jme3-utilities-debug library and related tests

The `jme3-utilities-debug` library, formerly a sub-project, merged
into the `jme3-utilities-heart` library on 10 March 2019.

## Version 0.9.12 released on 10 March 2019

 + Made `Describer` and `Dumper` both `Cloneable`.
 + Added the capability to dump an `AppStateManager`.
 + Added an option to limit the number of children per `Node` in `Dumper`.
 + Terrain is no longer a special case in `Dumper`.
 + Assigned names to geometries and materials in `AxesVisualizer`.
 + Changed () to {} in `ViewPort` dumps.

## Version 0.9.11 released on 19 February 2019

 + Privatized the `SkeletonVisualizer.setSkeleton()` method. (API change)
 + Bugfix: garbled visualization after `SkeletonVisualizer.setSubject()`.
 + Based on version 2.20 of the `jme3-utilities-heart` library.

## Version 0.9.10 released on 17 February 2019

 + Removed `LandscapeControl`. (API change)
 + Excluded the `arrow.blend` file from the JAR.
 + Bugfix: `RenderState` described incorrectly.
 + Changed `SkeletonVisualizer` to work with models that do not
   contain an animated `Geometry`.
 + Eliminated all dependencies on
   the `jme3-effects` and `jme3-terrain` libraries.
 + Flag disabled `MatParamOverride`s in `Describer`.
 + Based on version 2.19 of the `jme3-utilities-heart` library.

## Version 0.9.9 released on 5 January 2019

 + Based on version 2.18 of the `jme3-utilities-heart` library.

## Version 0.9.8 released on 28 December 2018

 + Made dumps more concise.
 + Clarified `Spatial` descriptions.
 + Simplified the `PointVisualizer.setEnabled()` method.
 + Based on version 2.17 of the `jme3-utilities-heart` library.

## Version 0.9.7 released on 9 December 2018

 + Added a `PointVisualizer` class.
 + Trim trailing zeroes in the `Describer` class.
 + Added 4 sprite textures: cross, lozenge, mascle, and square.
 + Based on version 2.16 of the `jme3-utilities-heart` library.

## Version 0.9.6 released on 19 November 2018

Based on version 2.13 of the `jme3-utilities-heart` library.

## Version 0.9.5 released on 12 November 2018

 + Bugfix: `SkeletonVisualizer` custom colors not deeply cloned.
 + Added `dump()` and `describe()` methods for `Bone` and `Skeleton`.
 + Dump material parameters one-per-line and make dumping optional.
 + Based on version 2.12 of the `jme3-utilities-heart` library.

## Version 0.9.4 released on 20 October 2018

 + Added a `subject` argument to the `SkeletonVisualizer`
   constructor. (API change)
 + Bugfix: `AxisVisualizer` rescaled its controlled spatial.
 + Bugfix: custom scene-graph control caused Describer to throw an exception.
 + Bugfix: reading an `AxesVisualizer` from J3O failed due to missing
   no-arg constructor.
 + Based on version 2.11 of the `jme3-utilities-heart` library.

## Version 0.9.3 released on 23 September 2018

 + Added a texture asset for a "ring" point shape.
 + Based on version 2.10 of the `jme3-utilities-heart` library.

## Version 0.9.2 released on 12 September 2018

 + Added `setGrass()` and `setMonumentScale()` methods to the
   `LandscapeControl` class.
 + Based on version 2.9 of the jme3-utilities-heart library.

## Version 0.9.1 released on 3 September 2018

 + Added a `describeScale(Vector3f)` method to the `Describer` class.
 + Based on version 2.8 of the jme3-utilities-heart library.

## Version 0.9.0 released on 22 August 2018

 + Replaced `Dumper` methods with `Describer` methods. (API change)
 + Renamed `DebugVersion.getVersionShort()` to `versionShort()`. (API change)
 + Renamed 4 "get" methods in `AxesVisualizer`. (API change)
 + Renamed 2 "get" methods in `BoundsVisualizer`. (API change)
 + Renamed `Describer.getListSeparator()` to `listSeparator()`. (API change)
 + Renamed `getHeadSize()`, `getLineWidth()`, `headColor()`, and `lineColor()`
   in `SkeletonVisualizer`. (API change)
 + Renamed `PerformanceAppState.getUpdateInterval()` to `updateInterval()`.
   (API change)
 + Added capability to describe `depthWrite` flag.
 + Generate shape textures programmatically.
 + Based on version 2.7 of the `jme3-utilities-heart` library.

## Version 0.8.10 released on 17 August 2018

 + Describe locations and directions of lights.
 + Added capability to describe and dump material-parameter overrides.
 + Based on version 2.6 of the `jme3-utilities-heart` library.

## Version 0.8.9 released on 24 July 2018

 + Based on version 2.5 of the `jme3-utilities-heart` library.
 + No longer need to `setWireframe` flag in visualizer render state.

## Version 0.8.8 released on 18 February 2018

Added mesh descriptions when dumping a scene graph.

## Version 0.8.7 released on 2 February 2018

Based on version 2.2 of the `jme3-utilities-heart` library.

## Version 0.8.6 released on 27 January 2018

 + Removed the interlock between `PerformanceAppState` and `StatsAppState`.
 + Added enable/disable methods to `PerformanceAppState`.

## Version 0.8.5 released on 25 January 2018

 + Removed all Bullet dependencies.
 + Targeted JME 3.2.1 .

## Version 0.8.4 released on 20 January 2018

 + Changed API of `SkeletonVisualizer`.
 + Bugfix: `SkeletonVisualizer` didn't apply custom colors to bone heads.
 + Described color/intensity of lights.

## Version 0.8.3 released on 2 January 2018

 + Added the capability to set the number of arrows in an AxesVisualizer.
 + When dumping spatials, describe their materials.

## Version 0.8.2for32 released on 5 December 2017

 + 1st release to target JME 3.2
 + Described cone collision shapes

## Version 0.8.1 released on 21 November 2017

 + Modified API of AxesVisualizer to specify arrow lengths in world units
   instead of local ones
 + Fixed AxesVisualizer.tipLocation() to return correct location when the
   controlled node has non-uniform scaling

## Version 0.8.0 released on 5 November 2017

 + Removed 4 public methods from the SkeletonVisualizer class
 + Added capability for SkeletonVisualizer to visualize a skeleton unrelated
   to its controlled spatial
 + Added an option for solid arrows in AxesVisualizer
 + Handled the case of spherical bounds in BoundsVisualizer
 + Added check for ignoreTransform in AxesVisualizer.tipLocation()

## Version 0.7.6 released on 22 September 2017

 + Used setWorldTransform() to address the ignoreTransform issue in
   SkeletonVisualizer
 + Recognized w=-1 quaternion as a rotation identity

## Version 0.7.5 released on 19 September 2017

 + Added hooks for describing materials and meshes
 + Standardized the BSD license texts

## Version 0.7.4 released on 17 August 2017

 + Split Describer off from the Dumper class
 + Redesigned SkeletonVisualizer to support hiding specific bones
 + Added accessors for the update interval of PerformanceAppState

## Version 0.7.3 released on 31 July 2017

 + Made Dumper.isControlEnabled() a static method
 + Added the capability to dump physics objects/spaces

## Version 0.7.2 released on 18 July 2017

 + Renamed 5 methods in Dumper class
 + Added point shapes to SkeletonVisualizer

## Version 0.7.1 released on 13 July 2017

When dumping cameras, split description across two lines

## Version 0.7.0 released on 9 July 2017

 + Renamed Printer to Dumper and made major changes to its API
 + Added the capability to dump a ViewPort or RenderManager
 + Renamed SkeletonDebugControl and made minor changes to its API
 + Renamed AxesControl to AxesVisualizer

## Version 0.6.0 released on 24 June 2017

 + Added a BoundsVisualizer
 + Bugfix for AxesControl.getAxisLength(): return correct value
 + Added AxesControl.tipLocation() method

## Version 0.5.6 released on 30 May 2017

Handle null spatials in SkeletonDebugControl

## Version 0.5.5 released on 24 May 2017

 + Added API to change the skeleton used by a SkeletonDebugControl
 + Fixed some aliasing bugs

## Version 0.5.4 released on 22 May 2017

+ Bugfix for SkeletonDebugControl: copy the transform of animated
  geometry on every update, in case it changes
+ Added custom bone colors to SkeletonDebugControl

## Version 0.5.3 released on 20 May 2017

+ The library now depends on jme3-utilities-heart instead of SkyControl.
+ Added capability to hide lines and/or points in SkeletonDebugControl
+ Separate colors for lines and points in SkeletonDebugControl (an incompatible
    change to its API)
+ Corrected lighting in PoseDemo (needed due to a change in SkyControl)

## Version 0.5.2 released on 5 May 2017

SkeletonDebugControl no longer depends on jME3's SkeletonDebug.

## Version 0.5.1 released on 4 May 2017

Made AxesControl and SkeletonDebugControl cloneable.

## Version 0.5.0 released on 9 April 2017

This was the initial baseline release.