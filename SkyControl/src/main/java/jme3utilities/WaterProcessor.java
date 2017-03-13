/*
 Copyright (c) 2013-2017, Stephen Gold
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

import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.water.SimpleWaterProcessor;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Simple water processor, extended to interact with viewport listeners.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class WaterProcessor
        extends SimpleWaterProcessor {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            WaterProcessor.class.getName());
    // *************************************************************************
    // fields

    /**
     * viewport listeners registered prior to initialization
     */
    final private ArrayList<ViewPortListener> listeners = new ArrayList<>(3);
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
     * Add a viewport listener to this processor.
     *
     * @param listener (not null)
     */
    public void addListener(ViewPortListener listener) {
        Validate.nonNull(listener, "listener");

        if (isInitialized()) {
            /*
             * Inform the listener about the already-created viewports.
             */
            listener.addViewPort(reflectionView);
            listener.addViewPort(refractionView);
        } else {
            /*
             * The viewports haven't been created yet, so queue up the listener
             * to be notified after they're created.
             */
            listeners.add(listener);
        }
    }
    // *************************************************************************
    // SimpleWaterProcessor methods

    /**
     * Initialize this processor prior to its 1st update.
     *
     * @param renderManager (not null)
     * @param viewPort (not null)
     */
    @Override
    public void initialize(RenderManager renderManager, ViewPort viewPort) {
        super.initialize(renderManager, viewPort);
        /*
         * Inform registered listeners about two new viewports.
         */
        for (ViewPortListener listener : listeners) {
            listener.addViewPort(reflectionView);
            listener.addViewPort(refractionView);
        }
    }
}
