# Release notes for the jme3-utilities-debug library and related tests

## Version 0.9.1 released on TBD

 + Added a describeScale(Vector3f) method to the Describer class.
 + Based on version 2.8 of the heart library.

## Version 0.9.0 released on 22 August 2018

 + Replaced Dumper methods with Describer methods. (API change)
 + Renamed DebugVersion.getVersionShort() to versionShort(). (API change)
 + Renamed 4 "get" methods in AxesVisualizer. (API change)
 + Renamed 2 "get" methods in BoundsVisualizer. (API change)
 + Renamed Describer.getListSeparator() to listSeparator. (API change)
 + Renamed getHeadSize(), getLineWidth(), headColor(), and lineColor()
   in SkeletonVisualizer. (API change)
 + Renamed PerformanceAppState.getUpdateInterval() to updateInterval().
   (API change)
 + Added capability to describe depthWrite flag.
 + Generate shape textures programmatically.
 + Based on version 2.7 of the heart library.

## Version 0.8.10 released on 17 August 2018

 + Describe locations and directions of lights.
 + Added capability to describe and dump material-parameter overrides.
 + Based on version 2.6 of the heart library.

## Version 0.8.9 released on 24 July 2018

 + Based on version 2.5 of the heart library.
 + No longer need to setWireframe flag in visualizer render state.

## Version 0.8.8 released on 18 February 2018

 + Added mesh descriptions when dumping a scene graph.

## Version 0.8.7 released on 2 February 2018

 + Based on version 2.2 of the heart library.

## Version 0.8.6 released on 27 January 2018

 + Removed the interlock between PerformanceAppState and StatsAppState.
 + Added enable/disable methods to PerformanceAppState.

## Version 0.8.5 released on 25 January 2018

 + Removed all Bullet dependencies.
 + Targeted JME 3.2.1 .

## Version 0.8.4 released on 20 January 2018

 + Changed API of SkeletonVisualizer.
 + Bugfix: SkeletonVisualizer didn't apply custom colors to bone heads.
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

 + When dumping cameras, split description across two lines

## Version 0.7.0 released on 9 July 2017

 + Renamed Printer to Dumper and made major changes to its API
 + Added the capability to dump a ViewPort or RenderManager
 + Renamed SkeletonDebugControl and made minor changes to its API
 + Renamed AxesControl to AxesVisualizer

## Version 0.6.0 released on 24 June 2017

 + Added a BoundsVisualizer
 + Bug fix for AxesControl.getAxisLength(): return correct value
 + Added AxesControl.tipLocation() method

## Version 0.5.6 released on 30 May 2017

 + Handle null spatials in SkeletonDebugControl

## Version 0.5.5 released on 24 May 2017

 + Added API to change the skeleton used by a SkeletonDebugControl
 + Fixed some aliasing bugs

## Version 0.5.4 released on 22 May 2017

+ Bug fix for SkeletonDebugControl: copy the transform of animated
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