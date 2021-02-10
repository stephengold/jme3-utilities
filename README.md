# Jme3-utilities Project

The Jme3-utilities Project contains Java packages and assets, developed for
sgold's jMonkeyEngine projects, which might prove useful in similar projects.
It contains 5 sub-projects:

 1. ui: the `jme3-utilities-ui` library for building user interfaces
 2. nifty: the `jme3-utilities-nifty` library for using NiftyGUI user
    interfaces with jMonkeyEngine
 3. x: the `jme3-utilities-x` library of experimental software
 4. moon-ccbysa: assets for a realistic Moon in `SkyControl`
 5. tests: demos, examples, and test software

The `SkyControl` library, formerly a sub-project, is now a separate project
at [GitHub](https://github.com/stephengold/SkyControl).

The textures sub-project is now part of
[the SkyControl Project](https://github.com/stephengold/SkyControl).

The `jme3-utilities-heart` library, formerly a sub-project, is now a separate project
at [GitHub](https://github.com/stephengold/Heart).

The `jme3-utilities-debug` library, formerly a sub-project, is now part of
[the Heart Library](https://github.com/stephengold/Heart).

The `Minie` library, formerly a sub-project, is now a separate project
at [GitHub](https://github.com/stephengold/Minie).

The `Wes` library, formerly a sub-project, is now a separate project
at [GitHub](https://github.com/stephengold/Wes).

Complete source code (in Java) is provided under
[a 3-clause BSD license](https://github.com/stephengold/jme3-utilities/blob/master/license.txt).

<a name="toc"/>

## Contents of this document

 + [Downloads](#downloads)
 + [Conventions](#conventions)
 + [History](#history)
 + [How to build Jme3-utilities from source](#build)
 + [Acknowledgments](#acks)

<a name="downloads"/>

## Downloads

Recent releases can be downloaded from
[GitHub](https://github.com/stephengold/jme3-utilities/releases).

Recent Maven artifacts (since ui v0.9.2 and x v0.2.20)
are available from MavenCentral:
[ui](https://repo1.maven.org/maven2/com/github/stephengold/jme3-utilities-ui/)
and [x](https://repo1.maven.org/maven2/com/github/stephengold/jme3-utilities-x/).

Older Maven artifacts are available from
[JFrog Bintray](https://bintray.com/stephengold/com.github.stephengold).

[Jump to table of contents](#toc)

<a name="conventions"/>

## Conventions

Most package names begin with `jme3utilities`.  Packages copied from
jMonkeyEngine, however, retain their original names, which began with `com.jme3`.

Both the source code and the pre-built libraries are compatible with JDK 7.

[Jump to table of contents](#toc)

<a name="history"/>

## History

Since September 2015, the Jme3-utilities Project has been hosted at
[GitHub](https://github.com/stephengold/jme3-utilities).

From November 2013 to September 2015, it was hosted at
[Google Code](https://code.google.com/archive/).

The evolution of each sub-project is chronicled in its release notes:

 + [debug](https://github.com/stephengold/jme3-utilities/blob/master/debug/release-notes.md)
 + [ui](https://github.com/stephengold/jme3-utilities/blob/master/ui/release-notes.md)
 + [nifty](https://github.com/stephengold/jme3-utilities/blob/master/nifty/release-notes.md)
 + [x](https://github.com/stephengold/jme3-utilities/blob/master/x/release-notes.md)

[Jump to table of contents](#toc)

<a name="build"/>

## How to build Jme3-utilities from source

### IDE setup

 + The setup instructions in this section are for jMonkeyEngine 3.2 SDKs
   (which are based on the NetBeans 8 IDE) and aren't expected to work with
   jMonkeyEngine 3.3 SDKs (which are based on the NetBeans 11 IDE).
 + It's easy to develop jMonkeyEngine 3.3 applications on a
   jMonkeyEngine 3.2 SDK, provided you use Gradle instead of Ant.
 + If you already have a jMonkeyEngine 3.2 SDK installed, skip to step 6.

The hardware and software requirements of the IDE are documented at
[the JME wiki](https://wiki.jmonkeyengine.org/docs/3.3/getting-started/requirements.html).

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
12. Expand the root project node to reveal the 5 sub-projects.
13. To start with, I recommend opening 3 sub-projects:  "nifty",
    "tests", and "ui".  Select them using control-click, then click on the
    "Open" button.

### Build the project

 1. In the "Projects" window of the IDE,
    right-click on the "tests" sub-project to select it.
 2. Select "Build".

### How to build Jme3-utilities without an IDE

 1. Install Git and a Java Development Kit (JDK), if you don't already have one.
 2. Download and extract the source code from GitHub:
     + `git clone https://github.com/stephengold/jme3-utilities.git`
     + `cd jme3-utilities`
     + `git checkout -b latest ui-0.9.2`
 3. Set the `JAVA_HOME` environment variable:
   + using Bash:  `export JAVA_HOME="` *path to your JDK* `"`
   + using Windows Command Prompt:  `set JAVA_HOME="` *path to your JDK* `"`
 4. Run the Gradle wrapper:
   + using Bash:  `./gradlew build`
   + using Windows Command Prompt:  `.\gradlew build`

After a successful build, new jars will be found in `*/build/libs`.

<a name="acks"/>

## Acknowledgments

Like most projects, the Jme3-utilities Project builds on the work of many who
have gone before.  I therefore acknowledge the following
software developers:

+ Paul Speed, for helpful insights which got me unstuck during debugging
+ Rémy Bouquet (aka "nehon") for many helpful insights
+ the creators of (and contributors to) the following software:
    + Adobe Photoshop Elements
    + the Ant and Gradle build tools
    + the FindBugs source-code analyzer
    + the Git and Subversion revision-control systems
    + the Google Chrome web browser
    + Guava core libraries for Java
    + the Java compiler, standard doclet, and runtime environment
    + the JCommander Java framework
    + jMonkeyEngine and the jME3 Software Development Kit
    + the Linux Mint operating system
    + LWJGL, the Lightweight Java Game Library
    + the Markdown document-conversion tool
    + Microsoft Windows
    + the NetBeans integrated development environment
    + the Nifty graphical user-interface library
    + the PMD source-code analyzer
    + the RealWorld Cursor Editor
    + the WinMerge differencing and merging tool

I am grateful to GitHub, [Sonatype], JFrog, and Imgur
for providing free hosting for this project
and many other open-source projects.

I'm also grateful to my dear Holly, for keeping me sane.

If I've misattributed anything or left anyone out, please let me know so I can
correct the situation: sgold@sonic.net

[Jump to table of contents](#toc)


[sonatype]: https://www.sonatype.com "Sonatype"
