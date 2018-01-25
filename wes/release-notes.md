# release notes for the Wes library and related tests

## Version 0.3.2 released on TBD

 + Base on heart library v2.0 to make this library physics-independent.

## Version 0.3.1 released on 22 January 2018

 + Add chain() and delayAll() methods to the TrackEdit class.
 + Target JME v3.2.1 .

## Version 0.3.0for32 released on 5 December 2017

 + 1st release to target JME v3.2
 + Utilize setTrackSpatial() with spatial tracks

## Version 0.2.4 released on 12 November 2017

 + Add an "endWeight" argument to the TrackEdit.wrap() method: an API change.
 + Handle null skeleton in Pose.rootBoneIndices().

## Version 0.2.3 released on 8 September 2017

 + Add fallback transforms to the interpolate() and transform() methods in
   TweenTransforms, for tracks that don't include all 3 transform components.
   These are API changes.

## Version 0.2.2 released on 7 September 2017

 + Generalized BoneTrack methods to also work for SpatialTracks. (This involved
   some API changes.)
 + Renamed TweenTransforms.boneTransform() to transform().
 + Added newTrack() and setKeyframes() methods to TrackEdit class.

## Version 0.2.1 released on 4 September 2017

This was the initial baseline release.