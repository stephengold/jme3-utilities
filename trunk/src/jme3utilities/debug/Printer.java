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
package jme3utilities.debug;

import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Logger;
import jme3utilities.MyControl;
import jme3utilities.MySpatial;
import jme3utilities.MyString;

/**
 * An object to dump portions of a jME3 scene graph for debugging.
 * <p>
 * printSubtree() is the usual interface to this class. The level of detail can
 * be configured dynamically.
 * <p>
 * The following forum post may be of interest:
 * http://hub.jmonkeyengine.org/forum/topic/a-simple-node-tree-printer-to-html/
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Printer {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Printer.class.getName());
    // *************************************************************************
    // fields
    /**
     * enable printing of render queue bucket assignments
     */
    private boolean printBucketFlag = false;
    /**
     * enable printing of cull hints
     */
    private boolean printCullFlag = false;
    /**
     * enable printing of shadow modes
     */
    private boolean printShadowFlag = false;
    /**
     * enable printing of location and scaling
     */
    private boolean printTransformFlag = false;
    /**
     * enable printing of user data
     */
    private boolean printUserFlag = true;
    /**
     * which stream to use for output: set by constructor
     */
    final private PrintStream stream;
    /**
     * separator between control names
     */
    private String controlNameSeparator = ",";
    /**
     * indentation for each level of recursion in the scene graph dump
     */
    private String indentIncrement = "  ";
    // *************************************************************************
    // constructors

    /**
     * Instantiate a printer which will use System.out for output.
     */
    public Printer() {
        stream = System.out;
    }

    /**
     * Instantiate a printer which will use the specified output stream.
     *
     * @param printStream (not null)
     */
    public Printer(PrintStream printStream) {
        Validate.nonNull(printStream, "print stream");
        stream = printStream;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Generate a textual description of a spatial's controls.
     *
     * @param spatial which spatial (not null)
     * @param enabled if true, describe only the enabled controls; if false,
     * describe only the disabled controls
     */
    public String describeControls(Spatial spatial, boolean enabled) {
        StringBuilder result = new StringBuilder();
        boolean addSeparators = false;
        int count = spatial.getNumControls();
        for (int i = 0; i < count; i++) {
            Object object = spatial.getControl(i);
            boolean isEnabled = isControlEnabled(object);
            if (isEnabled == enabled) {
                if (addSeparators) {
                    result.append(controlNameSeparator);
                } else {
                    addSeparators = true;
                }
                String description = describeControl(object);
                result.append(description);
            }
        }
        return result.toString();
    }

    /**
     * Dump the render queue bucket to which a spatial is assigned.
     *
     * @param spatial which spatial (not null)
     */
    public void printBucket(Spatial spatial) {
        /*
         * Print its local assignment.
         */
        Bucket bucket = spatial.getLocalQueueBucket();
        stream.printf(" bucket=%s", bucket.toString());
        if (bucket == Bucket.Inherit) {
            /*
             * Print its effective assignment.
             */
            bucket = spatial.getQueueBucket();
            stream.printf("/%s", bucket.toString());
        }
    }

    /**
     * List the controls associated with a spatial.
     *
     * @param spatial which spatial (not null)
     */
    public void printControls(Spatial spatial) {
        assert spatial != null;
        /*
         * List its enabled controls.
         */
        String description = describeControls(spatial, true);
        if (description.length() > 0) {
            stream.printf(" %s", description);
        }
        /*
         * List its disabled controls in parentheses.
         */
        description = describeControls(spatial, false);
        if (description.length() > 0) {
            stream.printf(" (%s)", description);
        }
    }

    /**
     * Dump the view frustum culling hints associated with a spatial.
     *
     * @param spatial which spatial (not null)
     */
    public void printCullHints(Spatial spatial) {
        /*
         * Print its local cull hint.
         */
        CullHint mode = spatial.getLocalCullHint();
        stream.printf(" cull=%s", mode.toString());
        if (mode == CullHint.Inherit) {
            /*
             * Print its effective cull hint.
             */
            mode = spatial.getCullHint();
            stream.printf("/%s", mode.toString());
        }
    }

    /**
     * List the lights associated with a spatial.
     *
     * @param spatial which spatial (not null)
     */
    public void printLights(Spatial spatial) {
        LightList lights = spatial.getLocalLightList();
        for (Light light : lights) {
            String name = light.getName();
            stream.printf(" L(%s)", name);
        }
    }

    /**
     * Print the world location of a spatial.
     *
     * @param spatial which spatial (not null)
     */
    public void printLocation(Spatial spatial) {
        Vector3f location = MySpatial.getWorldLocation(spatial);
        if (!location.equals(Vector3f.ZERO)) {
            stream.printf(" loc=[%.3f, %.3f, %.3f]",
                    location.x, location.y, location.z);
        }
    }

    /**
     * Print the world scale of a spatial.
     *
     * @param spatial which spatial (not null)
     */
    public void printScale(Spatial spatial) {
        Vector3f scale = spatial.getWorldScale();
        if (scale.x != scale.y || scale.y != scale.z) {
            stream.printf(" scale=%s", scale.toString());
        } else if (scale.x != 1f) {
            /*
             * uniform scaling
             */
            String valueString = Float.toString(scale.x);
            stream.printf(" scale=%s", valueString);
        }
    }

    /**
     * Print the shadow modes associated with a spatial.
     *
     * @param spatial which spatial (not null)
     */
    public void printShadowModes(Spatial spatial) {
        /*
         * Print its local shadow mode.
         */
        ShadowMode mode = spatial.getLocalShadowMode();
        stream.printf(" shad=%s", mode.toString());
        if (mode == ShadowMode.Inherit) {
            /*
             * Print its effective shadow mode.
             */
            mode = spatial.getShadowMode();
            stream.printf("/%s", mode.toString());
        }
    }

    /**
     * Dump a subtree of the scene graph.
     *
     * @param spatial root of the subtree (or null)
     */
    public void printSubtree(Spatial spatial) {
        printSubtree(spatial, "");
        stream.flush();
    }

    /**
     * Dump a subtree of the scene graph. Note: recursive!
     *
     * @param spatial root of the subtree (or null)
     * @param indent (not null)
     */
    public void printSubtree(Spatial spatial, String indent) {
        assert indent != null;

        if (spatial == null) {
            return;
        }
        stream.print(indent);

        int elementCount = spatial.getTriangleCount();
        stream.printf("%c[%d] ", describeType(spatial), elementCount);

        String name = spatial.getName();
        if (name == null) {
            stream.print("(no name)");
        } else {
            stream.print(MyString.quote(spatial.getName()));
        }
        /*
         * Print the spatial's controls and lights
         */
        printControls(spatial);
        printLights(spatial);
        if (printTransformFlag) {
            printLocation(spatial);
            printScale(spatial);
        }
        if (printUserFlag) {
            printUserData(spatial);
        }
        if (printBucketFlag) {
            printBucket(spatial);
        }
        if (printShadowFlag) {
            printShadowModes(spatial);
        }
        if (printCullFlag) {
            printCullHints(spatial);
        }
        stream.println();
        /*
         * If the spatial is a node (but not a terrain node),
         * print its children with incremented indentation.
         */
        if (spatial instanceof Node && !(spatial instanceof TerrainQuad)) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                printSubtree(child, indent + indentIncrement);
            }
        }
    }

    /**
     * Print the user data associated with a spatial.
     *
     * @param spatial which spatial (not null)
     */
    public void printUserData(Spatial spatial) {
        Collection<String> keys = spatial.getUserDataKeys();
        for (String key : keys) {
            Object value = spatial.getUserData(key);
            String valueString = MyString.escape(value.toString());
            if (value instanceof String) {
                valueString = MyString.quote(valueString);
            }
            stream.printf(" %s=%s", key, valueString);
        }
    }

    /**
     * Configure the control name separator.
     *
     * @param newValue (not null)
     * @return this instance for chaining
     */
    public Printer setControlNameSeparator(String newValue) {
        Validate.nonNull(newValue, "separator");
        controlNameSeparator = newValue;
        return this;
    }

    /**
     * Configure the indent increment.
     *
     * @param newValue (not null)
     * @return this instance for chaining
     */
    public Printer setIndentIncrement(String newValue) {
        Validate.nonNull(newValue, "increment");
        indentIncrement = newValue;
        return this;
    }

    /**
     * Configure printing of render queue bucket assignments.
     *
     * @param newValue true to enable, false to disable
     * @return this instance for chaining
     */
    public Printer setPrintBucket(boolean newValue) {
        printBucketFlag = newValue;
        return this;
    }

    /**
     * Configure printing of cull hints.
     *
     * @param newValue true to enable, false to disable
     * @return this instance for chaining
     */
    public Printer setPrintCull(boolean newValue) {
        printCullFlag = newValue;
        return this;
    }

    /**
     * Configure printing of shadow modes.
     *
     * @param newValue true to enable, false to disable
     * @return this instance for chaining
     */
    public Printer setPrintShadow(boolean newValue) {
        printShadowFlag = newValue;
        return this;
    }

    /**
     * Configure printing of location and scaling.
     *
     * @param newValue true to enable, false to disable
     * @return this instance for chaining
     */
    public Printer setPrintTransform(boolean newValue) {
        printTransformFlag = newValue;
        return this;
    }

    /**
     * Configure printing of user data.
     *
     * @param newValue true to enable, false to disable
     * @return this instance for chaining
     */
    public Printer setPrintUser(boolean newValue) {
        printUserFlag = newValue;
        return this;
    }
    // *************************************************************************
    // new protected methods

    /**
     * Generate a textual description of a scene-graph control.
     *
     * @param control (not null)
     */
    protected String describeControl(Object control) {
        Validate.nonNull(control, "control");

        String result = MyControl.describe(control);
        return result;
    }

    /**
     * Generate a one-letter description of a spatial.
     *
     * @param spatial which spatial
     */
    public static char describeType(Spatial spatial) {
        char result = MySpatial.describeType(spatial);
        return result;
    }

    /**
     * Test whether a scene-graph control is enabled.
     *
     * @param control which control to test (not null)
     * @return true if the control is enabled, otherwise false
     */
    protected boolean isControlEnabled(Object control) {
        boolean result = MyControl.isEnabled(control);

        return result;
    }
}