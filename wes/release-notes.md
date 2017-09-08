# release notes for the Wes library and related tests

## Version 0.2.3 released TBD

 + Add fallback transforms to the interpolate() and transform() methods in
   TweenTransforms, for tracks that don't include all 3 transform components.
   These are API changes.

## Version 0.2.2 released 7 September 2017

 + Generalized BoneTrack methods to also work for SpatialTracks. (This involved
   some API changes.)
 + Renamed TweenTransforms.boneTransform() to transform().
 + Added newTrack() and setKeyframes() methods to TrackEdit class.

## Version 0.2.1 released on 4 September 2017

This was the initial baseline release.