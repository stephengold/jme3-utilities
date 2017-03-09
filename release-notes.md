# release notes for the jme3-utilities project (for_jME3.0 branch)

## Version 0.9.1 released on TBD

Major features added:
 + SkyControl is now serializable.
 + SkyControl no longer restricts the moon to the ecliptic.
 + TestSkyControl now includes a hotkey editor.
 + TestSkyControl no longer disables flyCam.

Other notable changes:
 + SkyControl puts moving stars on quads instead of domes - fewer triangles!
 + App states are now based on NamedAppState instead of AbstractAppState.
 + moved noise/polygon/spline classes (not used by SkyControl) to new packages
 + added 'math.locus' package for describing 3-D regions
 + added 'evo' package for implementing evolutionary algorithms

## Version 0.9.0 released on 21 January 2017

This was the initial baseline release.