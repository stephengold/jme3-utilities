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
package jme3utilities;

import com.jme3.audio.AudioNode;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.debug.SkeletonDebugger;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Dump portions of a JME3 scene graph for debugging.
 *
 * printSubtree() is the usual interface to this class.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Printer {
    // *************************************************************************
    // constants

    /**
     * enable printing of render queue bucket assignments
     */
    final private static boolean printBucketFlag = false;
    /**
     * enable printing of cull hints
     */
    final private static boolean printCullFlag = false;
    /**
     * enable printing of shadow modes
     */
    final private static boolean printShadowFlag = false;
    /**
     * enable printing of position and scaling
     */
    final private static boolean printTransformFlag = false;
    /**
     * enable printing of user data
     */
    final private static boolean printUserFlag = true;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Printer.class.getName());
    /**
     * indentation for each level of recursion in the scene graph dump
     */
    final private static String indentIncrement = "  ";
    /**
     * separator between control names
     */
    final private static String separator = ",";
    // *************************************************************************
    // fields
    /**
     * which stream to use for output
     */
    final private PrintStream stream;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a printer which will use System.out for output
     */
    public Printer() {
        stream = System.out;
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
                    result.append(separator);
                } else {
                    addSeparators = true;
                }
                String description = MyControl.describe(object);
                result.append(description);
            }
        }
        return result.toString();
    }

    /**
     * Generate a one-letter description of a spatial.
     *
     * @param spatial which spatial
     */
    public static char describeType(Spatial spatial) {
        if (spatial instanceof AudioNode) {
            return 'a';
        } else if (spatial instanceof Geometry) {
            return 'g';
        } else if (spatial instanceof SkeletonDebugger) {
            return 's';
        } else if (spatial instanceof TerrainQuad) {
            return 't';
        } else if (spatial instanceof Node) {
            return 'n';
        }
        return '?';
    }

    /**
     * Test whether a scene-graph control is enabled.
     *
     * @param control which control to test (not null)
     * @return true if the control is enabled, otherwise false
     */
    public boolean isControlEnabled(Object control) {
        return MyControl.isEnabled(control);
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
     * Dump the controls associated with a spatial.
     *
     * @param spatial which spatial (not null)
     */
    public void printControls(Spatial spatial) {
        assert spatial != null;
        /*
         * Print its enabled controls.
         */
        String description = describeControls(spatial, true);
        if (description.length() > 0) {
            stream.printf(" %s", description);
        }
        /*
         * Print its disabled controls in parentheses.
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
     * Print the lights associated with a spatial.
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
        stream.printf("%s%c ", indent, describeType(spatial));
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
            /*
             * Print the spatial's location, scaling, and user data..
             */
            Vector3f location = MySpatial.getWorldLocation(spatial);
            if (!location.equals(Vector3f.ZERO)) {
                stream.printf(" loc=%s", location.toString());
            }
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
         * print its children with increase indentation.
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
}