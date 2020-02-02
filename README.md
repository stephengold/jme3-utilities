# Jme3-utilities Project

The Jme3-utilities Project contains Java packages and assets, developed for
sgold's jMonkeyEngine projects, which might prove useful in similar projects.
It contains 7 sub-projects:

 1. SkyControl: the `SkyControl` library for sky simulation
 2. moon-ccbysa: assets for a realistic Moon in `SkyControl`
 3. tests: demos, examples, and test software
 4. textures: generate textures used by `SkyControl`
 5. ui: the `jme3-utilities-ui` library for building user interfaces
 6. nifty: the `jme3-utilities-nifty` library for using NiftyGUI user
    interfaces with jMonkeyEngine
 7. x: the `jme3-utilities-x` library of experimental software

The `jme3-utilities-debug` library, formerly a sub-project, is now part of
[the Heart Library](https://github.com/stephengold/Heart).

The `Minie` library, formerly a sub-project, is now a separate project
at [GitHub](https://github.com/stephengold/Minie).

The `Wes` library, formerly a sub-project, is now a separate project
at [GitHub](https://github.com/stephengold/Wes).

The `jme3-utilities-heart` library, formerly a sub-project, is now a separate project
at [GitHub](https://github.com/stephengold/Heart).

Java source code is provided under
[a FreeBSD license](https://github.com/stephengold/jme3-utilities/blob/master/license.txt).

Summary of SkyControl features:

 + sun, moon, stars, horizon haze, and up to 6 cloud layers
 + compatible with static backgrounds such as cube maps
 + high resolution textures are provided -- or customize with your own textures
 + compatible with effects such as `SimpleWater`, shadows, bloom, and cartoon edges
 + continuous and reversible motion and blending of cloud layers
 + option to foreshorten clouds near the horizon
 + continuous and reversible motion of sun, moon, and stars based on time of day
 + updater to synchronize lighting and shadows with sun, moon, and clouds
 + continuous scaling of sun, moon, and clouds
 + option for continuously variable phase of the moon
 + demonstration apps and online tutorial provided
 + complete source code provided under FreeBSD license

<a name="toc"/>

## Contents of this document

 + [Downloads](#downloads)
 + [Conventions](#conventions)
 + [History](#history)
 + [How to build Jme3-utilities from source](#build)
 + [How to add SkyControl to an existing game](#addsky)
 + [External links](#links)
 + [Acknowledgments](#acks)

<a name="downloads"/>

## Downloads

Recent releases can be downloaded from
[GitHub](https://github.com/stephengold/jme3-utilities/releases).

Maven artifacts are available from
[JFrog Bintray](https://bintray.com/stephengold/jme3utilities).

[Jump to table of contents](#toc)

<a name="conventions"/>

## Conventions

Since the Jme3-utilities Project is not associated with an Internet domain,
package names generally begin with `jme3utilities`.  Packages copied from
jMonkeyEngine and the BVH Retarget Project,
however, retain their original names, which began with `com.jme3`.

Both the source code and the pre-built libraries are compatible with JDK 7.

World coordinate system:

 + the `+X` axis points toward the northern horizon
 + the `+Y` axis points up (toward the zenith)
 + the `+Z` axis points toward the eastern horizon

[Jump to table of contents](#toc)

<a name="history"/>

## History

Since September 2015, the Jme3-utilities Project has been hosted at
[GitHub](https://github.com/stephengold/jme3-utilities).

From November 2013 to September 2015, it was hosted at
[Google Code](https://code.google.com/archive/).

Old (2014) versions of the Jme3-utilities Project can still be found in
[the jMonkeyEngine-Contributions Project](https://github.com/jMonkeyEngine-Contributions/SkyControl).

The evolution of each sub-project is chronicled in its release notes:

 + [SkyControl](https://github.com/stephengold/jme3-utilities/blob/master/SkyControl/release-notes.md)
 + [debug](https://github.com/stephengold/jme3-utilities/blob/master/debug/release-notes.md)
 + [ui](https://github.com/stephengold/jme3-utilities/blob/master/ui/release-notes.md)
 + [nifty](https://github.com/stephengold/jme3-utilities/blob/master/nifty/release-notes.md)
 + [x](https://github.com/stephengold/jme3-utilities/blob/master/x/release-notes.md)

[Jump to table of contents](#toc)

<a name="build"/>

## How to build Jme3-utilities from source

Jme-utilities currently targets Version 3.2.4 of jMonkeyEngine.
You are welcome to use the Engine without installing
its Integrated Development Environment (IDE),
but I use the IDE, so I tend to assume you will too.

### IDE setup

If you already have the IDE installed, skip to step 6.

The hardware and software requirements of the IDE are documented at
[the JME wiki](https://jmonkeyengine.github.io/wiki/jme3/requirements.html).

 1. Download a jMonkeyEngine 3.2 Software Development Kit (SDK) from
    [GitHub](https://github.com/jMonkeyEngine/sdk/releases).
 2. Install the SDK, which includes:
    + the engine itself,
    + an IDE based on NetBeans,
    + various IDE plugins, and
    + the Blender 3D application.
 3. Open the IDE.
 4. The first time you open the IDE, it prompts you to
    specify a folder for storing projects:
    + Fill in the "Folder name" text box.
    + Click on the "Set Project Folder" button.
 5. The first time you open the IDE, you should update
    all the pre-installed plugins:
    + Menu bar -> "Tools" -> "Plugins" to open the "Plugins" dialog.
    + Click on the "Update" button to open the "Plugin Installer" wizard.
    + Click on the "Next >" button.
    + After the plugins have downloaded, click "Finish".
    + The IDE will restart.
 6. In order to open the Jme3-utilities Project in the IDE (or NetBeans),
    you will need to install the `Gradle Support` plugin:
    + Menu bar -> "Tools" -> "Plugins" to open the "Plugins" dialog.
    + Click on the "Available Plugins" tab.
    + Check the box next to "Gradle Support" in the "Gradle" category.
     If this plugin isn't shown in the IDE's "Plugins" tool,
     you can download it from
     [GitHub](https://github.com/kelemen/netbeans-gradle-project/releases).
    + Click on the "Install" button to open the "Plugin Installer" wizard.
    + Click on the "Next >" button.
    + Check the box next to
     "I accept the terms in all the license agreements."
    + Click on the "Install" button.
    + When the "Verify Certificate" dialog appears,
     click on the "Continue" button.
    + Click on the "Finish" button.
    + The IDE will restart.

### Source files

Clone the Jme3-utilities repository using Git:

 1. Open the "Clone Repository" wizard in the IDE:
     + Menu bar -> "Team" -> "Git" -> "Clone..." or
     + Menu bar -> "Team" -> "Remote" -> "Clone..."
 2. For "Repository URL:" specify
    `https://github.com/stephengold/jme3-utilities.git`
 3. Clear the "User:" and "Password:" text boxes.
 4. For "Clone into:" specify a writable folder (on a local filesystem)
    that doesn't already contain "jme3-utilities".
 5. Click on the "Next >" button.
 6. Make sure the "master" remote branch is checked.
 7. Click on the "Next >" button again.
 8. Make sure the Checkout Branch is set to "master".
 9. Make sure the "Scan for NetBeans Projects after Clone" box is checked.
10. Click on the "Finish" button.
11. When the "Clone Completed" dialog appears, click on the "Open Project..."
    button.
12. Expand the root project node to reveal the 8 sub-projects.
13. To start with, I recommend opening 3 sub-projects:  "SkyControl",
    "tests", and "textures".  Select them using control-click, then click on the
    "Open" button.
14. There will be errors in the "textures" sub-project.  However, the build
    task should resolve them.

### Build the project

 1. In the "Projects" window of the IDE,
    right-click on the "tests" sub-project to select it.
 2. Select "Build".

### How to build Jme3-utilities without an IDE

 1. Install build software:
   + a Java Development Kit,
   + Gradle, and
   + Git
 2. Download and extract the source code from GitHub:
     + `git clone https://github.com/stephengold/jme3-utilities.git`
     + `cd jme3-utilities`
     + `git checkout nifty-0.9.12for32`
 3. Set the `JAVA_HOME` environment variable:
   + using Bash:  `export JAVA_HOME="` *path to your JDK* `"`
   + using Windows Command Prompt:  `set JAVA_HOME="` *path to your JDK* `"`
 4. Run the Gradle wrapper:
   + using Bash:  `./gradlew build`
   + using Windows Command Prompt:  `.\gradlew build`

After a successful build, new jars will be found in `*/build/libs`.

<a name="addsky"/>

## How to add SkyControl to an existing game

SkyControl is a reusable sky simulation for jMonkeyEngine games.

Adding it to an existing JME3 project should be a simple 6-step process:

 1. Add jme3-utilities JARs to the classpath.
 2. Disable any existing sky which might interfere with SkyControl.
 3. Add a `SkyControl` instance to some node in the scene graph.
 4. Configure the `SkyControl` instance.
 5. Enable the `SkyControl` instance.
 6. Test and tune as necessary.

[Jump to table of contents](#toc)

<a name="links"/>

## External links

  + November 2013 [SkyControl demo video](https://www.youtube.com/watch?v=FsJRM6tr3oQ)
  + January 2014 [SkyControl update video](https://www.youtube.com/watch?v=gE4wxgBIkaw)
  + A [maze game](https://github.com/stephengold/jme3-maze) that uses the Jme3-utilities libraries.
  + A [flight simulation game](https://github.com/ZoltanTheHun/SkyHussars) that uses SkyControl.

[Jump to table of contents](#toc)

<a name="acks"/>

## Acknowledgments

Like most projects, the Jme3-utilities Project builds on the work of many who
have gone before.  I therefore acknowledge the following
artists and software developers:

+ Cris (aka "t0neg0d") for creating SkyDome (which provided both an inspiration
  and a starting point for SkyControl) and also for encouraging me to run with
  it ... thank you yet again!
+ Paul Speed, for helpful insights which got me unstuck during debugging
+ Rémy Bouquet (aka "nehon") for many helpful insights
+ Alexandr Brui (aka "javasabr") for a solving a problem with the
  de-serialization of `SkyControl`
+ the brave souls who volunteered to be alpha testers for SkyControl, including:
    + Davis Rollman
    + "Lockhead"
    + Jonatan Dahl
    + Mindaugas (aka "eraslt")
    + Thomas Kluge
    + "pixelapp"
    + Roger (aka "stenb")
+ the beta testers for SkyControl, including:
    + "madjack"
    + Benjamin D.
    + "Fissll"
    + Davis Rollman
+ users who found and reported bugs in later versions:
    + Anton Starastsin (aka "Antonystar")
+ the creators of (and contributors to) the following software:
    + Adobe Photoshop Elements
    + the Ant and Gradle build tools
    + the Blender 3-D animation suite
    + the FindBugs source-code analyzer
    + Gimp, the GNU Image Manipulation Program
    + the Git and Subversion revision-control systems
    + the Google Chrome web browser
    + Guava core libraries for Java
    + the Java compiler, standard doclet, and runtime environment
    + the JCommander Java framework
    + jMonkeyEngine and the jME3 Software Development Kit
    + the Linux Mint operating system
    + LWJGL, the Lightweight Java Game Library
    + the Markdown document conversion tool
    + Microsoft Windows
    + the NetBeans integrated development environment
    + the Nifty graphical user interface library
    + Open Broadcaster Software Studio
    + the PMD source-code analyzer
    + the RealWorld Cursor Editor
    + Alex Peterson's Spacescape tool
    + the WinMerge differencing and merging tool

Many of SkyControl's assets were based on the works of others who licensed their
works under liberal terms or contributed them to the public domain.
For this I thank:

+ Cris (aka "t0neg0d")
+ Jacques Descloitres, MODIS Rapid Response Team, NASA/GSFC
+ Tom Ruen

I am grateful to JFrog, Google, and Github] for providing free hosting for the
Jme3-utilities Project and many other open-source projects.

I'm also grateful to Quinn (for lending me one of her microphones) and finally
my dear Holly, for keeping me sane.

If I've misattributed anything or left anyone out, please let me know so I can
correct the situation: sgold@sonic.net

[Jump to table of contents](#toc)