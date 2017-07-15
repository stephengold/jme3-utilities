# release notes for the jme3-utilities-heart library and related tests

## Version 0.9.11 released on 14 July 2017

 + Fixed logic error in MyCamera.viewAspectRatio()
 + Moved mesh generators to new jme3utilities.mesh package
 + Rename Misc.getUserPath() to homePath() and generate absolute pathname
 + Added class RectangleOutlineMesh
 + Added method MyString.removeSuffix()
 + Added option to MyAsset.createWireframeMaterial() to set the point size
 + Creative use of mesh modes to save indices

## Version 0.9.10 released on 13 July 2017

 + Renamed Rectangle to RectangleMesh to avoid confusion
 + Removed Misc.isIdentity() for being redundant with MyMath.isIdentity()
 + Replaced MyCamera.aspectRatio() with {display/frustum/view}AspectRatio()
 + Added utility methods to MyCamera and Misc
 + Added LoopMesh class
 + Added simpler constructors for RectangleMesh
 + Distinguish BitmapText in MySpatial.describeType()

## Version 0.9.9 released on 7 July 2017

 + Fixed logic error in MyCamera.aspectRatio()
 + Added Rectangle class to generate meshes
 + Added utility methods to MyCamera, MyMath, MySpatial, and MyVector3f
 + Handle StatsView case in MyControl.isEnabled()

## Version 0.9.8 released on 30 May 2017

 + Changed API of MySkeleton for clarity
 + Better handling of multiple skeletons and missing skeletons in MySkeleton

## Version 0.9.7 released on 24 May 2017

 + Fixed aliasing bugs/hazards in MyAsset
 + Added methods to MyControl, MySpatial and MyString, partly from debug library

## Version 0.9.6 released on 22 May 2017

 + Added wireframe shaders and material definitions, partly from SkyControl lib
 + Added 4 new methods to the MyAnimation class

## Version 0.9.5 released on 20 May 2017

 + Changed semantics of escape(), quote(), and unescape() in MyString
 + Added new methods to MyAnimation, MySkeleton, MyVector3f
 + Removed the Indices class
 + Removed findMinMaxHeights() and getMaxY() from MySpatial
 + Created jme3-utilities-heart library by splitting off packages from the
   SkyControl and jme3-utilities-x libraries