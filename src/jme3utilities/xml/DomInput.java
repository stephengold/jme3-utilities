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
package jme3utilities.xml;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * DOM input utility methods. Aside from test cases, all methods here should be
 * public and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
final public class DomInput {
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
     * @param element element to use (not null)
     * @return new instance
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
     * @param element element to use (not null)
     * @param scaleFactor scale factor (in meters per world unit, &gt;0)
     * @return new instance
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