// (c) Copyright 2013 Stephen Gold <sgold@sonic.net>
// Distributed under the terms of the GNU General Public License

/*
 This file is part of the JME3 Utilities Package.

 The JME3 Utilities Package is free software: you can redistribute it and/or
 modify it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or (at your
 option) any later version.

 The JME3 Utilities Package is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.

 You should have received a copy of the GNU General Public License along with
 the JME3 Utilities Package.  If not, see <http://www.gnu.org/licenses/>.
 */
package jme3utilities.sky;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.Control;
import com.jme3.water.SimpleWaterProcessor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple water processor, extended in order to plays well with SkyControl.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class WaterProcessor
        extends SimpleWaterProcessor {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(WaterProcessor.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate a processor.
     *
     * @param manager asset manager (not null)
     */
    public WaterProcessor(AssetManager manager) {
        super(manager);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Register a sky control with this (already initialized) processor.
     *
     * @param control which control (not null)
     */
    public void addSkyControl(Control control) {
        if (control == null) {
            throw new NullPointerException("control cannot be null");
        }

        if (!isInitialized()) {
            /*
             * The viewports haven't been created yet!
             */
            logger.log(Level.WARNING, "not initialized yet");
            return;
        }
        /*
         * Notify the control about this processor's viewports.
         */
        if (!(control instanceof SkyControl)) {
            return;
        }
        SkyControl skyControl = (SkyControl) control;
        skyControl.addViewPort(reflectionView);
        skyControl.addViewPort(refractionView);
    }
    // *************************************************************************
    // SimpleWaterProcessor methods

    /**
     * Initialize this processor.
     *
     * @param renderManager (not null)
     * @param viewPort (not null)
     */
    @Override
    public void initialize(RenderManager renderManager, ViewPort viewPort) {
        super.initialize(renderManager, viewPort);

        SkyControl skyControl = reflectionScene.getControl(SkyControl.class);
        if (skyControl == null) {
            return;
        }
        /*
         * Inform the control about this processor's viewports.
         */
        skyControl.addViewPort(reflectionView);
        skyControl.addViewPort(refractionView);
    }
}