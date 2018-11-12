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

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.SceneProcessor;
import com.jme3.post.filters.DepthOfFieldFilter;
import com.jme3.post.filters.PosterizationFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
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
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyQuaternion;
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
     * separator between items in lists (not null, may be empty)
     */
    private String listSeparator = ",";
    // *************************************************************************
    // new methods exposed

    /**
     * Generate a textual description of a bone, not including its children.
     *
     * @param bone (not null, unaffected)
     * @return a description (not null, not empty)
     */
    public String describe(Bone bone) {
        StringBuilder builder = new StringBuilder(30);
        String nameText = MyString.quote(bone.getName());
        builder.append(nameText);

        String flags = "";
        if (MySkeleton.getAttachments(bone) != null) {
            flags += 'A';
        }
        if (bone.hasUserControl()) {
            flags += 'U';
        }
        if (!flags.isEmpty()) {
            builder.append(' ');
            builder.append(flags);
        }

        List<Bone> children = bone.getChildren();
        if (!children.isEmpty()) {
            int numChildren = children.size();
            String childText = String.format(" with %d child%s:", numChildren,
                    (numChildren == 1 ? "" : "ren"));
            builder.append(childText);
        }

        return builder.toString();
    }

    /**
     * Generate a textual description of a material, not including its
     * parameters.
     *
     * @param material material to describe (may be null, unaffected)
     * @return a description (not null, may be empty)
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

        result.append(" def=");
        MaterialDef def = material.getMaterialDef();
        String defName = (def == null) ? null : def.getName();
        String description = MyString.quote(defName);
        result.append(description);

        result.append(' ');
        RenderState state = material.getAdditionalRenderState();
        description = describe(state);
        result.append(description);

        Collection<MatParam> params = material.getParams();
        int numParams = params.size();
        String count = String.format(" %d parameter%s", numParams,
                (numParams == 1) ? "" : "s");
        result.append(count);

        return result.toString();
    }

    /**
     * Generate a textual description of a material parameter.
     *
     * @param matParam the material parameter to describe (not null, unaffected)
     * @return a description (not null, may be empty)
     */
    public String describe(MatParam matParam) {
        StringBuilder result = new StringBuilder(20);
        result.append(' ');
        String paramName = matParam.getName();
        result.append(paramName);
        result.append('=');
        String value = matParam.getValueAsString();
        result.append(value);

        return result.toString();
    }

    /**
     * Generate a textual description of a mesh.
     *
     * @param mesh the mesh to describe (may be null, unaffected)
     * @return a description (not null, not empty)
     */
    public String describe(Mesh mesh) {
        StringBuilder result = new StringBuilder(20);
        boolean addSeparators = false;

        String name = mesh.getClass().getSimpleName();
        result.append(name);

        result.append(" mode=");
        Mesh.Mode mode = mesh.getMode();
        result.append(mode);
        result.append(" buffers=");

        IntMap<VertexBuffer> buffers = mesh.getBuffers();
        for (IntMap.Entry<VertexBuffer> bufferEntry : buffers) {
            if (addSeparators) {
                result.append(listSeparator);
            } else {
                addSeparators = true;
            }
            VertexBuffer buffer = bufferEntry.getValue();
            VertexBuffer.Type type = buffer.getBufferType();
            result.append(type);
        }

        return result.toString();
    }

    /**
     * Generate a textual description of a skeleton, not including its bones.
     *
     * @param skeleton (not null, unaffected)
     * @return a description (not null, not empty)
     */
    public String describe(Skeleton skeleton) {
        StringBuilder builder = new StringBuilder(30);
        builder.append("Skeleton");
        Bone[] rootBones = skeleton.getRoots();
        int numRoots = rootBones.length;
        String rootsText = String.format(" with %d root bone%s", numRoots,
                (numRoots == 1) ? "" : "s");
        builder.append(rootsText);

        return builder.toString();
    }

    /**
     * Generate a textual description of an axisIndex.
     *
     * @param axisIndex (0&rarr;X, 1&rarr;Y, 2&rarr;Z)
     * @return a description (not null, not empty)
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
     * Describe the render-queue bucket to which the specified spatial is
     * assigned.
     *
     * @param spatial the spatial to describe (not null, unaffected)
     * @return a description (not null, not empty)
     */
    public String describeBucket(Spatial spatial) {
        StringBuilder result = new StringBuilder(20);
        /*
         * Describe its local assignment.
         */
        result.append("bucket=");
        RenderQueue.Bucket bucket = spatial.getLocalQueueBucket();
        result.append(bucket);
        if (bucket == RenderQueue.Bucket.Inherit) {
            /*
             * Describe its effective assignment.
             */
            result.append('/');
            bucket = spatial.getQueueBucket();
            result.append(bucket);
        }

        return result.toString();
    }

    /**
     * Generate a textual description for all controls added to the specified
     * spatial.
     *
     * @param spatial the spatial to describe (not null, unaffected)
     * @return a description (not null, may be empty)
     */
    public String describeControls(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");
        StringBuilder result = new StringBuilder(50);
        /*
         * List enabled controls first.
         */
        String enabled = describeControls(spatial, true);
        result.append(enabled);
        /*
         * List disabled controls last, in parentheses.
         */
        String disabled = describeControls(spatial, false);
        if (enabled.length() > 0 && disabled.length() > 0) {
            result.append(' ');
        }
        if (disabled.length() > 0) {
            result.append('(');
            result.append(disabled);
            result.append(')');
        }

        return result.toString();
    }

    /**
     * Generate a textual description of the view-frustum culling hints
     * associated with the specified spatial.
     *
     * @param spatial the spatial describe (not null, unaffected)
     * @return a description (not null, not empty)
     */
    public String describeCull(Spatial spatial) {
        StringBuilder result = new StringBuilder(20);
        /*
         * Describe its local cull hint.
         */
        result.append("cull=");
        Spatial.CullHint mode = spatial.getLocalCullHint();
        result.append(mode);
        if (mode == Spatial.CullHint.Inherit) {
            /*
             * Describe its effective cull hint.
             */
            result.append('/');
            mode = spatial.getCullHint();
            result.append(mode);
        }

        return result.toString();
    }

    /**
     * Generate a textual description of a filter post-processor's filters.
     *
     * @param fpp the processor to describe (not null, unaffected)
     * @return a description (not null, may be empty)
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
     * Generate a textual description the flags associated with a view port.
     *
     * @param viewPort the view port to describe (not null, unaffected)
     * @return a description (not null, not empty)
     */
    public String describeFlags(ViewPort viewPort) {
        StringBuilder result = new StringBuilder(20);

        if (!viewPort.isClearColor()) {
            result.append("NO");
        }
        result.append("clColor,");

        if (!viewPort.isClearDepth()) {
            result.append("NO");
        }
        result.append("clDepth,");

        if (!viewPort.isClearStencil()) {
            result.append("NO");
        }
        result.append("clStencil");

        return result.toString();
    }

    /**
     * Generate a textual description of the world location of the specified
     * spatial.
     *
     * @param spatial the spatial to describe (not null, unaffected)
     * @return a description (not null, may be empty)
     */
    public String describeLocation(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");
        StringBuilder result = new StringBuilder(30);

        Vector3f location = MySpatial.worldLocation(spatial, null);
        if (!MyVector3f.isZero(location)) {
            result.append("loc=");
            result.append(location);
        }

        return result.toString();
    }

    /**
     * Generate a textual description of the world orientation of the specified
     * spatial.
     *
     * @param spatial the spatial to describe (not null, unaffected)
     * @return a description (not null, may be empty)
     */
    public String describeOrientation(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");
        StringBuilder result = new StringBuilder(30);

        Quaternion orientation = MySpatial.worldOrientation(spatial, null);
        if (!MyQuaternion.isRotationIdentity(orientation)) {
            result.append("orient=");
            result.append(orientation);
        }

        return result.toString();
    }

    /**
     * Generate a textual description of the material-parameter overrides of the
     * specified spatial.
     *
     * @param spatial the spatial to describe (not null, unaffected)
     * @return a description (not null, not empty)
     */
    public String describeOverrides(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");
        StringBuilder result = new StringBuilder(20);

        result.append("mpo=(");
        boolean addSeparators = false;

        List<MatParamOverride> list = spatial.getLocalMatParamOverrides();
        for (MatParamOverride override : list) {
            if (addSeparators) {
                result.append(listSeparator);
            } else {
                addSeparators = true;
            }
            String description = describe(override);
            result.append(description);
        }
        result.append(')');

        return result.toString();
    }

    /**
     * Generate a textual description of a viewport's scene processors.
     *
     * @param viewPort the view port to describe (not null, unaffected)
     * @return a description (not null, may be empty)
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
     * Generate a textual description of the world scale of the specified
     * spatial.
     *
     * @param spatial the spatial to describe (not null, unaffected)
     * @return a description (not null, may be empty)
     */
    public String describeScale(Spatial spatial) {
        Validate.nonNull(spatial, "spatial");

        Vector3f worldScale = MySpatial.worldScale(spatial, null);
        String result = describeScale(worldScale);

        return result;
    }

    /**
     * Generate a textual description of the specified scale vector.
     *
     * @param vector the vector to describe (not null, unaffected)
     * @return a description (not null, may be empty)
     */
    public String describeScale(Vector3f vector) {
        Validate.nonNull(vector, "vector");
        StringBuilder result = new StringBuilder(30);

        if (!MyVector3f.isScaleUniform(vector)) {
            result.append("scale=");
            result.append(vector);
        } else if (!MyVector3f.isScaleIdentity(vector)) {
            /*
             * uniform scaling
             */
            result.append("scale=");
            result.append(vector.x);
        }

        return result.toString();
    }

    /**
     * Generate a textual description of the shadow modes associated with the
     * specified spatial.
     *
     * @param spatial the spatial to describe (not null, unaffected)
     * @return a description (not null, not empty)
     */
    public String describeShadow(Spatial spatial) {
        StringBuilder result = new StringBuilder(20);
        /*
         * Describe its local shadow mode.
         */
        result.append("shadow=");
        RenderQueue.ShadowMode mode = spatial.getLocalShadowMode();
        result.append(mode);
        if (mode == RenderQueue.ShadowMode.Inherit) {
            /*
             * Describe its effective shadow mode.
             */
            result.append('/');
            mode = spatial.getShadowMode();
            result.append(mode);
        }

        return result.toString();
    }

    /**
     * Generate a textual description of the user data associated with a
     * spatial.
     *
     * @param spatial the spatial to describe (not null, unaffected)
     * @return a description (not null, may be empty)
     */
    public String describeUserData(Spatial spatial) {
        StringBuilder result = new StringBuilder(50);
        boolean addSeparators = false;

        Collection<String> keys = spatial.getUserDataKeys();
        for (String key : keys) {
            if (addSeparators) {
                result.append(' ');
            } else {
                addSeparators = true;
            }
            result.append(key);
            result.append('=');
            Object value = spatial.getUserData(key);
            String valueString = MyString.escape(value.toString());
            if (value instanceof String) {
                valueString = MyString.quote(valueString);
            }
            result.append(valueString);
        }

        return result.toString();
    }

    /**
     * Read the list separator.
     *
     * @return separator text string (not null, may be empty)
     */
    public String listSeparator() {
        assert listSeparator != null;
        return listSeparator;
    }

    /**
     * Alter the list separator.
     *
     * @param newSeparator (not null, may be empty)
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
     * @param camera the camera to describe (may be null, unaffected)
     * @return a description (not null, not empty)
     * @see #describeMore(com.jme3.renderer.Camera)
     */
    protected String describe(Camera camera) {
        String result = MyCamera.describe(camera);
        return result;
    }

    /**
     * Generate a textual description of a scene-graph control.
     *
     * @param control the control to describe (not null, unaffected)
     * @return a description (not null, may be empty)
     */
    protected String describe(Control control) {
        Validate.nonNull(control, "control");
        String result = MyControl.describe(control);
        return result;
    }

    /**
     * Generate a textual description of a filter.
     *
     * @param filter the filter to describe (unaffected)
     * @return a description (not null, not empty)
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
     * @param light the light to describe (unaffected)
     * @return a description (not null, not empty)
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
     * @param lightList the list to describe (not null, unaffected)
     * @return a description (not null, may be empty)
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
     * @param override the override to describe (not null, unaffected)
     * @return a description (not null, not empty)
     */
    protected String describe(MatParamOverride override) {
        StringBuilder result = new StringBuilder(50);
        String name = override.getName();
        result.append(name);
        Object value = override.getValue();
        if (value == null) {
            result.append("=null");
        } else {
            String valueString = value.toString();
            if (valueString.length() <= 8) {
                result.append('=');
                result.append(valueString);
            }
        }

        return result.toString();
    }

    /**
     * Generate a textual description of a render state.
     *
     * @param state the view port to describe (not null, unaffected)
     * @return a description (not null, not empty)
     */
    protected String describe(RenderState state) {
        StringBuilder result = new StringBuilder(20);

        if (state.isDepthTest()) {
            result.append("NO");
        }
        result.append("dTest,");

        if (state.isDepthWrite()) {
            result.append("NO");
        }
        result.append("dWrite,");

        if (state.isWireframe()) {
            result.append("NO");
        }
        result.append("wireframe");

        return result.toString();
    }

    /**
     * Generate a textual description of a scene processor.
     *
     * @param processor the processor to describe (may be null, unaffected)
     * @return a description (not null, not empty)
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
     * Generate a textual description for a subset of the controls added to the
     * specified spatial.
     *
     * @param spatial the spatial to describe (not null, unaffected)
     * @param enabled if true, describe only the enabled controls; if false,
     * describe only the disabled controls
     * @return a description (not null, may be empty)
     */
    protected String describeControls(Spatial spatial, boolean enabled) {
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
     * Generate additional textual description of a camera.
     *
     * @param camera the camera to describe (not null, unaffected)
     * @return a description (not null, not empty)
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
     * @param spatial the spatial to describe (unaffected, may be null)
     * @return a mnemonic character
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
     * Test whether the specified scene-graph control is enabled.
     *
     * @param control the control to test (not null, unaffected)
     * @return true if the control is enabled, otherwise false
     */
    protected boolean isControlEnabled(Control control) {
        Validate.nonNull(control, "control");

        boolean result = !MyControl.canDisable(control)
                || MyControl.isEnabled(control);

        return result;
    }
}
