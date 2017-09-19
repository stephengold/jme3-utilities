/*
 Copyright (c) 2014, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the copyright holder nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

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

/*
 * vertex shader used by dome06.j3md
 */
attribute vec2 inTexCoord;
attribute vec3 inPosition;
uniform mat4 g_WorldViewProjectionMatrix;
uniform vec2 m_TopCoord;
varying vec2 skyTexCoord;

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

#ifdef HAS_CLOUDS2
        uniform float m_Clouds2Scale;
        uniform vec2 m_Clouds2Offset;
        varying vec2 clouds2Coord;
#endif

#ifdef HAS_CLOUDS3
        uniform float m_Clouds3Scale;
        uniform vec2 m_Clouds3Offset;
        varying vec2 clouds3Coord;
#endif

#ifdef HAS_CLOUDS4
        uniform float m_Clouds4Scale;
        uniform vec2 m_Clouds4Offset;
        varying vec2 clouds4Coord;
#endif

#ifdef HAS_CLOUDS5
        uniform float m_Clouds5Scale;
        uniform vec2 m_Clouds5Offset;
        varying vec2 clouds5Coord;
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
        #ifdef HAS_CLOUDS2
                clouds2Coord = inTexCoord * m_Clouds2Scale + m_Clouds2Offset;
        #endif
        #ifdef HAS_CLOUDS3
                clouds3Coord = inTexCoord * m_Clouds3Scale + m_Clouds3Offset;
        #endif
        #ifdef HAS_CLOUDS4
                clouds4Coord = inTexCoord * m_Clouds4Scale + m_Clouds4Offset;
        #endif
        #ifdef HAS_CLOUDS5
                clouds5Coord = inTexCoord * m_Clouds5Scale + m_Clouds5Offset;
        #endif

        gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1);
}