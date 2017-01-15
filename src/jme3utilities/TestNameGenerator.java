/*
 Copyright (c) 2017, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities;

import com.jme3.app.SimpleApplication;
import com.jme3.export.JmeExporter;
import com.jme3.export.binary.BinaryExporter;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static jme3utilities.NameGenerator.getPrefix;
import static jme3utilities.NameGenerator.isFrom;

/**
 * Simple application to test the NameGenerator class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestNameGenerator extends SimpleApplication {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TestNameGenerator.class.getName());

    /**
     * Entry point for test application.
     *
     * @param ignored
     */
    public static void main(String[] ignored) {
        Misc.setLoggingLevels(Level.WARNING);
        Logger.getLogger("com.jme3.audio.openal.ALAudioRenderer")
                .setLevel(Level.SEVERE);

        TestNameGenerator application = new TestNameGenerator();
        application.setShowSettings(false);
        application.start();
    }

    @Override
    public void simpleInitApp() {
        System.out.printf("Test results for class NameGenerator:%n%n");

        NameGenerator example = new NameGenerator();
        System.out.printf("pristine = %s%n", example.toString());

        String apple1 = example.unique("apple");
        assert isFrom(apple1, "apple");
        assert getPrefix(apple1).equals("apple");

        String apple2 = example.unique("apple");
        assert isFrom(apple2, "apple");
        assert getPrefix(apple2).equals("apple");
        assert !apple1.equals(apple2);

        String pear1 = example.unique("pear");
        assert isFrom(pear1, "pear");
        assert getPrefix(pear1).equals("pear");
        assert !apple1.equals(pear1);
        assert !apple2.equals(pear1);

        String apple3 = example.unique("apple");
        assert isFrom(apple3, "apple");
        assert getPrefix(apple3).equals("apple");
        assert !apple2.equals(apple3);

        System.out.printf("used = %s%n", example.toString());

        File targetFile = new File("assets/tmp.j3o");
        JmeExporter exporter = BinaryExporter.getInstance();
        try {
            exporter.save(example, targetFile);
        } catch (IOException exception) {
            assert false;
        }
        System.out.printf("Successfully exported.%n");

        example.reset();
        System.out.printf("reset = %s%n", example.toString());

        NameGenerator imported = (NameGenerator) assetManager.loadAsset(
                "tmp.j3o");
        System.out.printf("imported = %s%n%n", imported.toString());

        System.out.printf("Success.%n");
        stop();
    }
}
