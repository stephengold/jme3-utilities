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
 * fragment shader used by dome66.j3md in its "Glow" technique
 */
uniform vec4 m_ClearGlow;
varying vec2 skyTexCoord;

#ifdef HAS_OBJECT0
	uniform sampler2D m_Object0ColorMap;
        uniform vec4 m_Object0Glow;
	varying vec2 object0Coord;
#endif

#ifdef HAS_OBJECT1
	uniform sampler2D m_Object1ColorMap;
        uniform vec4 m_Object1Glow;
	varying vec2 object1Coord;
#endif

#ifdef HAS_OBJECT2
	uniform sampler2D m_Object2ColorMap;
        uniform vec4 m_Object2Glow;
	varying vec2 object2Coord;
#endif

#ifdef HAS_OBJECT3
	uniform sampler2D m_Object3ColorMap;
        uniform vec4 m_Object3Glow;
	varying vec2 object3Coord;
#endif

#ifdef HAS_OBJECT4
	uniform sampler2D m_Object4ColorMap;
        uniform vec4 m_Object4Glow;
	varying vec2 object4Coord;
#endif

#ifdef HAS_OBJECT5
	uniform sampler2D m_Object5ColorMap;
        uniform vec4 m_Object5Glow;
	varying vec2 object5Coord;
#endif

#ifdef HAS_HAZE
        uniform sampler2D m_HazeAlphaMap;
        uniform vec4 m_HazeGlow;
#endif

#ifdef HAS_CLOUDS0
        uniform sampler2D m_Clouds0AlphaMap;
        uniform vec4 m_Clouds0Glow;
	varying vec2 clouds0Coord;
#endif

#ifdef HAS_CLOUDS1
        uniform sampler2D m_Clouds1AlphaMap;
        uniform vec4 m_Clouds1Glow;
	varying vec2 clouds1Coord;
#endif

#ifdef HAS_CLOUDS2
        uniform sampler2D m_Clouds2AlphaMap;
        uniform vec4 m_Clouds2Glow;
	varying vec2 clouds2Coord;
#endif

#ifdef HAS_CLOUDS3
        uniform sampler2D m_Clouds3AlphaMap;
        uniform vec4 m_Clouds3Glow;
	varying vec2 clouds3Coord;
#endif

#ifdef HAS_CLOUDS4
        uniform sampler2D m_Clouds4AlphaMap;
        uniform vec4 m_Clouds4Glow;
	varying vec2 clouds4Coord;
#endif

#ifdef HAS_CLOUDS5
        uniform sampler2D m_Clouds5AlphaMap;
        uniform vec4 m_Clouds5Glow;
	varying vec2 clouds5Coord;
#endif

vec4 mixColors(vec4 color0, vec4 color1) {
        vec4 result;
        float a0 = color0.a * (1.0 - color1.a);
        result.a = a0 + color1.a;
        if (result.a > 0.0) {
                result.rgb = (a0 * color0.rgb + color1.a * color1.rgb)/result.a;
        } else {
                result.rgb = vec3(0.0);
        }
        return result;
}

void main() {
        vec4 stars = vec4(0.0);

        vec4 objects = vec4(0.0);

        #ifdef HAS_OBJECT0
                if (all(floor(object0Coord) == vec2(0, 0))) {
                        objects = m_Object0Glow;
                        objects *= texture2D(m_Object0ColorMap, object0Coord);
                }
	#endif

        #ifdef HAS_OBJECT1
                if (all(floor(object1Coord) == vec2(0, 0))) {
                        vec4 object1 = m_Object1Glow;
                        object1 *= texture2D(m_Object1ColorMap, object1Coord);
                        objects = mixColors(objects, object1);
                }
	#endif

        #ifdef HAS_OBJECT2
                if (all(floor(object2Coord) == vec2(0, 0))) {
                        vec4 object2 = m_Object2Glow;
                        object2 *= texture2D(m_Object2ColorMap, object2Coord);
                        objects = mixColors(objects, object2);
                }
	#endif

        #ifdef HAS_OBJECT3
                if (all(floor(object3Coord) == vec2(0, 0))) {
                        vec4 object3 = m_Object3Glow;
                        object3 *= texture2D(m_Object3ColorMap, object3Coord);
                        objects = mixColors(objects, object3);
                }
	#endif

        #ifdef HAS_OBJECT4
                if (all(floor(object4Coord) == vec2(0, 0))) {
                        vec4 object4 = m_Object4Glow;
                        object4 *= texture2D(m_Object4ColorMap, object4Coord);
                        objects = mixColors(objects, object4);
                }
	#endif

        #ifdef HAS_OBJECT5
                if (all(floor(object5Coord) == vec2(0, 0))) {
                        vec4 object5 = m_Object5Glow;
                        object5 *= texture2D(m_Object5ColorMap, object5Coord);
                        objects = mixColors(objects, object5);
                }
	#endif

        vec4 color = mixColors(stars, objects);

        vec4 clear = m_ClearGlow;
	#ifdef HAS_HAZE
                vec4 haze = m_HazeGlow;
                haze.a *= texture2D(m_HazeAlphaMap, skyTexCoord).r;
	        clear = mixColors(clear, haze);
	#endif
        color = mixColors(color, clear);
        // Bright parts of objects shine through the clear areas.
        color.rgb += objects.rgb * objects.a * (1.0 - clear.rgb) * clear.a;

	#ifdef HAS_CLOUDS0
                vec4 clouds0 = m_Clouds0Glow;
		clouds0.a *= texture2D(m_Clouds0AlphaMap, clouds0Coord).r;
                color = mixColors(color, clouds0);
        #endif

	#ifdef HAS_CLOUDS1
                vec4 clouds1 = m_Clouds1Glow;
		clouds1.a *= texture2D(m_Clouds1AlphaMap, clouds1Coord).r;
                color = mixColors(color, clouds1);
        #endif

	#ifdef HAS_CLOUDS2
                vec4 clouds2 = m_Clouds2Glow;
		clouds2.a *= texture2D(m_Clouds2AlphaMap, clouds2Coord).r;
                color = mixColors(color, clouds2);
        #endif

	#ifdef HAS_CLOUDS3
                vec4 clouds3 = m_Clouds3Glow;
		clouds3.a *= texture2D(m_Clouds3AlphaMap, clouds3Coord).r;
                color = mixColors(color, clouds3);
        #endif

	#ifdef HAS_CLOUDS4
                vec4 clouds4 = m_Clouds4Glow;
		clouds4.a *= texture2D(m_Clouds4AlphaMap, clouds4Coord).r;
                color = mixColors(color, clouds4);
        #endif

	#ifdef HAS_CLOUDS5
                vec4 clouds5 = m_Clouds5Glow;
		clouds5.a *= texture2D(m_Clouds5AlphaMap, clouds5Coord).r;
                color = mixColors(color, clouds5);
        #endif

	gl_FragColor = color;
}