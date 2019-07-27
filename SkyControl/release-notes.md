# release notes for the SkyControl library and related tests

## Version 0.9.19for32 released on 26 July 2019

Notable changes:
 + base on version 2.30 of the heart library.

## Version 0.9.18 released on 7 June 2019

Notable changes:
 + base on version 2.28.1 of the heart library and target JME 3.2.3-stable

## Version 0.9.17 released on 10 March 2019

Notable changes:
 + prevent shadows from being cast by/on a `FloorControl`
 + add `LandscapeControl` (from the debug library) to the tests
 + base on version 2.21 of the heart library

## Version 0.9.16 released on 14 January 2018

Notable changes:
 + base on version 2.18 of the heart library and target JME 3.2.2-stable

## Version 0.9.15 released on 28 December 2018

Notable changes:
 + automate downloading and decompression of the star catalog
 + base on version 2.17 of the heart library and target JME 3.2.2-beta1

## Version 0.9.14 released on 23 September 2018

Notable changes:
 + rename `Constants.getVersionShort()` to `versionShort()`. (API change)
 + base on version 2.10 of the heart library

## Version 0.9.13 released on 24 July 2018

Notable changes:
 + deprecate the old constructors for `SkyControl` and `SkyControlCore`
 + rm bogus assert from `TestSkyControlRun`
 + base on version 2.5 of the heart library

## Version 0.9.12 released on 18 February 2018

Notable changes:
 + add new constructors and a `TwoDomes` stars option, for use with edge filters
 + add a `--nocubes` command-line parameter to `TestSkyControl`
 + add a cartoon-edge filter to `TestSkyControl`

## Version 0.9.11 released on 25 January 2018

Notable changes:
 + base on heart library v2.0 to make this library physics-independent

## Version 0.9.10 released on 23 January 2018

Notable changes:
 + update `SkyControl` and `FloorControl` cameras from the `RenderManager`

## Version 0.9.9 released on 22 January 2018

Notable changes:
 + target JME v3.2.1
 + give each `Updater` its own list of view ports

## Version 0.9.8 released on 14 January 2018

Notable changes:
 + fix a bug in `Updater` that caused an `IllegalArgumentException` while cloning
 + standardize the BSD license texts for tests

## Version 0.9.7for32 released on 5 December 2017

Notable changes:
 + 1st release to target JME 3.2
 + use a contrast-adjustment filter in `GlobeRenderer`
 + rename private fields

## Version 0.9.6 released on 19 September 2017

Notable changes:
 + use the `DomeMesh` class from the heart library
 + move wireframe material to heart library
 + make `SkyControl` class `JmeCloneable`
 + fix some potential aliasing bugs related to `Material.setColor()`
 + standardize the BSD license texts

## Version 0.9.5 released on 20 May 2017

Moved 2 general-purpose packages to new "jme3-utilites-heart" library, which
  this library now depends upon.

## Version 0.9.4 released on 14 May 2017

Notable changes:
 + fix a logic bug which set the main light to the wrong color

## Version 0.9.3 released on 5 May 2017

Notable changes:
 + add utility methods
 + use latest jCommander in tests
 + exclude assets from sources JAR

## Version 0.9.2 released on 9 April 2017

Notable changes:
 + fix a de-serialization bug that affected SkyControl and FloorControl
 + move all "virally licensed" assets out of the SkyControl library
 + build using Gradle instead of Ant
 + rename the {get/set}Cloud{Rate/YOffset} methods in the API

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