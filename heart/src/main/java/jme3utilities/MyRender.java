/*
 Copyright (c) 2019, Stephen Gold
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

import com.jme3.renderer.Caps;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GLExt;
import com.jme3.renderer.opengl.GLRenderer;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.logging.Logger;

/**
 * Utility methods that operate on jME3 renderers.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class MyRender {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MyRender.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyRender() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the default degree of anisotropic filtering. A partial workaround
     * for JME issue #1074.
     *
     * @param renderer which renderer (not null, unaffected)
     * @return the degree (&ge;1)
     */
    public static int defaultAnisotropicFilter(GLRenderer renderer) {
        Field field;
        try {
            field = GLRenderer.class.getDeclaredField(
                    "defaultAnisotropicFilter");
        } catch (NoSuchFieldException exception) {
            throw new RuntimeException(exception);
        }
        field.setAccessible(true);

        int result;
        try {
            result = (int) field.get(renderer);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }

        assert result >= 1 : result;
        return result;
    }

    /**
     * Access the baseline OpenGL interface of the specified renderer.
     *
     * @param renderer which renderer (not null, unaffected)
     * @return the pre-existing interface (not null)
     */
    public static GL getGL(GLRenderer renderer) {
        Field field;
        try {
            field = GLRenderer.class.getDeclaredField("gl");
        } catch (NoSuchFieldException exception) {
            throw new RuntimeException(exception);
        }
        field.setAccessible(true);

        GL result;
        try {
            result = (GL) field.get(renderer);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }

        assert result != null;
        return result;
    }

    /**
     * Test whether alpha-to-coverage is enabled for the specified renderer. A
     * partial workaround for JME issue #1074.
     *
     * @param renderer which renderer (not null, unaffected)
     * @return true if enabled, otherwise false
     */
    public static boolean isAlphaToCoverage(GLRenderer renderer) {
        boolean result = false;
        EnumSet caps = renderer.getCaps();
        if (caps.contains(Caps.Multisample)) {
            GL gl = getGL(renderer);
            result = gl.glIsEnabled(GLExt.GL_SAMPLE_ALPHA_TO_COVERAGE_ARB);
        }

        return result;
    }
}
