/*
 Copyright (c) 2013-2014, Stephen Gold
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
package jme3utilities.sky;

import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.shadow.AbstractShadowFilter;
import com.jme3.shadow.AbstractShadowRenderer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.ViewPortListener;

/**
 * The component of SkyControl which keeps track of all the lights, shadows, and
 * viewports which the control updates. It also keeps track of the values
 * applied during the most recent update.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Updater
        implements ViewPortListener {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Updater.class.getName());
    // *************************************************************************
    // fields
    /**
     * which ambient light to update (or null for none)
     */
    private AmbientLight ambientLight = null;
    /**
     * shadow filters whose intensities are updated by the control - not
     * serialized
     */
    final private ArrayList<AbstractShadowFilter> shadowFilters =
            new ArrayList<>();
    /**
     * shadow renderers whose intensities are updated by the control - not
     * serialized
     */
    final private ArrayList<AbstractShadowRenderer> shadowRenderers =
            new ArrayList<>();
    /**
     * viewports whose background colors are updated by the control - not
     * serialized
     */
    private ArrayList<ViewPort> viewPorts = new ArrayList<>();
    /**
     * most recent color for ambient light (or null if not updated yet)
     */
    private ColorRGBA ambientColor = null;
    /**
     * most recent color for viewport background (or null if not updated yet)
     */
    private ColorRGBA backgroundColor = null;
    /**
     * most recent color for main directional light (or null if not updated yet)
     */
    private ColorRGBA mainColor = null;
    /**
     * which directional light to update (or null for none)
     */
    private DirectionalLight mainLight = null;
    /**
     * most recent shadow intensity
     */
    private float shadowIntensity = 0f;
    /**
     * most recent direction for main directional light (unit vector, or null if
     * not updated yet)
     */
    private Vector3f direction = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Add a shadow filter to the list of filters whose intensities are updated
     * by the control. Note that the list is not serialized.
     *
     * @param shadowFilter (not null)
     */
    public void addShadowFilter(AbstractShadowFilter shadowFilter) {
        if (shadowFilter == null) {
            throw new NullPointerException("shadow filter should not be null");
        }

        shadowFilters.add(shadowFilter);
    }

    /**
     * Add a shadow renderer to the list of renderers whose intensities are
     * updated by the control. Note that the list is not serialized.
     *
     * @param shadowRenderer (not null)
     */
    public void addShadowRenderer(AbstractShadowRenderer shadowRenderer) {
        if (shadowRenderer == null) {
            throw new NullPointerException(
                    "shadow renderer should not be null");
        }

        shadowRenderers.add(shadowRenderer);
    }

    /**
     * Copy the most recent color for ambient light.
     *
     * @return a new instance (or null if not updated yet)
     */
    public ColorRGBA getAmbientColor() {
        if (ambientColor == null) {
            return null;
        }
        return ambientColor.clone();
    }

    /**
     * Copy the most recent color for the viewport background.
     *
     * @return a new instance (or null if not updated yet)
     */
    public ColorRGBA getBackgroundColor() {
        if (backgroundColor == null) {
            return null;
        }
        return backgroundColor.clone();
    }

    /**
     * Copy the most recent direction for the main directional light.
     *
     * @return a new instance (or null if not updated yet)
     */
    public Vector3f getDirection() {
        if (direction == null) {
            return null;
        }
        return direction.clone();
    }

    /**
     * Copy the most recent color for the main directional light.
     *
     * @return a new instance (or null if not updated yet)
     */
    public ColorRGBA getMainColor() {
        if (mainColor == null) {
            return null;
        }
        return mainColor.clone();
    }

    /**
     * Read the most recent shadow intensity.
     *
     * @return intensity of shadows (<=1, >=0)
     */
    public float getShadowIntensity() {
        return shadowIntensity;
    }

    /**
     * Remove a shadow filter from the list of filters whose intensities are
     * updated by the control. Note that the list is not serialized.
     *
     * @param shadowFilter (not null)
     */
    public void removeShadowFilter(AbstractShadowFilter shadowFilter) {
        if (shadowFilter == null) {
            throw new NullPointerException("shadow filter should not be null");
        }

        boolean success = shadowFilters.remove(shadowFilter);
        if (!success) {
            logger.log(Level.WARNING, "not removed");
        }
    }

    /**
     * Remove a shadow renderer from the list of renderers whose intensities are
     * updated by the control. Note that the list is not serialized.
     *
     * @param shadowRenderer (not null)
     */
    public void removeShadowRenderer(AbstractShadowRenderer shadowRenderer) {
        if (shadowRenderer == null) {
            throw new NullPointerException(
                    "shadow renderer should not be null");
        }

        boolean success = shadowRenderers.remove(shadowRenderer);
        if (!success) {
            logger.log(Level.WARNING, "not removed");
        }
    }

    /**
     * Save a reference to the scene's ambient light. As long as the reference
     * has a non-null value, the control will continuously update the light's
     * color and intensity.
     *
     * @param ambientLight the scene's main ambient light (or null for none)
     */
    public void setAmbientLight(AmbientLight ambientLight) {
        this.ambientLight = ambientLight;
    }

    /**
     * Save a reference to the scene's main directional light. As long as the
     * reference has a non-null value, the control will continuously update the
     * light's color, direction, and intensity.
     *
     * @param mainLight the scene's main directional light (or null for none)
     */
    public void setMainLight(DirectionalLight mainLight) {
        this.mainLight = mainLight;
    }

    /**
     * Update all the lights, shadows, and viewports.
     *
     * @param ambientColor color and intensity of ambient light (not null, not
     * altered)
     * @param backgroundColor color of viewport backgrounds (not null, not
     * altered)
     * @param mainColor color and intensity of the main directional light (not
     * null, not altered)
     * @param shadowIntensity intensity of shadows (<=1, >=0)
     * @param direction direction to the main light source (unit vector, not
     * altered)
     */
    void update(ColorRGBA ambientColor, ColorRGBA backgroundColor,
            ColorRGBA mainColor, float shadowIntensity, Vector3f direction) {
        if (ambientColor == null) {
            throw new NullPointerException("ambient color should not be null");
        }
        if (backgroundColor == null) {
            throw new NullPointerException(
                    "background color should not be null");
        }
        if (mainColor == null) {
            throw new NullPointerException("main color should not be null");
        }
        if (shadowIntensity > 1f || shadowIntensity < 0f) {
            logger.log(Level.SEVERE, "intensity={0}", shadowIntensity);
            throw new IllegalArgumentException(
                    "shadow intensity should be between 0 and 1");
        }
        if (direction == null) {
            throw new NullPointerException("direction should not be null");
        }
        if (!direction.isUnitVector()) {
            logger.log(Level.SEVERE, "direction={0}", direction);
            throw new IllegalArgumentException(
                    "direction should be a unit vector");
        }
        /*
         * Copy new values to the corresponding "most recent" fields.
         */
        if (this.ambientColor == null) {
            this.ambientColor = ambientColor.clone();
        } else {
            this.ambientColor.set(ambientColor);
        }
        if (this.backgroundColor == null) {
            this.backgroundColor = backgroundColor.clone();
        } else {
            this.backgroundColor.set(backgroundColor);
        }
        if (this.mainColor == null) {
            this.mainColor = mainColor.clone();
        } else {
            this.mainColor.set(mainColor);
        }
        this.shadowIntensity = shadowIntensity;
        if (this.direction == null) {
            this.direction = direction.clone();
        } else {
            this.direction.set(direction);
        }

        if (mainLight != null) {
            mainLight.setColor(mainColor);
            /*
             * The direction of the main light is the direction in which it
             * propagates, which is the opposite of the direction to the
             * light source.
             */
            Vector3f propagationDirection = direction.negate();
            mainLight.setDirection(propagationDirection);
        }
        if (ambientLight != null) {
            ambientLight.setColor(ambientColor);
        }
        for (AbstractShadowFilter filter : shadowFilters) {
            filter.setShadowIntensity(shadowIntensity);
        }
        for (AbstractShadowRenderer renderer : shadowRenderers) {
            renderer.setShadowIntensity(shadowIntensity);
        }
        for (ViewPort viewPort : viewPorts) {
            viewPort.setBackgroundColor(backgroundColor);
        }
    }
    // *************************************************************************
    // ViewPortListener methods

    /**
     * Add a viewport to the list of viewports whose background colors are
     * updated by the control. Note that the list is not serialized.
     *
     * @param viewPort (not null)
     */
    @Override
    public void addViewPort(ViewPort viewPort) {
        if (viewPort == null) {
            throw new NullPointerException("view port should not be null");
        }

        viewPorts.add(viewPort);
    }

    /**
     * Remove a viewport from the list of viewports whose background colors are
     * updated by the control. Note that the list is not serialized.
     *
     * @param viewPort (not null)
     */
    @Override
    public void removeViewPort(ViewPort viewPort) {
        if (viewPort == null) {
            throw new NullPointerException("view port should not be null");
        }

        boolean success = viewPorts.remove(viewPort);
        if (!success) {
            logger.log(Level.WARNING, "not removed");
        }
    }
}