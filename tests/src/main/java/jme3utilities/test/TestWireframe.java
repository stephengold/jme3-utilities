/*
 Copyright (c) 2018, Stephen Gold
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

import com.jme3.app.SimpleApplication;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.debug.Dumper;
import jme3utilities.mesh.RectangleMesh;

/**
 * A simple application to test MyAsset.createWireframeMaterial().
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestWireframe extends SimpleApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TestWireframe.class.getName());
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the application.
     *
     * @param unused array of command-line arguments
     */
    public static void main(String[] unused) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Misc.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);

        TestWireframe app = new TestWireframe();
        app.start();
        /*
         * ... and onward to LoopMeshTest.simpleInitApp()!
         */
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Initialize the application.
     */
    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);

        Material material = MyAsset.createWireframeMaterial(assetManager,
                ColorRGBA.Green);
        Geometry geom = new Geometry("loop", new RectangleMesh());
        geom.setMaterial(material);
        rootNode.attachChild(geom);

        Dumper d = new Dumper();
        d.setDumpTransform(true);
        d.dump(renderManager);
    }
}
