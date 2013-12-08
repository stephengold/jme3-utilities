/*
 Copyright (c) 2013, Stephen Gold
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

/*
 * vertex shader used by dome62.j3md
 */
attribute vec2 inTexCoord;
attribute vec3 inPosition;
uniform mat4 g_WorldViewProjectionMatrix;
uniform vec2 m_TopCoord;
varying vec2 skyTexCoord;

#ifdef HAS_OBJECT0
        uniform vec2 m_Object0Center;
        uniform vec2 m_Object0TransformU;
        uniform vec2 m_Object0TransformV;
        varying vec2 object0Coord;
        varying vec2 object0Offset;
#endif

#ifdef HAS_OBJECT1
        uniform vec2 m_Object1Center;
        uniform vec2 m_Object1TransformU;
        uniform vec2 m_Object1TransformV;
        varying vec2 object1Coord;
        varying vec2 object1Offset;
#endif

#ifdef HAS_OBJECT2
        uniform vec2 m_Object2Center;
        uniform vec2 m_Object2TransformU;
        uniform vec2 m_Object2TransformV;
        varying vec2 object2Coord;
        varying vec2 object2Offset;
#endif

#ifdef HAS_OBJECT3
        uniform vec2 m_Object3Center;
        uniform vec2 m_Object3TransformU;
        uniform vec2 m_Object3TransformV;
        varying vec2 object3Coord;
        varying vec2 object3Offset;
#endif

#ifdef HAS_OBJECT4
        uniform vec2 m_Object4Center;
        uniform vec2 m_Object4TransformU;
        uniform vec2 m_Object4TransformV;
        varying vec2 object4Coord;
        varying vec2 object4Offset;
#endif

#ifdef HAS_OBJECT5
        uniform vec2 m_Object5Center;
        uniform vec2 m_Object5TransformU;
        uniform vec2 m_Object5TransformV;
        varying vec2 object5Coord;
        varying vec2 object5Offset;
#endif

#ifdef HAS_CLOUDS0
        uniform float m_Clouds0Scale;
        uniform vec2 m_Clouds0Offset;
        varying vec2 clouds0Coord;
#endif

#ifdef HAS_CLOUDS1
        uniform float m_Clouds1Scale;
        uniform vec2 m_Clouds1Offset;
        varying vec2 clouds1Coord;
#endif

void main(){
        skyTexCoord = inTexCoord;
        /*
         * The following cloud texture coordinate calculations must be kept
         * consistent with those in SkyMaterial.getTransparency(int,Vector2f) .
         */
        #ifdef HAS_CLOUDS0
                clouds0Coord = inTexCoord * m_Clouds0Scale + m_Clouds0Offset;
        #endif
        #ifdef HAS_CLOUDS1
                clouds1Coord = inTexCoord * m_Clouds1Scale + m_Clouds1Offset;
        #endif

        #ifdef HAS_OBJECT0
                object0Offset = inTexCoord - m_Object0Center;
                object0Coord.x = dot(m_Object0TransformU, object0Offset);
                object0Coord.y = dot(m_Object0TransformV, object0Offset);
                object0Coord += m_TopCoord;
        #endif
        #ifdef HAS_OBJECT1
                object1Offset = inTexCoord - m_Object1Center;
                object1Coord.x = dot(m_Object1TransformU, object1Offset);
                object1Coord.y = dot(m_Object1TransformV, object1Offset);
                object1Coord += m_TopCoord;
        #endif
        #ifdef HAS_OBJECT2
                object2Offset = inTexCoord - m_Object2Center;
                object2Coord.x = dot(m_Object2TransformU, object2Offset);
                object2Coord.y = dot(m_Object2TransformV, object2Offset);
                object2Coord += m_TopCoord;
        #endif
        #ifdef HAS_OBJECT3
                object3Offset = inTexCoord - m_Object3Center;
                object3Coord.x = dot(m_Object3TransformU, object3Offset);
                object3Coord.y = dot(m_Object3TransformV, object3Offset);
                object3Coord += m_TopCoord;
        #endif
        #ifdef HAS_OBJECT4
                object4Offset = inTexCoord - m_Object4Center;
                object4Coord.x = dot(m_Object4TransformU, object4Offset);
                object4Coord.y = dot(m_Object4TransformV, object4Offset);
                object4Coord += m_TopCoord;
        #endif
        #ifdef HAS_OBJECT5
                object5Offset = inTexCoord - m_Object5Center;
                object5Coord.x = dot(m_Object5TransformU, object5Offset);
                object5Coord.y = dot(m_Object5TransformV, object5Offset);
                object5Coord += m_TopCoord;
        #endif

        gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1);
}