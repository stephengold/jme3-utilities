/*
 Copyright (c) 2020, Stephen Gold
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
package jme3utilities;

import com.jme3.scene.VertexBuffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Logger;

/**
 * Utility methods that operate on individual elements of vertex buffers.
 * Outside of index buffers, each element represents a single mesh vertex.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Element {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(Element.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Element() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy all data from one vertex-buffer element to another. The vertex
     * buffers must have the same type, format, and number of components per
     * element.
     *
     * @param source the VertexBuffer containing the source element (not null,
     * unaffected)
     * @param sourceIndex the index of the source element (&ge;0)
     * @param target the VertexBuffer containing the target element (not null,
     * modified)
     * @param targetIndex the index of the target element (&ge;0)
     */
    public static void copy(VertexBuffer source, int sourceIndex,
            VertexBuffer target, int targetIndex) {
        int sourceCount = source.getNumElements();
        Validate.inRange(sourceIndex, "source index", 0, sourceCount - 1);
        int targetCount = target.getNumElements();
        Validate.inRange(targetIndex, "target index", 0, targetCount - 1);

        assert source.getBufferType() == target.getBufferType();
        assert source.getFormat() == target.getFormat();
        assert source.getNumComponents() == target.getNumComponents();

        int numComponents = target.getNumComponents();
        VertexBuffer.Format format = target.getFormat();
        if (format == VertexBuffer.Format.Half) {
            numComponents *= 2;
        }
        int sourceStart = numComponents * sourceIndex;
        int targetStart = numComponents * targetIndex;

        switch (format) {
            case Byte:
            case Half:
            case UnsignedByte:
                ByteBuffer sbBuf = (ByteBuffer) source.getData();
                ByteBuffer tbBuf = (ByteBuffer) target.getData();
                for (int cIndex = 0; cIndex < numComponents; ++cIndex) {
                    byte b = sbBuf.get(sourceStart + cIndex);
                    tbBuf.put(targetStart + cIndex, b);
                }
                break;

            case Double:
                DoubleBuffer sdBuf = (DoubleBuffer) source.getData();
                DoubleBuffer tdBuf = (DoubleBuffer) target.getData();
                for (int cIndex = 0; cIndex < numComponents; ++cIndex) {
                    double d = sdBuf.get(sourceStart + cIndex);
                    tdBuf.put(targetStart + cIndex, d);
                }
                break;

            case Float:
                FloatBuffer sfBuf = (FloatBuffer) source.getData();
                FloatBuffer tfBuf = (FloatBuffer) target.getData();
                for (int cIndex = 0; cIndex < numComponents; ++cIndex) {
                    float f = sfBuf.get(sourceStart + cIndex);
                    tfBuf.put(targetStart + cIndex, f);
                }
                break;

            case Int:
            case UnsignedInt:
                IntBuffer siBuf = (IntBuffer) source.getData();
                IntBuffer tiBuf = (IntBuffer) target.getData();
                for (int cIndex = 0; cIndex < numComponents; ++cIndex) {
                    int i = siBuf.get(sourceStart + cIndex);
                    tiBuf.put(targetStart + cIndex, i);
                }
                break;

            case Short:
            case UnsignedShort:
                ShortBuffer ssBuf = (ShortBuffer) source.getData();
                ShortBuffer tsBuf = (ShortBuffer) target.getData();
                for (int cIndex = 0; cIndex < numComponents; ++cIndex) {
                    short s = ssBuf.get(sourceStart + cIndex);
                    tsBuf.put(targetStart + cIndex, s);
                }
                break;

            default:
                String message = "Unrecognized buffer format: " + format;
                throw new UnsupportedOperationException(message);
        }
    }
}
