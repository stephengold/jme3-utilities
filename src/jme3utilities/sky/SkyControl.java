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
package jme3utilities.sky;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.texture.Texture;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyColor;
import jme3utilities.math.MyMath;

/**
 * Simple control to simulate a dynamic sky using assets and techniques derived
 * from
 * http://code.google.com/p/jmonkeyplatform-contributions/source/browse/trunk/SkyDome
 * <p>
 * While not astronomically accurate, the simulation approximates the motion of
 * the sun and moon as seen from Earth. The coordinate system is: +X=north
 * horizon, +Y=zenith (up), and +Z=east horizon. The sun crosses the meridian at
 * noon (12:00 hours).
 * <p>
 * The control is disabled at creation. When enabled, it attaches a "sky" node
 * to the controlled spatial, which must be a scene-graph node. For best
 * results, place the scene's main light, ambient light, and shadow
 * filters/renderers under simulation control by adding them to the Updater.
 * <p>
 * The "top" dome is oriented so that its rim is parallel to the horizon. The
 * top dome implements the sun, moon, clear sky color, and horizon haze. Object
 * 0 is the sun, and object 1 is the moon.
 * <p>
 * This control simulates up to six layers of clouds. The cloud density may be
 * adjusted by invoking setCloudiness(). The rate of cloud motion may be
 * adjusted by invoking setCloudsSpeed(). Flatten the clouds for best results;
 * this puts them on a translucent "clouds only" dome.
 * <p>
 * To simulate star motion, several more domes are added: one for northern
 * stars, one for southern stars, and an optional "bottom" dome which extends
 * the horizon haze for scenes with a low horizon.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SkyControl
        extends SkyControlCore {
    // *************************************************************************
    // constants

    /**
     * base color of the daytime sky: pale blue
     */
    final private static ColorRGBA colorDay = new ColorRGBA(
            0.4f, 0.6f, 1f, Constants.alphaMax);
    /**
     * light color and intensity for full moonlight: bluish gray
     */
    final private static ColorRGBA moonLight = new ColorRGBA(
            0.4f, 0.4f, 0.6f, Constants.alphaMax);
    /**
     * light color and intensity for moonless night: nearly black
     */
    final private static ColorRGBA starLight = new ColorRGBA(
            0.03f, 0.03f, 0.03f, Constants.alphaMax);
    /**
     * light color and intensity for full sunlight: yellowish white
     */
    final private static ColorRGBA sunLight = new ColorRGBA(
            0.8f, 0.8f, 0.75f, Constants.alphaMax);
    /**
     * color blended in around sunrise and sunset: ruddy orange
     */
    final private static ColorRGBA twilight = new ColorRGBA(
            0.6f, 0.3f, 0.15f, Constants.alphaMax);
    /**
     * extent of the twilight periods before sunrise and after sunset, expressed
     * as the sine of the sun's angle below the horizon (&le;1, &ge;0)
     */
    final private static float limitOfTwilight = 0.1f;
    /**
     * object index for the moon
     */
    final public static int moonIndex = 1;
    /**
     * object index for the sun
     */
    final public static int sunIndex = 0;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SkyControl.class.getName());
    /**
     * light direction for starlight: don't make this perfectly vertical because
     * that might cause shadow map aliasing
     */
    final private static Vector3f starlightDirection = new Vector3f(
            1f, 9f, 1f).normalizeLocal();
    // *************************************************************************
    // fields
    
    /**
     * true if clouds modulate the main light, false for steady light (the
     * default)
     */
    private boolean cloudModulationFlag = false;
    /**
     * texture scale for moon images; larger value gives a larger moon
     * <p>
     * The default value (0.02) exaggerates the moon's size by a factor of 8.
     */
    private float moonScale = 0.02f;
    /**
     * texture scale for sun images; larger value would give a larger sun
     * <p>
     * The default value (0.08) exaggerates the sun's size by a factor of 8.
     */
    private float sunScale = 0.08f;
    /**
     * off-screen renderer for the moon
     */
    private GlobeRenderer moonRenderer = null;
    /**
     * phase of the moon: default is FULL
     */
    private LunarPhase phase = LunarPhase.FULL;
    /**
     * orientations of the sun and stars relative to the observer
     */
    private SunAndStars sunAndStars = null;
    /**
     * lights, shadows, and viewports to update
     */
    private Updater updater = null;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor for serialization purposes only. Do not invoke
     * directly!
     */
    public SkyControl() {
        super();
    }

    /**
     * Instantiate a disabled control for no clouds, full moon, no cloud
     * modulation, no lights, no shadows, and no viewports. For a visible sky,
     * the control must be (1) added to a node of the scene graph and (2)
     * enabled.
     *
     * @param assetManager for loading textures and material definitions (not
     * null)
     * @param camera the application's camera (not null)
     * @param cloudFlattening the oblateness (ellipticity) of the dome with the
     * clouds: 0 &rarr; no flattening (hemisphere), 1 &rarr; maximum flattening
     * @param starMotion true to simulate moving stars, false for fixed stars
     * @param bottomDome true to create a material and geometry for the
     * hemisphere below the horizon, false to leave this hemisphere to
     * background color (if starMotionFlag=false) or stars (if
     * starMotionFlag=true)
     */
    public SkyControl(AssetManager assetManager, Camera camera,
            float cloudFlattening, boolean starMotion, boolean bottomDome) {
        super(assetManager, camera, cloudFlattening, starMotion, bottomDome);

        sunAndStars = new SunAndStars();
        updater = new Updater();
        setPhase(phase);
        setSunStyle("Textures/skies/suns/hazy-disc.png");

        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Compute the direction to the center of the moon.
     *
     * @return new unit vector in world (horizontal) coordinates
     */
    public Vector3f getMoonDirection() {
        float solarLongitude = sunAndStars.getSolarLongitude();
        float celestialLongitude = solarLongitude + phaseAngle;
        celestialLongitude = MyMath.modulo(celestialLongitude, FastMath.TWO_PI);
        Vector3f worldDirection = sunAndStars.convertToWorld(
                0f, celestialLongitude);

        return worldDirection;
    }

    /**
     * Access the orientations of the sun and stars.
     *
     * @return pre-existing instance
     */
    public SunAndStars getSunAndStars() {
        assert sunAndStars != null;
        return sunAndStars;
    }

    /**
     * Access the updater.
     *
     * @return pre-existing instance
     */
    public Updater getUpdater() {
        assert updater != null;
        return updater;
    }

    /**
     * Alter the cloud modulation flag.
     *
     * @param newValue true for clouds to modulate the main light, false for a
     * steady main light
     */
    public void setCloudModulation(boolean newValue) {
        cloudModulationFlag = newValue;
    }

    /**
     * Alter the angular diameter of the moon.
     *
     * @param newDiameter (in radians, &lt;Pi, &gt;0)
     */
    public void setLunarDiameter(float newDiameter) {
        if (!(newDiameter > 0f && newDiameter < FastMath.PI)) {
            logger.log(Level.SEVERE, "diameter={0}", newDiameter);
            throw new IllegalArgumentException(
                    "diameter should be between 0 and Pi");
        }

        moonScale = newDiameter * topMesh.uvScale / FastMath.HALF_PI;
    }

    /**
     * Alter the phase of the moon to a pre-set value.
     *
     * @param newPreset (or null to hide the moon)
     */
    final public void setPhase(LunarPhase newPreset) {
        if (newPreset == LunarPhase.CUSTOM) {
            setPhaseAngle(phaseAngle);
            return;
        }

        if (moonRenderer != null) {
            moonRenderer.setEnabled(false);
        }
        phase = newPreset;
        if (newPreset != null) {
            phaseAngle = newPreset.longitudeDifference();
            String assetPath = newPreset.imagePath();
            topMaterial.addObject(moonIndex, assetPath);
        }
    }

    /**
     * Customize the phase angle of the moon for off-screen rendering.
     *
     * @param newAngle (in radians, &le;2*Pi, &ge;0)
     */
    public void setPhaseAngle(float newAngle) {
        Validate.inRange(newAngle, "phase angle", 0f, FastMath.TWO_PI);
        if (moonRenderer == null) {
            throw new IllegalStateException("moon renderer not yet added");
        }

        moonRenderer.setEnabled(true);
        phase = LunarPhase.CUSTOM;
        phaseAngle = newAngle;

        Texture dynamicTexture = moonRenderer.getTexture();
        topMaterial.addObject(moonIndex, dynamicTexture);
    }

    /**
     * Alter the angular diameter of the sun.
     *
     * @param newDiameter (in radians, &lt;Pi, &gt;0)
     */
    public void setSolarDiameter(float newDiameter) {
        if (!(newDiameter > 0f && newDiameter < FastMath.PI)) {
            logger.log(Level.SEVERE, "diameter={0}", newDiameter);
            throw new IllegalArgumentException(
                    "diameter should be between 0 and Pi");
        }

        sunScale = newDiameter * topMesh.uvScale
                / (Constants.discDiameter * FastMath.HALF_PI);
    }

    /**
     * Alter the sun's color map.
     *
     * @param assetPath to new color map (not null)
     */
    final public void setSunStyle(String assetPath) {
        Validate.nonNull(assetPath, "path");

        topMaterial.addObject(sunIndex, assetPath);
    }
    // *************************************************************************
    // SkyControlCore methods

    /**
     * Create a shallow copy of this control.
     *
     * @return a new instance
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public SkyControl clone() throws CloneNotSupportedException {
        SkyControl clone = (SkyControl) super.clone();
        return clone;
    }

    /**
     * Callback invoked when the sky node's geometric state is about to be
     * updated, once per frame while attached and enabled.
     *
     * @param updateInterval time interval between updates (in seconds, &ge;0)
     */
    @Override
    public void controlUpdate(float updateInterval) {
        super.controlUpdate(updateInterval);
        updateAll();
    }

    /**
     * De-serialize this instance, for example when loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule ic = importer.getCapsule(this);

        cloudModulationFlag = ic.readBoolean("cloudModulationFlag", false);
        moonScale = ic.readFloat("moonScale", 0.02f);
        sunScale = ic.readFloat("sunScale", 0.08f);
        /* moon renderer not serialized */
        phase = ic.readEnum("phase", LunarPhase.class, LunarPhase.FULL);
        sunAndStars = (SunAndStars) ic.readSavable("sunAndStars", null);
        updater = (Updater) ic.readSavable("updater", null);
    }

    /**
     * Serialize this instance, for example when saving to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);
        OutputCapsule oc = exporter.getCapsule(this);

        oc.write(cloudModulationFlag, "cloudModulationFlag", false);
        oc.write(moonScale, "moonScale", 0.02f);
        oc.write(sunScale, "sunScale", 0.08f);
        /* moon renderer not serialized */
        oc.write(phase, "phase", LunarPhase.FULL);
        oc.write(sunAndStars, "sunAndStars", null);
        oc.write(updater, "updater", null);
    }
    // *************************************************************************
    // private methods

    /**
     * Compute where mainDirection intersects the cloud dome in the dome's local
     * coordinates, accounting for the dome's flattening and vertical offset.
     *
     * @param mainDirection (unit vector with non-negative y-component)
     * @return new unit vector
     */
    private Vector3f intersectCloudDome(Vector3f mainDirection) {
        assert mainDirection != null;
        assert mainDirection.isUnitVector() : mainDirection;
        assert mainDirection.y >= 0f : mainDirection;

        double cosSquared = MyMath.sumOfSquares(mainDirection.x,
                mainDirection.z);
        if (cosSquared == 0.0) {
            /*
             * Special case when the main light is directly overhead.
             */
            return new Vector3f(0f, 1f, 0f);
        }

        float deltaY;
        float semiMinorAxis;
        if (cloudsOnlyDome == null) {
            deltaY = 0f;
            semiMinorAxis = 1f;
        } else {
            Vector3f offset = cloudsOnlyDome.getLocalTranslation();
            assert offset.x == 0f : offset;
            assert offset.y <= 0f : offset;
            assert offset.z == 0f : offset;
            deltaY = offset.y;

            Vector3f scale = cloudsOnlyDome.getLocalScale();
            assert scale.x == 1f : scale;
            assert scale.y > 0f : scale;
            assert scale.z == 1f : scale;
            semiMinorAxis = scale.y;
        }
        /*
         * Solve for the most positive root of a quadratic equation
         * in w = sqrt(x^2 + z^2).  Use double precision arithmetic.
         */
        double cosAltitude = Math.sqrt(cosSquared);
        double tanAltitude = mainDirection.y / cosAltitude;
        double smaSquared = semiMinorAxis * semiMinorAxis;
        double a = tanAltitude * tanAltitude + smaSquared;
        assert a > 0.0 : a;
        double b = -2.0 * deltaY * tanAltitude;
        double c = deltaY * deltaY - smaSquared;
        double discriminant = MyMath.discriminant(a, b, c);
        assert discriminant >= 0.0 : discriminant;
        double w = (-b + Math.sqrt(discriminant)) / (2.0 * a);

        double distance = w / cosAltitude;
        if (distance > 1.0) {
            /*
             * Squash rounding errors.
             */
            distance = 1.0;
        }
        float x = (float) (mainDirection.x * distance);
        float y = (float) MyMath.circle(w);
        float z = (float) (mainDirection.z * distance);
        Vector3f result = new Vector3f(x, y, z);

        assert result.isUnitVector() : result;
        return result;
    }

    /**
     * Compute the clockwise (left-handed) rotation of the moon's texture
     * relative to the sky's texture.
     *
     * @param longitude the moon's celestial longitude (in radians)
     * @param uvCenter texture coordinates of the moon's center (not null)
     * @return new unit vector with its x-component equal to the cosine of the
     * rotation angle and its y-component equal to the sine of the rotation
     * angle
     */
    private Vector2f lunarRotation(float longitude, Vector2f uvCenter) {
        assert uvCenter != null;
        /*
         * Compute UV coordinates for 0.01 radians north of the center
         * of the moon.
         */
        Vector3f north = sunAndStars.convertToWorld(1f, longitude);
        Vector2f uvNorth = topMesh.directionUV(north);
        if (uvNorth != null) {
            Vector2f offset = uvNorth.subtract(uvCenter);
            assert offset.length() > 0f : offset;
            Vector2f result = offset.normalize();
            return result;
        }
        /*
         * Compute UV coordinates for 0.01 radians south of the center
         * of the moon.
         */
        Vector3f south = sunAndStars.convertToWorld(-1f, longitude);
        Vector2f uvSouth = topMesh.directionUV(south);
        if (uvSouth != null) {
            Vector2f offset = uvCenter.subtract(uvSouth);
            assert offset.length() > 0f : offset;
            Vector2f result = offset.normalize();
            return result;
        }
        assert false : south;
        return null;
    }

    /**
     * Specify a globe renderer for the moon.
     *
     * @param newRenderer (not null)
     */
    public void setMoonRenderer(GlobeRenderer newRenderer) {
        Validate.nonNull(newRenderer, "renderer");

        if (moonRenderer != null) {
            boolean enabledFlag = moonRenderer.isEnabled();
            newRenderer.setEnabled(enabledFlag);
        }
        moonRenderer = newRenderer;

        if (moonRenderer.isEnabled()) {
            Texture dynamicTexture = moonRenderer.getTexture();
            topMaterial.addObject(moonIndex, dynamicTexture);
        }
    }

    /**
     * Update astronomical objects, sky color, lighting, and stars.
     */
    private void updateAll() {
        Vector3f sunDirection = updateSun();
        /*
         * Daytime sky texture is phased in during the twilight periods
         * before sunrise and after sunset. Update the sky material's
         * clear color accordingly.
         */
        ColorRGBA clearColor = colorDay.clone();
        clearColor.a = FastMath.saturate(1f + sunDirection.y / limitOfTwilight);
        topMaterial.setClearColor(clearColor);

        Vector3f moonDirection = updateMoon();
        updateLighting(sunDirection, moonDirection);
        if (starMotionFlag && starCube != null) {
            sunAndStars.orientExternalSky(starCube, false);
        }
    }

    /**
     * Update background colors, cloud colors, haze color, sun color, lights,
     * and shadows.
     *
     * @param sunDirection world direction to the sun (length=1)
     * @param moonDirection world direction to the moon (length=1 or null)
     */
    private void updateLighting(Vector3f sunDirection, Vector3f moonDirection) {
        assert sunDirection != null;
        assert sunDirection.isUnitVector() : sunDirection;
        if (moonDirection != null) {
            assert moonDirection.isUnitVector() : moonDirection;
        }

        float sineSolarAltitude = sunDirection.y;
        float sineLunarAltitude;
        if (moonDirection != null) {
            sineLunarAltitude = moonDirection.y;
        } else {
            sineLunarAltitude = -1f;
        }
        updateObjectColors(sineSolarAltitude, sineLunarAltitude);
        /*
         * Determine the world direction to the main light source.
         */
        boolean moonUp = sineLunarAltitude >= 0f;
        boolean sunUp = sineSolarAltitude >= 0f;
        float moonWeight = getMoonIllumination();
        Vector3f mainDirection;
        if (sunUp) {
            mainDirection = sunDirection;
        } else if (moonUp && moonWeight > 0f) {
            assert moonDirection != null;
            mainDirection = moonDirection;
        } else {
            mainDirection = starlightDirection;
        }
        assert mainDirection.isUnitVector() : mainDirection;
        assert mainDirection.y >= 0f : mainDirection;
        /*
         * Determine the base color (applied to horizon haze, bottom dome, and
         * viewport backgrounds) using the sun's altitude:
         *  + sunlight when ssa >= 0.25,
         *  + twilight when ssa = 0,
         *  + blend of moonlight and starlight when ssa <= -0.04,
         * with linearly interpolated transitions.
         */
        ColorRGBA baseColor;
        if (sunUp) {
            float dayWeight = FastMath.saturate(sineSolarAltitude / 0.25f);
            baseColor = MyColor.interpolateLinear(
                    dayWeight, twilight, sunLight);
        } else {
            ColorRGBA blend;
            if (moonUp && moonWeight > 0f) {
                blend = MyColor.interpolateLinear(moonWeight, starLight,
                        moonLight);
            } else {
                blend = starLight;
            }
            float nightWeight = FastMath.saturate(-sineSolarAltitude / 0.04f);
            baseColor = MyColor.interpolateLinear(nightWeight, twilight, blend);
        }
        topMaterial.setHazeColor(baseColor);
        if (bottomMaterial != null) {
            bottomMaterial.setColor("Color", baseColor);
        }

        ColorRGBA cloudsColor = updateCloudsColor(baseColor, sunUp, moonUp);
        /*
         * Determine what fraction of the main light passes through the clouds.
         */
        float transmit;
        if (cloudModulationFlag && (sunUp || moonUp && moonWeight > 0f)) {
            /*
             * Modulate light intensity as clouds pass in front.
             */
            Vector3f intersection = intersectCloudDome(mainDirection);
            Vector2f texCoord = cloudsMesh.directionUV(intersection);
            transmit = cloudsMaterial.getTransmission(texCoord);

        } else {
            transmit = 1f;
        }
        /*
         * Determine the color and intensity of the main light.
         */
        ColorRGBA main;
        if (sunUp) {
            /*
             * By day, the main light has the base color, modulated by
             * clouds and the cube root of the sine of the sun's altitude.
             */
            float sunFactor = transmit * MyMath.cubeRoot(sineSolarAltitude);
            main = baseColor.mult(sunFactor);

        } else if (moonUp) {
            /*
             * By night, the main light is a blend of moonlight and starlight,
             * with the moon's portion modulated by clouds and the moon's phase.
             */
            float moonFactor = transmit * moonWeight;
            main = MyColor.interpolateLinear(moonFactor, starLight, moonLight);

        } else {
            main = starLight.clone();
        }
        /*
         * The ambient light color is based on the clouds color;
         * its intensity is modulated by the "slack" left by
         * strongest component of the main light.
         */
        float slack = 1f - MyMath.max(main.r, main.g, main.b);
        assert slack >= 0f : slack;
        ColorRGBA ambient = cloudsColor.mult(slack);
        /*
         * Compute the recommended shadow intensity as the fraction of
         * the total light which is directional.
         */
        float mainAmount = main.r + main.g + main.b;
        float ambientAmount = ambient.r + ambient.g + ambient.b;
        float totalAmount = mainAmount + ambientAmount;
        assert totalAmount > 0f : totalAmount;
        float shadowIntensity = FastMath.saturate(mainAmount / totalAmount);
        /*
         * Determine the recommended bloom intensity using the sun's altitude.
         */
        float bloomIntensity = 6f * sineSolarAltitude;
        bloomIntensity = FastMath.clamp(bloomIntensity, 0f, 1.7f);

        updater.update(ambient, baseColor, main, bloomIntensity,
                shadowIntensity, mainDirection);
    }

    /**
     * Update the moon's position and size.
     *
     * @return world direction to the moon (new unit vector) or null if the moon
     * is hidden
     */
    private Vector3f updateMoon() {
        if (phase == null) {
            topMaterial.hideObject(moonIndex);
            return null;
        }
        if (phase == LunarPhase.CUSTOM) {
            assert moonRenderer != null;
            float intensity = 2f + FastMath.abs(phaseAngle - FastMath.PI);
            moonRenderer.setLightIntensity(intensity);
            moonRenderer.setPhase(phaseAngle);
        }
        /*
         * Compute the UV coordinates of the center of the moon.
         */
        float solarLongitude = sunAndStars.getSolarLongitude();
        float celestialLongitude = solarLongitude + phaseAngle;
        celestialLongitude = MyMath.modulo(celestialLongitude, FastMath.TWO_PI);
        Vector3f worldDirection = sunAndStars.convertToWorld(
                0f, celestialLongitude);
        Vector2f uvCenter = topMesh.directionUV(worldDirection);

        if (uvCenter != null) {
            Vector2f rotation = lunarRotation(celestialLongitude, uvCenter);
            /*
             * Reveal the object and update its texture transform.
             */
            topMaterial.setObjectTransform(moonIndex, uvCenter, moonScale,
                    rotation);
        } else {
            topMaterial.hideObject(moonIndex);
        }

        return worldDirection;
    }

    /**
     * Update the colors of the sun and moon based on their altitudes.
     *
     * @param sineSolarAltitude (&le;1, &ge:-1)
     * @param sineLunarAltitude (&le;1, &ge:-1)
     */
    private void updateObjectColors(float sineSolarAltitude,
            float sineLunarAltitude) {
        assert sineSolarAltitude <= 1f : sineSolarAltitude;
        assert sineSolarAltitude >= -1f : sineSolarAltitude;
        assert sineLunarAltitude <= 1f : sineLunarAltitude;
        assert sineLunarAltitude >= -1f : sineLunarAltitude;
        /*
         * Update the sun's color.
         */
        float green = FastMath.saturate(3f * sineSolarAltitude);
        float blue = FastMath.saturate(sineSolarAltitude - 0.1f);
        ColorRGBA sunColor = new ColorRGBA(1f, green, blue, Constants.alphaMax);
        topMaterial.setObjectColor(sunIndex, sunColor);
        topMaterial.setObjectGlow(sunIndex, sunColor);
        /*
         * Update the moon's color.
         */
        green = FastMath.saturate(2f * sineLunarAltitude + 0.6f);
        blue = FastMath.saturate(5f * sineLunarAltitude + 0.1f);
        ColorRGBA moonColor = new ColorRGBA(
                1f, green, blue, Constants.alphaMax);
        topMaterial.setObjectColor(moonIndex, moonColor);
    }

    /**
     * Update the sun's position and size.
     *
     * @return world direction to the sun (new unit vector)
     */
    private Vector3f updateSun() {
        /*
         * Compute the UV coordinates of the center of the sun.
         */
        Vector3f worldDirection = sunAndStars.getSunDirection();
        Vector2f uv = topMesh.directionUV(worldDirection);
        if (uv == null) {
            /*
             * The sun is below the horizon, so hide it.
             */
            topMaterial.hideObject(sunIndex);
        } else {
            topMaterial.setObjectTransform(sunIndex, uv, sunScale, null);
        }

        return worldDirection;
    }
}