# Jme3-utilities Project

The Jme3-utilities Project contains Java packages and assets, developed for
sgold's jMonkeyEngine projects, which might prove useful in similar projects.

It contains 4 sub-projects:

1. nifty: the `jme3-utilities-nifty` library for using NiftyGUI user
   interfaces with jMonkeyEngine
2. x: the `jme3-utilities-x` library of experimental software
3. moon-ccbysa: assets for a realistic Moon in `SkyControl`
4. tests: demos, examples, and test software

The `SkyControl` library, formerly a sub-project, is now a separate project
at [GitHub][skycontrol].

The textures sub-project is now part of
[the SkyControl Project][skycontrol].

The `jme3-utilities-heart` library, formerly a sub-project, is now a separate project
at [GitHub][heart].

The `jme3-utilities-ui` library, formerly a sub-project, is now a separate project
at [GitHub][acorus].

The `jme3-utilities-debug` library, formerly a sub-project, is now part of
[the Heart Library][heart].

The `Minie` library, formerly a sub-project, is now a separate project
at [GitHub][minie].

The `Wes` library, formerly a sub-project, is now a separate project
at [GitHub][wes].

Complete source code (in Java) is provided under
[a 3-clause BSD license][license].


<a name="toc"></a>

## Contents of this document

+ [How to build Jme3-utilities from source](#build)
+ [Downloads](#downloads)
+ [Conventions](#conventions)
+ [History](#history)
+ [Acknowledgments](#acks)


<a name="build"></a>

## How to build Jme3-utilities from source

1. Install a [Java Development Kit (JDK)][openJDK],
   if you don't already have one.
2. Point the `JAVA_HOME` environment variable to your JDK installation:
  + using Bash: `export JAVA_HOME="` *path to installation* `"`
  + using Windows Command Prompt: `set JAVA_HOME="` *path to installation* `"`
  + using PowerShell: `$env:JAVA_HOME = '` *path to installation* `'`
3. Download and extract the Jme3-utilities source code from GitHub:
  + `git clone https://github.com/stephengold/jme3-utilities.git`
  + `cd jme3-utilities`
  + `git checkout -b latest nifty-0.9.25`
4. Run the [Gradle] wrapper:
  + using Bash or PowerShell: `./gradlew build`
  + using Windows Command Prompt: `.\gradlew build`

After a successful build,
Maven artifacts will be found in `*/build/libs`.

You can install the Maven artifacts to your local Maven repository:
+ using Bash or PowerShell: `./gradlew install`
+ using Windows Command Prompt: `.\gradlew install`

You can restore the project to a pristine state:
+ using Bash or PowerShell: `./gradlew clean`
+ using Windows Command Prompt: `.\gradlew clean`

[Jump to table of contents](#toc)


<a name="downloads"></a>

## Downloads

Recent releases can be downloaded from
[GitHub](https://github.com/stephengold/jme3-utilities/releases).

Recent Maven artifacts (since nifty v0.9.18 and x v0.2.20)
are available from MavenCentral:
[nifty](https://repo1.maven.org/maven2/com/github/stephengold/jme3-utilities-nifty)
and [x](https://repo1.maven.org/maven2/com/github/stephengold/jme3-utilities-x/).

[Jump to table of contents](#toc)


<a name="conventions"></a>

## Conventions

Most package names begin with `jme3utilities`.  Packages copied from
jMonkeyEngine, however, retain their original names, which began with `com.jme3`.

The source code is compatible with JDK 7.
The pre-built libraries are compatible with JDK 8.

[Jump to table of contents](#toc)


<a name="history"></a>

## History

Since September 2015, the Jme3-utilities Project has been hosted at
[GitHub](https://github.com/stephengold/jme3-utilities).

From November 2013 to September 2015, it was hosted at
[Google Code](https://code.google.com/archive/).

The evolution of each sub-project is chronicled in its release notes:

+ [debug](https://github.com/stephengold/jme3-utilities/blob/master/debug/release-notes.md)
+ [nifty](https://github.com/stephengold/jme3-utilities/blob/master/nifty/release-notes.md)
+ [x](https://github.com/stephengold/jme3-utilities/blob/master/x/release-notes.md)

[Jump to table of contents](#toc)


<a name="acks"></a>

## Acknowledgments

Like most projects, the Jme3-utilities Project builds on the work of many who
have gone before.  I therefore acknowledge the following
software developers:

+ Paul Speed, for helpful insights which got me unstuck during debugging
+ Rémy Bouquet (aka "nehon") for many helpful insights
+ the creators of (and contributors to) the following software:
    + [Adobe Photoshop Elements][elements]
    + the [Ant] and [Gradle] build tools
    + the [FindBugs] source-code analyzer
    + the [Firefox] and [Google Chrome][chrome] web browsers
    + the [Git] and Subversion revision-control systems
    + Guava core libraries for Java
    + the [IntelliJ IDEA][idea] and [NetBeans] integrated development environments
    + the [Java] compiler, standard doclet, and virtual machine
    + the JCommander Java framework
    + [jMonkeyEngine][jme] and the jME3 Software Development Kit
    + the [Linux Mint][mint] operating system
    + LWJGL, the Lightweight Java Game Library
    + the [Markdown] document-conversion tool
    + the [Meld] visual merge tool
    + Microsoft Windows
    + the [Nifty] graphical user-interface library
    + the PMD source-code analyzer
    + the [WinMerge] differencing and merging tool

I am grateful to [GitHub], [Sonatype], [JFrog], and [Imgur]
for providing free hosting for this project
and many other open-source projects.

I'm also grateful to my dear Holly, for keeping me sane.

If I've misattributed anything or left anyone out, please let me know, so I can
correct the situation: sgold@sonic.net

[Jump to table of contents](#toc)


[acorus]: https://github.com/stephengold/Acorus "Acorus Project"
[ant]: https://ant.apache.org "Apache Ant Project"
[chrome]: https://www.google.com/chrome "Chrome"
[elements]: https://www.adobe.com/products/photoshop-elements.html "Photoshop Elements"
[findbugs]: http://findbugs.sourceforge.net "FindBugs Project"
[firefox]: https://www.mozilla.org/en-US/firefox "Firefox"
[git]: https://git-scm.com "Git"
[github]: https://github.com "GitHub"
[gradle]: https://gradle.org "Gradle Project"
[heart]: https://github.com/stephengold/Heart "Heart Project"
[idea]: https://www.jetbrains.com/idea/ "IntelliJ IDEA"
[imgur]: https://imgur.com/ "Imgur"
[java]: https://java.com "Java"
[jfrog]: https://www.jfrog.com "JFrog"
[jme]: https://jmonkeyengine.org  "jMonkeyEngine Project"
[license]: https://github.com/stephengold/jme3-utilities/blob/master/license.txt "jme3-utilities license"
[markdown]: https://daringfireball.net/projects/markdown "Markdown Project"
[meld]: https://meldmerge.org "Meld Tool"
[minie]: https://stephengold.github.io/Minie/minie/overview.html "Minie Project"
[mint]: https://linuxmint.com "Linux Mint Project"
[netbeans]: https://netbeans.org "NetBeans Project"
[nifty]: https://nifty-gui.github.io/nifty-gui "Nifty GUI Project"
[openJDK]: https://openjdk.java.net "OpenJDK Project"
[skycontrol]: https://github.com/stephengold/SkyControl "SkyControl Project"
[sonatype]: https://www.sonatype.com "Sonatype"
[utilities]: https://github.com/stephengold/jme3-utilities "Jme3-utilities Project"
[wes]: https://github.com/stephengold/Wes "Wes Project"
[winmerge]: https://winmerge.org "WinMerge Project"