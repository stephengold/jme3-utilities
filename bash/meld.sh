#!/bin/bash

set -e

S1=/home/sgold/Git/jmonkeyengine/jme3-niftygui/src/main/java
D1=/home/sgold/Git/jme3-utilities/nifty/src/main/java

/usr/bin/meld --diff $S1 $D1