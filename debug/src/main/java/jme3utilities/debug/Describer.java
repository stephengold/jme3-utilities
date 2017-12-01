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
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.ConeCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
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
import com.jme3.scene.control.Control;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.PointLightShadowFilter;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.shadow.SpotLightShadowFilter;
import com.jme3.shadow.SpotLightShadowRenderer;
import com.jme3.water.ReflectionProcessor;
import com.jme3.water.SimpleWaterProcessor;
import com.jme3.water.SimpleWaterProcessor.RefractionProcessor;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.MyControl;
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
     * separator between items in lists
     */
    private String listSeparator = ",";
    // *************************************************************************
    // new methods exposed

    /**
     * Generate a textual description of a collision shape.
     *
     * @param shape (not null, unaffected)
     * @return description (not null)
     */
    public String describe(CollisionShape shape) {
        Validate.nonNull(shape, "shape");

        String name = shape.getClass().getSimpleName();
        if (name.endsWith("CollisionShape")) {
            name = MyString.removeSuffix(name, "CollisionShape");
        }

        String result = name;
        if (shape instanceof CapsuleCollisionShape) {
            CapsuleCollisionShape capsule = (CapsuleCollisionShape) shape;
            int axis = capsule.getAxis();
            result += describeAxis(axis);
            float height = capsule.getHeight();
            float radius = capsule.getRadius();
            result += String.format("[h=%f,r=%f]", height, radius);

        } else if (shape instanceof ConeCollisionShape) {
            ConeCollisionShape cone = (ConeCollisionShape) shape;
            //int axis = cone.getAxis(); TODO
            //result += describeAxis(axis);
            float height = cone.getHeight();
            float radius = cone.getRadius();
            result += String.format("[h=%f,r=%f]", height, radius);

        } else if (shape instanceof CompoundCollisionShape) {
            CompoundCollisionShape compound = (CompoundCollisionShape) shape;
            String desc = describeChildShapes(compound);
            result += String.format("[%s]", desc);

        } else if (shape instanceof CylinderCollisionShape) {
            CylinderCollisionShape cylinder = (CylinderCollisionShape) shape;
            int axis = cylinder.getAxis();
            result += describeAxis(axis);
            Vector3f halfExtents = cylinder.getHalfExtents();
            result += String.format("[hx=%f,hy=%f,hz=%f]",
                    halfExtents.x, halfExtents.y, halfExtents.z);

        } else if (shape instanceof SphereCollisionShape) {
            SphereCollisionShape sphere = (SphereCollisionShape) shape;
            float radius = sphere.getRadius();
            result += String.format("[r=%f]", radius);
        }

        return result;
    }

    /**
     * Generate a textual description of a material.
     *
     * @param material material to be described (may be null, unaffected)
     * @return description (not null)
     */
    public String describe(Material material) {
        StringBuilder result = new StringBuilder(20);
        // TODO
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
        // TODO
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
     * Generate a textual description of a compound shape's children.
     *
     * @param compound shape being described (not null)
     * @return description (not null)
     */
    public String describeChildShapes(CompoundCollisionShape compound) {
        StringBuilder result = new StringBuilder(20);
        boolean addSeparators = false;
        List<ChildCollisionShape> children = compound.getChildren();
        int count = children.size();
        for (int i = 0; i < count; i++) {
            ChildCollisionShape child = children.get(i);
            if (addSeparators) {
                result.append("  ");
            } else {
                addSeparators = true;
            }
            String desc = describe(child.shape);
            result.append(desc);

            Vector3f location = child.location;
            desc = String.format("@[%.3f, %.3f, %.3f]",
                    location.x, location.y, location.z);
            result.append(desc);

            Quaternion rotation = new Quaternion();
            rotation.fromRotationMatrix(child.rotation);
            if (!MyQuaternion.isRotationIdentity(rotation)) {
                result.append("rot");
                desc = rotation.toString();
                result.append(desc);
            }
        }

        return result.toString();
    }

    /**
     * Generate a textual description of a spatial's controls.
     *
     * @param spatial spatial being described (not null)
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
     * Generate a textual description of a viewport's scene processors.
     *
     * @param viewPort view port being described (not null)
     * @return description (not null)
     */
    public String describeProcessors(ViewPort viewPort) {
        StringBuilder result = new StringBuilder(20);
        boolean addSeparators = false;
        List<SceneProcessor> pList = viewPort.getProcessors();
        int count = pList.size();
        for (int i = 0; i < count; i++) {
            SceneProcessor processor = pList.get(i);
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
     * Generate a textual description of a filter post-processor's filters.
     *
     * @param fpp processor being described (not null)
     * @return description (not null)
     */
    public String describeFilters(FilterPostProcessor fpp) {
        StringBuilder result = new StringBuilder(20);
        boolean addSeparators = false;

        Iterator<Filter> iterator = fpp.getFilterIterator();
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }

        iterator = fpp.getFilterIterator();
        for (int i = 0; i < count; i++) {
            Filter filter = iterator.next();
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
            if (light instanceof AmbientLight) {
                result = "AL";
            } else if (light instanceof DirectionalLight) {
                result = "DL";
            } else if (light instanceof PointLight) {
                result = "PL";
            } else if (light instanceof SpotLight) {
                result = "SL";
            } else {
                result = light.getClass().getSimpleName();
                if (result.isEmpty()) {
                    result = "?L";
                }
            }
            String name = light.getName();
            result += MyString.quote(name);
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
        int count = lightList.size();
        for (int i = 0; i < count; i++) {
            Light light = lightList.get(i);
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
     * Generate a textual description of a scene processor.
     *
     * @param processor processor to describe (unaffected)
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
        char result = MySpatial.describeType(spatial);
        return result;
    }

    /**
     * Test whether a scene-graph control is enabled.
     *
     * @param control control to test (not null)
     * @return true if the control is enabled, otherwise false
     */
    protected static boolean isControlEnabled(Control control) {
        Validate.nonNull(control, "control");

        boolean result = MyControl.isEnabled(control);
        return result;
    }
}
