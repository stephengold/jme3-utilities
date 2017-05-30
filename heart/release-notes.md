# release notes for the jme3-utilities-heart library and related tests

## Version 0.9.8 released on TBD

 + Changed API of MySkeleton for clarity.
 + Better handling of multiple skeletons and missing skeletons in MySkeleton.

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