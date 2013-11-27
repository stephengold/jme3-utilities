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
import java.io.PrintStream;
import java.util.logging.Logger;

/**
 * XML input utility methods. Aside from test cases, all methods here should be
 * public and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class XmlOutput {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(XmlOutput.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private XmlOutput() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Serialize a quaternion to an XML stream.
     *
     * @param stream (not null)
     * @param tag XML tag (not null)
     * @param quaternion which quaternion to serialize (not null)
     * @param indent whitespace at the start of the line (not null)
     */
    public static void put(PrintStream stream, String tag,
            Quaternion quaternion, String indent) {
        assert tag != null;
        assert indent != null;

        stream.printf("%s<%s", indent, tag);
        putAttribute(stream, "x", quaternion.getX(), 6);
        putAttribute(stream, "y", quaternion.getY(), 6);
        putAttribute(stream, "z", quaternion.getZ(), 6);
        putAttribute(stream, "w", quaternion.getW(), 6);
        stream.println("/>");
    }

    /**
     * Serialize a vector to an XML stream.
     *
     * @param stream (not null)
     * @param tag XML tag (not null)
     * @param vector which vector to serialize (not null, unaffected)
     * @param indent whitespace at the start of the line (not null)
     * @param scaleFactor (in meters per world unit, >0)
     */
    public static void put(PrintStream stream, String tag, Vector3f vector,
            String indent, float scaleFactor) {
        assert tag != null;
        assert indent != null;
        assert scaleFactor > 0f : scaleFactor;

        stream.printf("%s<%s", indent, tag);
        putAttribute(stream, "x", vector.x * scaleFactor, 4);
        putAttribute(stream, "y", vector.y * scaleFactor, 4);
        putAttribute(stream, "z", vector.z * scaleFactor, 4);
        stream.println("/>");
    }

    /**
     * Write an attribute-value pair to an XML stream: a float.
     *
     * @param stream (not null)
     * @param name name of the attribute (not null)
     * @param floatValue value of the attribute
     * @param precision max digits after the decimal point (>=0)
     */
    public static void putAttribute(PrintStream stream, String name,
            float floatValue, int precision) {
        assert stream != null;
        assert name != null;
        assert precision >= 0 : precision;

        String formatString = String.format("%%.%df", precision);
        String stringValue = String.format(formatString, floatValue);
        stringValue = MyString.trimFloat(stringValue);
        putAttribute(stream, name, stringValue);
    }

    /**
     * Write an attribute-value pair to an XML stream: a string.
     *
     * @param stream (not null)
     * @param name name of the attribute (not null)
     * @param stringValue value of the attribute (not null)
     */
    public static void putAttribute(PrintStream stream, String name,
            String stringValue) {
        assert name != null;
        assert stringValue != null;

        String quotedValue = MyString.quote(stringValue);
        stream.printf(" %s=%s", name, quotedValue);
    }
}