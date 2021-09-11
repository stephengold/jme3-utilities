# release log for the jme3-utilities-nifty library and related tests

## Version 0.9.23 released on 10 September 2021

 + Add a `MultiSelectDialog` class. (API change!)
 + Add "Select all" and "Invert selection" buttons to multi-select dialogs.
 + Add count of selected items to multi-select dialogs.
 + Include a multi-select dialog in `TestPopups`.
 + Move `TestPopups` and related classes to a new package.
 + Base on version 7.1.0 of the Heart Library.

## Version 0.9.22 released on 22 August 2021

Base on version 7.0.0 of the Heart Library and version 0.9.5 of
the jme3-utilities-ui library.

## Version 0.9.21 released on 1 July 2021

Add the "dialog", "submenu", and "tool" icon textures, from Maud.

## Version 0.9.20 released on 31 May 2021

Base on version 6.4.4 of the Heart Library and version 0.9.4 of
the jme3-utilities-ui library.

## Version 0.9.19+for34 released on 23 April 2021

Base on version 6.4.3+for34 of the Heart Library and version 0.9.3+for34 of
the jme3-utilities-ui library.

## Version 0.9.18 released on 9 February 2021

 + Publish to MavenCentral instead of JCenter.
 + Base on version 6.4.2 of the Heart Library
   and version 0.9.2 of the jme3-utilities-ui library.

## Version 0.9.17 released on 15 December 2020

 + Add a "text-and-check" dialog.
 + Base on version 6.2.0 of the Heart Library
   and version 0.9.1 of the jme3-utilities-ui library.

## Version 0.9.16 released on 9 September 2020

Add a `setNiftyColorsAsSrgb()` method to the `GuiApplication` class,
to specify that Nifty colors are defined in sRGB space. This setting is
useful when gamma correction will be enabled.

## Version 0.9.15 released on 17 August 2020

 + Remove the `getId()` method from the `WindowController` class.
 + Rename the `setActiveDialog()` method in the `PopScreenController` class.
 + Revert the fix for the multi-atlas batch render issue.
 + Bind the "ScreenShot" hotkey when editing display settings.
 + Add the `FloatSliderDialog` class.
 + Base on version 3.3.2-stable of jMonkeyEngine,
   version 6.0.0 of the Heart Library,
   and version 0.9.0 of the jme3-utilities-ui library.

## Version 0.9.14 released on 1 April 2020

 + Raise the log priorities of 3 warning messages in `GuiScreenController`.
 + Base on version 3.3.0-stable of jMonkeyEngine,
   version 5.2.1 of the Heart Library,
   and version 0.8.2 of the jme3-utilities-ui library.

## Version 0.9.13for33 released on 4 February 2020

 + Change the Maven groupId from "jme3utilities" to "com.github.stephengold"
 + Base on version 5.0 of the Heart Library

## Version 0.9.12for33 released on 4 January 2020

Base on version 4.3 of the jme3-utilities-heart library
and version 0.7.10 of the jme3-utilities-ui library.

## Version 0.9.11for33 released on 8 December 2019

 + Fix multi-atlas batch render issue (submitted by joliver82).
 + Base on version 4.2 of the jme3-utilities-heart library
   and version 0.7.9 of the jme3-utilities-ui library.

## Version 0.9.10for33 released on 23 September 2019

 + Protect `GuiEvent` no-arg constructor.
 + Base on version 4.0 of the jme3-utilities-heart library
   and version 0.7.8 of the jme3-utilities-ui library.

## Version 0.9.9for33 released on 25 August 2019

+ Disable `BasicScreenController` during cleanup.
+ Base on version 3.0 of the jme3-utilities-heart library
  and version 0.7.7 of the jme3-utilities-ui library.

## Version 0.9.8for33 released on 7 August 2019

Base on version 2.31 of the jme3-utilities-heart library
and version 0.7.6 of the jme3-utilities-ui library.

## Version 0.9.6for33 released on 5 July 2019

Base on version 3.3.0-alpha2 of jMonkeyEngine,
version 2.29 of the jme3-utilities-heart library,
and version 0.7.4 of the jme3-utilities-ui library.

## Version 0.9.5 released on 7 June 2019

 + Turn on Java 7 compatibility.
 + Base on version 1.4.3 of nifty and nifty-default-controls libraries,
   version 2.28.1 of jme3-utilities-heart library
   and version 0.7.3 of the jme3-utilities-ui library.

## Version 0.9.4 released on 28 April 2019

 + Implement an experimental fix for JME issue #99.
 + Build using Gradle v5.3.1 .

## Version 0.9.3 released on 19 March 2019

 + In `GuiScreenController`, add handlers for checkbox and slider events.
 + Base on version 3.2.3-stable of jMonkeyEngine,
   version 2.23 of the jme3-utilities-heart library,
   and version 0.7.2 of the jme3-utilities-ui library.

## Version 0.9.2 released on 10 March 2019

Base on version 2.21 of the jme3-utilities-heart library.

## Version 0.9.1 released on 13 February 2019

 + Revert to version 1.4.2 of Nifty to resolve some issues.
 + Add a `TestMultipleWindows` example app.
 + Adjust the width of the "Bind" button in `BindScreen`.

## Version 0.9.0 released on 13 January 2019

 + Add a `PopupMenuBuilder` class based on the Maud's `MenuBuilder`.
 + Add a display-settings editor screen.
 + Add a `DisplaySizeDialog` class based on Maud's `DimensionsDialog`
 + Add abstract class `Tool` for tool windows.
 + Catch exceptions while loading XML.
 + Fix an `AssertionError` that occurred after selecting from the menu
   in `TestBindScreen`.
 + Base on version 3.2.2-stable of jMonkeyEngine,
   version 2.18 of the jme3-utilities-heart library,
   and version 0.7.0 of the jme3-utilities-ui library.

## Version 0.8.3 released on 28 December 2018

 + Base on version 1.4.3 of Nifty, version 3.2.2-beta1 of jMonkeyEngine,
   version 2.17 of the jme3-utilities-heart library,
   and version 0.6.7 of the jme3-utilities-ui library.
 + Improved argument validation in the `SoundHandleJme` class.

## Version 0.8.2 released on 23 September 2018

 + Require a controller for every dialog box.
 + Rename `LibraryVersion.getVersionShort()` to `versionShort()`.
 + Based on version 2.10 of the jme3-utilities-heart library.

## Version 0.8.1 released on 14 September 2018

 + Allow modal dialog boxes to open popup menus
 + Replace `PopScreenController.hasActiveDialog()` with `getActiveDialog()`
   (API change)

## Version 0.8.0 released on 13 September 2018

 + Improve the extensibility of dialog boxes. Formerly commit-action suffixes
   were constructed in `PopScreenController.dialogCommit()` based on what
   controls the dialog contained.  That functionality has moved to the dialog
   controller, providing more flexibility.
 + Add a `MinimalDialog` class for use as a default and a superclass.
 + Use `TextEntryDialog` as a superclass for all dialog boxes based on
   `Interface/Nifty/dialogs/text-entry.xml`.

## Version 0.7.13 released on 17 August 2018

 + Add a commit description to `TextEntryDialog` class.
 + Dim the screen by 25% for each popup menu.

## Version 0.7.12 released on 24 July 2018

 + Based on version 2.5 of the jme3-utilities-heart library.
 + Remove an obsolete TODO comment.
 + Correct a typographical error in a comment.

## Version 0.7.11 released on 9 February 2018

 + Dynamically generate layouts for large infoboxes.
 + Specify a text color for small infoboxes.

## Version 0.7.10 released on 8 February 2018

 + Add `setColorBank()` methods to the `GuiScreenController` and
   `GuiWindowController` classes.
 + Add `storeResult` arguments to the `readVectorBank()` methods.

## Version 0.7.9 released on 2 February 2018

 + More flexible policies for the placement of submenus.
 + Disable warnings while loading the screen layout in `BasicScreenController`.
 + Don't pre-validate the screen layout in `BasicScreenController`.
 + Add `readColorBank()` methods to the `GuiScreenController` and
   `GuiWindowController` classes.
 + Incorporate the fix for Nifty issue #384.
 + Remove the `useControls` and `useStyles` directives from the layouts for
   simple popups.

## Version 0.7.8 released on 25 January 2018

 + Bypass the jme3-niftygui library to avoid its dependency on
   nifty-style-black.
 + Base on heart library v2.0 to avoid its dependency on jme3-bullet.

## Version 0.7.7 released on 10 January 2018

Add `disableCheckBox()` methods to the `GuiScreenController` and
 `GuiWindowController` classes.

## Version 0.7.6 released on 4 January 2018

 + Remove `disableSlider()` and `enableSlider()` methods from `GuiSceneController`.
 + Fix an assertion error triggered by clicking a mouse button in "bind" screen.
 + In "bind" screen, selecting a bound hotkey using a keypress will now select
   its action as well.
 + Add a readVectorBank() method to the GuiWindowController class.

## Version 0.7.5 released on 3 January 2018

 + Remove one of the PopScreenController.showPopupMenu() methods.
 + Add allowNull option to float/integer/long dialog controllers.
 + Privatize activateDialog() and activatePopupMenu() in PopScreenController.
 + Fix the logic error that caused FloatDialog to accept "NaN" as input.
 + Add the GuiWindowController class.
 + Add dialog controllers for booleans, doubles, and vectors.
 + Add enable/disable methods for named sliders to GuiScreenController.
 + Implement positioning policies for submenus.
 + Setting a button's text to "" will now hide the button.

## Version 0.7.4 released on 19 December 2017

Replace GuiScreenController.setButtonLabel() with setButtonText(), with
 new semantics.

## Version 0.7.3for32 released on 5 December 2017

1st release to target JME 3.2

## Version 0.7.2 released on 7 November 2017

API changes to GuiScreenController:
 + Add SliderTransform arguments to readSlider() and setSlider() methods.
 + Remove updateSlider() and updateLogSlider() methods.
 + Add readVectorBank() method.

Move dialog controllers to new package 'jme3utilities.nifty.dialog'.

## Version 0.7.1 released on 20 September 2017

 + Use commas to join indices selected by a multi-select dialog box.
 + Remove test message from PopScreenController.showMultiSelectDialog().
 + Allow a dialog box to display another dialog box.

## Version 0.7.0 released on 19 September 2017

 + Require a list or array to invoke PopScreenController.showPopupMenu().
 + Add multi-select dialog box.
 + Standardized the BSD license texts.

## Version 0.6.10 released on 5 September 2017

 + Align infobox body text with the top of its panel.
 + Accommodate infobox body text up to 400 lines long.

## Version 0.6.9 released on 3 September 2017

 + Added 2 more dialog controllers: LongDialog and TextEntryDialog.
 + Widened text-entry dialog boxes by 100 pixels.
 + In popup menus, use the main "0" key to select the 10th item.
 + Reduced display darkening for popup menus, from 50% to 37%.

## Version 0.6.8 released on 16 July 2017

+ Use wrap="true" for labels in dialog boxes and infoboxes.
+ Made the GUI render order an initialization option.

## Version 0.6.7 released on 3 July 2017

Render NiftyGUI after JME's guiNode.

## Version 0.6.6 released on 25 June 2017

Publicize constructors for dialog controllers.

## Version 0.6.5 released on 24 June 2017

+ Added simpler API for PopScreenController.showTextEntryDialog() method.
+ Add 2 dialog controllers: FloatDialog and IntegerDialog.

## Version 0.6.4 released on 20 May 2017

+ The library now depends on jme3-utilities-heart instead of SkyControl.
+ Register each window controller with its screen controller.
+ Added some methods, raised the visibility of others.

## Version 0.6.3 released on 10 May 2017

+ Added a simple confirmation dialog box.
+ Added DialogController class for customizing dialog boxes.
+ Added a feedback section to text-entry dialog box.
+ Bound the F5 key in both menu-input mode and dialog-input mode.

## Version 0.6.2 released on 2 May 2017

Public access to updateSliderStatus() in GuiScreenController class.

## Version 0.6.1 released on 27 April 2017

Added informational dialog boxes and changed APIs, renaming showDialog() and
 showPopup() methods.

## Version 0.6.0 released on 15 April 2017

Added WindowController class and revamped APIs (especially GuiScreenController)
 making most methods public and non-static.

## Version 0.5.2 released on 13 April 2017

Improved sizing of popup menus.

## Version 0.5.1 released on 12 April 2017

Fixed assertion error when activating BindScreen from a popup.

## Version 0.5.0 released on 9 April 2017

This was the initial baseline release.