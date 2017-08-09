# release notes for the jme3-utilities-ui library and related tests

## Version 0.5.7 released on 9 August 2017

 Add support for zip locators.

## Version 0.5.6 released on 7 August 2017

+ Protected flycamNames from external modification.
+ Improved handling of filesystem exceptions.
+ Added several registration methods to the Locators class.

## Version 0.5.5 released on 16 July 2017

Publicize InputMode.activate() and InputMode.deactivate() so they can be
overriden.

## Version 0.5.4 released on 15 July 2017

+ Added a Locator class to manage asset locators.
+ Added getWrittenAssetDirPath() to the ActionApplication class.
+ Made ActionApplication.filePath() return an absolute pathname.

## Version 0.5.3 released on 20 May 2017

+ The library now depends on jme3-utilities-heart instead of SkyControl.
+ Added didntHandle() callback to the ActionApplication class.

## Version 0.5.2 released on 16 May 2017

Bugfix: When disabling an InputMode, remove the listener.

## Version 0.5.1 released on 10 May 2017

Extended the Hotkey class to include mouse buttons, redesigned the API.

## Version 0.5.0 released on 9 April 2017

This was the initial baseline release.