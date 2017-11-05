# release notes for the jme3-utilities-debug library and related tests

## Version 0.8.0 released 5 November 2017

 + Removed 4 public methods from the SkeletonVisualizer class
 + Added capability for SkeletonVisualizer to visualize a skeleton unrelated
   to its controlled spatial
 + Added an option for solid arrows in AxesVisualizer
 + Handled the case of spherical bounds in BoundsVisualizer
 + Added check for ignoreTransform in AxesVisualizer.tipLocation()

## Version 0.7.6 released 22 September 2017

 + Used setWorldTransform() to address the ignoreTransform issue in
   SkeletonVisualizer
 + Recognized w=-1 quaternion as a rotation identity

## Version 0.7.5 released 19 September 2017

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