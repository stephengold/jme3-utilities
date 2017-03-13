# release notes for the SkyControl library and related tests

## Version 0.9.2 released on TBD

Notable changes:
 + switched build from Ant to Gradle

## Version 0.9.1 released on 8 March 2017

Major features added:
 + SkyControl is now serializable.
 + SkyControl no longer restricts the moon to the ecliptic.
 + TestSkyControl now includes a hotkey editor.
 + TestSkyControl no longer disables flyCam.

Other notable changes:
 + SkyControl puts moving stars on quads instead of domes - fewer triangles!
 + improved compatibility with jMonkeyEngine 3.1
 + App states are now based on NamedAppState instead of AbstractAppState.
 + moved noise/polygon/spline classes (not used by SkyControl) out of library

## Version 0.9.0 released on 21 January 2017

This was the initial baseline release.