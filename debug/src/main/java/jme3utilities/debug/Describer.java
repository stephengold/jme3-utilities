/*
 Copyright (c) 2013-2018, Stephen Gold
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
package jme3utilities.debug;

import com.jme3.app.state.ScreenshotAppState;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.SceneProcessor;
import com.jme3.post.filters.DepthOfFieldFilter;
import com.jme3.post.filters.PosterizationFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.Control;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.PointLightShadowFilter;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.shadow.SpotLightShadowFilter;
import com.jme3.shadow.SpotLightShadowRenderer;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.util.IntMap;
import com.jme3.water.ReflectionProcessor;
import com.jme3.water.SimpleWaterProcessor;
import com.jme3.water.SimpleWaterProcessor.RefractionProcessor;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.MyControl;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * Generate compact textual descriptions of jME3 objects.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Describer {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Describer.class.getName());
    /**
     * separator between items in lists
     */
    private String listSeparator = ",";
    // *************************************************************************
    // new methods exposed

    /**
     * Generate a textual description of a material.
     *
     * @param material material to be described (may be null, unaffected)
     * @return description (not null)
     */
    public String describe(Material material) {
        if (material == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(20);

        String name = material.getName();
        if (name == null) {
            result.append("(no name)");
        } else {
            result.append(MyString.quote(name));
        }

        MaterialDef def = material.getMaterialDef();
        String defName = def == null ? null : def.getName();
        String description = String.format(" def=%s", MyString.quote(defName));
        result.append(description);

        RenderState state = material.getAdditionalRenderState();
        if (state.isDepthTest()) {
            result.append(" depthTest");
        }
        if (state.isWireframe()) {
            result.append(" wireframe");
        }

        Collection<MatParam> params = material.getParams();
        for (MatParam param : params) {
            String paramName = param.getName();
            String value = param.getValueAsString();
            description = String.format(" %s=%s", paramName, value);
            result.append(description);
        }

        return result.toString();
    }

    /**
     * Generate a textual description of a mesh.
     *
     * @param mesh mesh to be described (may be null, unaffected)
     * @return description (not null)
     */
    public String describe(Mesh mesh) {
        StringBuilder result = new StringBuilder(20);
        boolean addSeparators = false;

        String name = mesh.getClass().getSimpleName();
        result.append(name);

        Mesh.Mode mode = mesh.getMode();
        String modeDescription = mode.toString();
        result.append(" mode=");
        result.append(modeDescription);
        result.append(" buffers=");

        IntMap<VertexBuffer> buffers = mesh.getBuffers();
        for (IntMap.Entry<VertexBuffer> bufferEntry : buffers) {
            VertexBuffer buffer = bufferEntry.getValue();
            VertexBuffer.Type type = buffer.getBufferType();
            String description = type.toString();
            if (addSeparators) {
                result.append(',');
            } else {
                addSeparators = true;
            }
            result.append(description);
        }

        return result.toString();
    }

    /**
     * Generate a textual description of an axisIndex.
     *
     * @param axisIndex (0&rarr;X, 1&rarr;Y, 2&rarr;Z)
     * @return description (not null)
     */
    public String describeAxis(int axisIndex) {
        Validate.inRange(axisIndex, "axis index", MyVector3f.firstAxis,
                MyVector3f.lastAxis);

        String result;
        switch (axisIndex) {
            case MyVector3f.xAxis:
                result = "X";
                break;
            case MyVector3f.yAxis:
                result = "Y";
                break;
            case MyVector3f.zAxis:
                result = "Z";
                break;
            default:
                throw new IllegalArgumentException();
        }

        return result;
    }

    /**
     * Generate a textual description of a spatial's controls.
     *
     * @param spatial spatial being described (not null, unaffected)
     * @param enabled if true, describe only the enabled controls; if false,
     * describe only the disabled controls
     * @return description (not null)
     */
    public String describeControls(Spatial spatial, boolean enabled) {
        StringBuilder result = new StringBuilder(20);
        boolean addSeparators = false;
        int count = spatial.getNumControls();
        for (int i = 0; i < count; i++) {
            Control control = spatial.getControl(i);
            boolean isEnabled = isControlEnabled(control);
            if (isEnabled == enabled) {
                if (addSeparators) {
                    result.append(listSeparator);
                } else {
                    addSeparators = true;
                }
                String description = describe(control);
                result.append(description);
            }
        }
        return result.toString();
    }

    /**
     * Generate a textual description of a filter post-processor's filters.
     *
     * @param fpp processor being described (not null, unaffected)
     * @return description (not null)
     */
    public String describeFilters(FilterPostProcessor fpp) {
        StringBuilder result = new StringBuilder(20);
        boolean addSeparators = false;

        List<Filter> list = fpp.getFilterList();
        for (Filter filter : list) {
            if (addSeparators) {
                result.append(listSeparator);
            } else {
                addSeparators = true;
            }
            String description = describe(filter);
            result.append(description);
        }

        return result.toString();
    }

    /**
     * Generate a textual description of a viewport's scene processors.
     *
     * @param viewPort view port being described (not null, unaffected)
     * @return description (not null)
     */
    public String describeProcessors(ViewPort viewPort) {
        StringBuilder result = new StringBuilder(20);
        boolean addSeparators = false;
        List<SceneProcessor> pList = viewPort.getProcessors();
        for (SceneProcessor processor : pList) {
            if (addSeparators) {
                result.append(listSeparator);
            } else {
                addSeparators = true;
            }
            String description = describe(processor);
            result.append(description);
        }

        return result.toString();
    }

    /**
     * Read the list separator.
     *
     * @return separator text string (not null)
     */
    public String getListSeparator() {
        assert listSeparator != null;
        return listSeparator;
    }

    /**
     * Alter the list separator.
     *
     * @param newSeparator (not null)
     */
    public void setListSeparator(String newSeparator) {
        Validate.nonNull(newSeparator, "new separator");
        listSeparator = newSeparator;
    }
    // *************************************************************************
    // new protected methods

    /**
     * Generate a textual description of a camera.
     *
     * @param camera camera to describe (unaffected)
     * @return description (not null, not empty)
     * @see #describeMore(com.jme3.renderer.Camera)
     */
    protected String describe(Camera camera) {
        String result = MyCamera.describe(camera);
        return result;
    }

    /**
     * Generate a textual description of a scene-graph control.
     *
     * @param control (not null)
     * @return description (not null)
     */
    protected String describe(Control control) {
        Validate.nonNull(control, "control");
        String result = MyControl.describe(control);
        return result;
    }

    /**
     * Generate a textual description of a filter.
     *
     * @param filter filter to describe (unaffected)
     * @return description (not null, not empty)
     */
    protected String describe(Filter filter) {
        String result;
        if (filter instanceof DepthOfFieldFilter) {
            result = "DOF";
        } else if (filter instanceof DirectionalLightShadowFilter) {
            result = "DShadow";
        } else if (filter instanceof PointLightShadowFilter) {
            result = "PShadow";
        } else if (filter instanceof PosterizationFilter) {
            result = "Poster";
        } else if (filter instanceof SpotLightShadowFilter) {
            result = "SShadow";
        } else if (filter == null) {
            result = "null";
        } else {
            result = filter.getClass().getSimpleName();
            result = result.replace("Filter", "");
            if (result.isEmpty()) {
                result = "?";
            }
        }

        return result;
    }

    /**
     * Generate a brief textual description of a light.
     *
     * @param light light to describe (unaffected)
     * @return description (not null, not empty)
     */
    protected String describe(Light light) {
        String result;
        if (light == null) {
            result = "null";
        } else {
            String name = MyString.quote(light.getName());
            ColorRGBA color = light.getColor();
            String rgb;
            if (color.r == color.g && color.g == color.b) {
                rgb = String.format("rgb=%.2f", color.r);
            } else {
                rgb = String.format("r=%.2f g=%.2f b=%.2f",
                        color.r, color.g, color.b);
            }
            if (light instanceof AmbientLight) {
                result = String.format("AL%s(%s)", name, rgb);

            } else if (light instanceof DirectionalLight) {
                Vector3f direction = ((DirectionalLight) light).getDirection();
                String dir = String.format("dx=%.2f dy=%.2f dz=%.2f",
                        direction.x, direction.y, direction.z);
                result = String.format("DL%s(%s, %s)", name, rgb, dir);

            } else if (light instanceof PointLight) {
                Vector3f location = ((PointLight) light).getPosition();
                String loc = String.format("x=%.2f y=%.2f z=%.2f",
                        location.x, location.y, location.z);
                result = String.format("PL%s(%s, %s)", name, rgb, loc);

            } else if (light instanceof SpotLight) {
                SpotLight spotLight = (SpotLight) light;
                Vector3f location = spotLight.getPosition();
                String loc = String.format("x=%.2f y=%.2f z=%.2f",
                        location.x, location.y, location.z);
                Vector3f direction = spotLight.getDirection();
                String dir = String.format("dx=%.2f dy=%.2f dz=%.2f",
                        direction.x, direction.y, direction.z);
                result = String.format("SL%s(%s, %s, %s)", name, rgb, loc, dir);

            } else {
                result = light.getClass().getSimpleName();
                if (result.isEmpty()) {
                    result = String.format("?L%s(%s)", name, rgb);
                }
            }
        }

        return result;
    }

    /**
     * Generate a textual description of a light list.
     *
     * @param lightList list to describe (not null, unaffected)
     * @return description (not null)
     */
    protected String describe(LightList lightList) {
        StringBuilder result = new StringBuilder(50);
        boolean addSeparators = false;
        for (Light light : lightList) {
            if (addSeparators) {
                result.append(listSeparator);
            } else {
                addSeparators = true;
            }
            String description = describe(light);
            result.append(description);
        }

        return result.toString();
    }

    /**
     * Generate a textual description of a material-parameter override.
     *
     * @param override override to describe (not null, unaffected)
     * @return description (not null, not empty)
     */
    protected String describe(MatParamOverride override) {
        String result = override.getName();
        Object value = override.getValue();
        if (value == null) {
            result += "=null";
        } else {
            String valueString = value.toString();
            if (valueString.length() <= 8) {
                result += String.format("=%s", valueString);
            }
        }

        return result;
    }

    /**
     * Generate a textual description of a scene processor.
     *
     * @param processor processor to describe (may be null, unaffected)
     * @return description (not null, not empty)
     */
    protected String describe(SceneProcessor processor) {
        String result;
        if (processor instanceof DirectionalLightShadowRenderer) {
            result = "DShadow";
        } else if (processor instanceof FilterPostProcessor) {
            FilterPostProcessor fpp = (FilterPostProcessor) processor;
            String desc = describeFilters(fpp);
            result = String.format("filters<%s>", desc);
        } else if (processor instanceof PointLightShadowRenderer) {
            result = "PShadow";
        } else if (processor instanceof ReflectionProcessor) {
            result = "Reflect";
        } else if (processor instanceof RefractionProcessor) {
            result = "Refract";
        } else if (processor instanceof ScreenshotAppState) {
            result = "Screenshot";
        } else if (processor instanceof SimpleWaterProcessor) {
            result = "SimpleWater";
        } else if (processor instanceof SpotLightShadowRenderer) {
            result = "SShadow";
        } else if (processor == null) {
            result = "null";
        } else {
            result = processor.getClass().getSimpleName();
            if (result.isEmpty()) {
                result = "?";
            }
        }

        return result;
    }

    /**
     * Generate additional textual description of a camera.
     *
     * @param camera camera to describe (not null, unaffected)
     * @return description (not null, not empty)
     * @see #describe(com.jme3.renderer.Camera)
     */
    protected String describeMore(Camera camera) {
        Validate.nonNull(camera, "camera");
        String result = MyCamera.describeMore(camera);
        return result;
    }

    /**
     * Generate a single-character description of a spatial.
     *
     * @param spatial spatial being described
     * @return mnemonic character
     */
    protected char describeType(Spatial spatial) {
        char result;
        if (spatial instanceof TerrainQuad) {
            result = 'q';
        } else {
            result = MySpatial.describeType(spatial);
        }

        return result;
    }

    /**
     * Test whether a scene-graph control is enabled.
     *
     * @param control control to test (not null)
     * @return true if the control is enabled, otherwise false
     */
    protected boolean isControlEnabled(Control control) {
        Validate.nonNull(control, "control");

        boolean result = MyControl.isEnabled(control);
        return result;
    }
}
