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

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * DOM input utility methods. Aside from test cases, all methods here should be
 * public and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class DomInput {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(DomInput.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private DomInput() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Create a quaternion from a DOM element.
     *
     * @param element which element (not null)
     * @return a new instance
     */
    public static Quaternion createQuaternion(Element element) {
        NamedNodeMap map = element.getAttributes();
        float x = getFloat(map, "x", 0f);
        float y = getFloat(map, "y", 0f);
        float z = getFloat(map, "z", 0f);
        float w = getFloat(map, "w", 0f);
        Quaternion quaternion = new Quaternion(x, y, z, w);
        return quaternion;
    }

    /**
     * Create a vector from a DOM element.
     *
     * @param element which element (not null)
     * @param scaleFactor scale factor (in meters per world unit, >0)
     * @return a new instance
     */
    public static Vector3f createVector3f(Element element, float scaleFactor) {
        assert scaleFactor > 0f : scaleFactor;

        NamedNodeMap map = element.getAttributes();
        float x = getFloat(map, "x", 0f);
        float y = getFloat(map, "y", 0f);
        float z = getFloat(map, "z", 0f);
        Vector3f vector = new Vector3f(x, y, z);
        vector.divideLocal(scaleFactor);
        return vector;
    }

    /**
     * Obtain a float value from a named DOM attribute.
     *
     * @param map map of named DOM attributes (not null)
     * @param name name of the desired attribute (not null)
     * @param defaultValue
     */
    public static float getFloat(NamedNodeMap map, String name,
            float defaultValue) {
        assert name != null;

        Attr attr = (Attr) map.getNamedItem(name);
        if (attr == null) {
            logger.log(Level.SEVERE,
                    "Attribute {0} was missing from XML document, set to {1}.",
                    new Object[]{
                        MyString.quote(name), defaultValue
                    });
            return defaultValue;
        }
        String text = attr.getValue();
        float result = Float.parseFloat(text);
        return result;
    }
}