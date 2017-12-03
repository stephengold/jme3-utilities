# release notes for the jme3-utilities-heart library and related tests

## Version 1.0.0for31 released on 3 December 2017

 + Forced MySpatial.findMinMaxCoords(Spatial) to use world coordinates
 + Renamed MySpatial.findObject() to findEnabledRbc()
 + Added isAllPositive() method to MyVector3f class
 + Added axis-index constants to MyVector3f class

## Version 0.9.27 released on 21 November 2017

 + Modified API of MyVector3f.lineMeetsSphere() to use Line and center
 + Fixed assertion failure during MyMath.localizeRay()
 + Added ViewPortAppState class
 + Added mouseLine() method to MyCamera class
 + Added describeType() method to MyControl class
 + Added countBones() method to MyMesh class
 + Added cardinalizeLocal() method to MyQuaternion class
 + Added countMeshBones() and findControlledSpatial() methods to MySpatial class
 + Added firstAxis, lastAxis, and numAxes public constants to MyVector3f class
 + Added lineMeetsLine(), localizeDirection(), and normalizeLocal()
   methods to MyVector3f class

## Version 0.9.26 released on 27 October 2017

 + Added listAnimatedMeshes() and subtreeContainsAny() to MySpatial class
 + Added {cancel/get/map}Attachments() methods to MySkeleton class
 + Added UncachedKey class

## Version 0.9.25 released on 26 September 2017

 + Added caller-provided storage and used the transform of an animated geometry
   in MySkeleton.worldLocation()
 + Removed modelLocation() and 2 worldOrientation() methods from MySkeleton
 + Added more tests for ignoreTransform
 + Added support for more bones than skinning matrices in
   MyMesh.vertexLocation()
 + Made use of MyMesh.vertexVector3f() in MySpatial.findMinMaxCoords()

## Version 0.9.24 released on 22 September 2017

 + Removed getMapLocation(), moveWorld(), moveChildWorld(), rotateChild(),
   rotateObject(), and rotateY() methods from the MySpatial class
 + Added checks for ignoreTransform
 + Improved handling of non-uniform scaling and disabled RigidBodyContols
 + Added findObject(), isIgnoringTransforms(), and setWorldTransform() methods
   to the MySpatial class
 + Added MyQuaternion.isRotationIdentity() and MyVector3f.isScaleIdentity()
 + Replaced asserts with IllegalArgumentExceptions

## Version 0.9.23 released on 19 September 2017

 + Removed the MyAnimation.listAnimations(), MySkeleton.findBoneIndex(), and
   MyString.getLine() methods
 + Renamed MyAnimation.describe(Track) to describeTrackType
 + Require an anim control to invoke MyAnimation.describe(Animation,...) or
   MyAnimation.describe(Track,...)
 + Require a control to invoke MyControl.describe()
 + Changed the MyString.join() methods to handle items of any type
 + Changed MyString.sharedPrefixLength() to handle char sequences of any type
 + Added methods DomeMesh.getUVScale(), MyBone.descendsFrom(), and
   MyControl.findSkeleton() methods
 + Improved support for multiple skeletons in MySkeleton.findBone()
 + Improved support for SpatialTrack in MyAnimation.describe(Track,...) and
   also in MyAnimation.getTargetName()
 + Improved support for attach nodes in MySkeleton.setName()

## Version 0.9.22 released on 11 September 2017

 + Renamed MyControl.isValid() to canDisable()
 + Changed some asserts to exceptions in MyControl class
 + Added canApplyPhysicsLocal(), isApplyPhysicsLocal(), objectName(), and
   setApplyPhysicsLocal() to MyControl
 + Handle ChaseCamera and MotionEvent in MyControl

## Version 0.9.21 released on 8 September 2017

 + Handle bone-index buffers containing shorts instead of bytes
 + Added MyAnimation.findSpatialTrack()
 + Allow MyAnimation.get{Scales/Translations}() to return null

## Version 0.9.20 released on 7 September 2017

 + Renamed MyAnimation.findTrack() to findBoneTrack()
 + Added MyAnimation.countTracks()
 + Added MyAnimation.get{Rotations/Scales/Translations}()
 + Added class StringLoader
 + Standardized the BSD license texts

## Version 0.9.19 released on 31 August 2017

 + Removed 10 editing methods from MyAnimation
 + Renamed MyAnimation.createTrack()
 + Fixed NPE while applying MyMesh.vertexLocation() to non-animated model
 + Added accumulateScaled() method to MyQuaternion

## Version 0.9.18 released on 30 August 2017

 + Moved the hasUV() method from Misc to MyMesh
 + Fixed NPE during MyAnimation.behead()
 + Added vertex{World}Location() and vertexVector3() methods to MyMesh
 + Added count{Spatial/UserData/Vertices}() methods to MySpatial
 + Added join() method for lists of strings (not needed in Java8)
 + Added more cases to MySpatial.describeType()

## Version 0.9.17 released on 17 August 2017

 + Removed MyAnimation.blendTo() method
 + Split MyMesh utility class off from MySkeleton class
 + Added behead() and zeroFirst() methods to MyAnimation class
 + Added AlphaDiscardThreshold, Color, and UseVertexColor parameters to the
   multicolor2 material definitions

## Version 0.9.16 released on 11 August 2017

 + Added driveMap() and listZipEntries() methods to Misc
 + Added fourthRoot() and lerp() methods to MyMath
 + Added slerp(), squad(), and squadA() methods to MyQuaternion
 + Added listSkeletons() method to MySkeleton
 + Added countControls() and listControls() methods to MySpatial
 + Added addMatchPrefix() method to MyString
 + Added accumulateScaled(), eq(), ne(), and lerp() methods to MyVector3f

## Version 0.9.15 released on 6 August 2017

 + Fixed a serious logic error in Generator.nextVector3f()
 + Added 5 methods to MyQuaternion, 2 methods to MyMath, and 1 to Validate

## Version 0.9.14 released on 31 July 2017

 + Moved snapLocal() from MyMath to new MyQuaternion class
 + Added method MyMath.standardize()
 + Added methods countNe() and standardize() to MyVector3f
 + Added methods ne() and standardize() to MyQuaternion
 + Added methods distinct() and findPreviousIndex() to MyArray
 + Added 5 methods to MyAnimation
 + Added 3 methods to MySpatial

## Version 0.9.13 released on 20 July 2017

 + Fixed NPEs caused by bone tracks without scales
 + Avoid use of == and != on floats, causing semantic changes in some cases
 + Moved 3 methods from MyMath to new MyArray class
 + Privatized protected fields in DomeMesh and LoopMesh
 + Added method MyArray.hasDistinct()
 + Added method MyCamera.listViewPorts()
 + Added method MyMath.isBetween()
 + Added methods aboutEquals(), compareTo(), and equals() to ReadXZ/VectorXZ

## Version 0.9.12 released on 18 July 2017

 + Added material definition multicolor2 to render point shapes
 + Added DomeMesh and RoundedRectangle classes
 + Added another constructor for RectangleMesh
 + Added method MyMath.normalize()
 + Use Mesh.Mode.LineLoop (where feasible) when generating meshes

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