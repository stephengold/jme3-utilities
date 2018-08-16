/*
 Copyright (c) 2017-2018, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.test;

import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.export.JmeExporter;
import com.jme3.export.binary.BinaryExporter;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.NameGenerator;
import jme3utilities.ui.ActionApplication;

/**
 * Simple application to test the NameGenerator class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestNameGenerator extends ActionApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TestNameGenerator.class.getName());
    /**
     * asset path to test import/export
     */
    final private static String exportAssetPath = "testNameGenerator.j3o";
    // *************************************************************************
    // new methods exposed

    /**
     * Entry point for test application.
     *
     * @param ignored command-line arguments
     */
    public static void main(String[] ignored) {
        Misc.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);

        TestNameGenerator application = new TestNameGenerator();
        application.setShowSettings(false);
        application.start();
    }

    /**
     * Initialize the application and perform tests.
     */
    @Override
    public void actionInitializeApplication() {
        System.out.printf("Test results for class NameGenerator:%n%n");

        NameGenerator example = new NameGenerator();
        System.out.printf("pristine = %s%n", example.toString());

        String apple1 = example.unique("apple");
        assert NameGenerator.isFrom(apple1, "apple");
        assert NameGenerator.getPrefix(apple1).equals("apple");

        String apple2 = example.unique("apple");
        assert NameGenerator.isFrom(apple2, "apple");
        assert NameGenerator.getPrefix(apple2).equals("apple");
        assert !apple1.equals(apple2);

        String pear1 = example.unique("pear");
        assert NameGenerator.isFrom(pear1, "pear");
        assert NameGenerator.getPrefix(pear1).equals("pear");
        assert !apple1.equals(pear1);
        assert !apple2.equals(pear1);

        String apple3 = example.unique("apple");
        assert NameGenerator.isFrom(apple3, "apple");
        assert NameGenerator.getPrefix(apple3).equals("apple");
        assert !apple2.equals(apple3);

        System.out.printf("used = %s%n", example.toString());

        String exportPath = ActionApplication.filePath(exportAssetPath);
        File exportFile = new File(exportPath);
        JmeExporter exporter = BinaryExporter.getInstance();
        try {
            exporter.save(example, exportFile);
        } catch (IOException exception) {
            assert false;
        }
        System.out.printf("Successfully exported.%n");

        example.reset();
        System.out.printf("reset = %s%n", example.toString());

        NameGenerator imported
                = (NameGenerator) assetManager.loadAsset(exportAssetPath);
        System.out.printf("imported = %s%n%n", imported.toString());

        System.out.printf("Success.%n");
        stop();
    }
}
