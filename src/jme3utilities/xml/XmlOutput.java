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
import java.io.PrintStream;
import java.util.logging.Logger;
import jme3utilities.MyString;

/**
 * XML output utility methods. Aside from test cases, all methods here should be
 * public and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
final public class XmlOutput {
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
     * @param scaleFactor (in meters per world unit, &gt;0)
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
     * @param precision max digits after the decimal point (&ge;0)
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