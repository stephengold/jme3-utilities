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
 * fragment shader used by dome66.j3md
 */
uniform vec4 m_ClearColor;
varying vec2 skyTexCoord;

#ifdef HAS_STARS
        uniform sampler2D m_StarsColorMap;
#endif

#ifdef HAS_OBJECT0
        uniform vec4 m_Object0Color;
	uniform sampler2D m_Object0ColorMap;
	varying vec2 object0Coord;
#endif

#ifdef HAS_OBJECT1
        uniform vec4 m_Object1Color;
	uniform sampler2D m_Object1ColorMap;
	varying vec2 object1Coord;
#endif

#ifdef HAS_OBJECT2
        uniform vec4 m_Object2Color;
	uniform sampler2D m_Object2ColorMap;
	varying vec2 object2Coord;
#endif

#ifdef HAS_OBJECT3
        uniform vec4 m_Object3Color;
	uniform sampler2D m_Object3ColorMap;
	varying vec2 object3Coord;
#endif

#ifdef HAS_OBJECT4
        uniform vec4 m_Object4Color;
	uniform sampler2D m_Object4ColorMap;
	varying vec2 object4Coord;
#endif

#ifdef HAS_OBJECT5
        uniform vec4 m_Object5Color;
	uniform sampler2D m_Object5ColorMap;
	varying vec2 object5Coord;
#endif

#ifdef HAS_CLOUDS0
        uniform sampler2D m_Clouds0AlphaMap;
        uniform vec4 m_Clouds0Color;
	varying vec2 clouds0Coord;
#endif

#ifdef HAS_CLOUDS1
        uniform sampler2D m_Clouds1AlphaMap;
        uniform vec4 m_Clouds1Color;
	varying vec2 clouds1Coord;
#endif

#ifdef HAS_CLOUDS2
        uniform sampler2D m_Clouds2AlphaMap;
        uniform vec4 m_Clouds2Color;
	varying vec2 clouds2Coord;
#endif

#ifdef HAS_CLOUDS3
        uniform sampler2D m_Clouds3AlphaMap;
        uniform vec4 m_Clouds3Color;
	varying vec2 clouds3Coord;
#endif

#ifdef HAS_CLOUDS4
        uniform sampler2D m_Clouds4AlphaMap;
        uniform vec4 m_Clouds4Color;
	varying vec2 clouds4Coord;
#endif

#ifdef HAS_CLOUDS5
        uniform sampler2D m_Clouds5AlphaMap;
        uniform vec4 m_Clouds5Color;
	varying vec2 clouds5Coord;
#endif

#ifdef HAS_HAZE
        uniform sampler2D m_HazeAlphaMap;
        uniform vec4 m_HazeColor;
#endif

void main(){
        #ifdef HAS_STARS
                vec4 stars = texture2D(m_StarsColorMap, skyTexCoord);
        #else
                vec4 stars = vec4(0.0);
        #endif

        #ifdef HAS_OBJECT0
                vec4 objects = texture2D(m_Object0ColorMap, object0Coord);
                objects *= m_Object0Color;
        #else
                vec4 objects = vec4(0.0);
	#endif

        #ifdef HAS_OBJECT1
                vec4 object1 = texture2D(m_Object1ColorMap, object1Coord);
                object1 *= m_Object1Color;
                objects = mix(objects, object1, object1.a);
	#endif

        #ifdef HAS_OBJECT2
                vec4 object2 = texture2D(m_Object2ColorMap, object2Coord);
                object2 *= m_Object2Color;
                objects = mix(objects, object2, object2.a);
	#endif

        #ifdef HAS_OBJECT3
                vec4 object3 = texture2D(m_Object3ColorMap, object3Coord);
                object3 *= m_Object3Color;
                objects = mix(objects, object3, object3.a);
	#endif

        #ifdef HAS_OBJECT4
                vec4 object4 = texture2D(m_Object4ColorMap, object4Coord);
                object4 *= m_Object4Color;
                objects = mix(objects, object4, object4.a);
	#endif

        #ifdef HAS_OBJECT5
                vec4 object5 = texture2D(m_Object5ColorMap, object5Coord);
                object5 *= m_Object5Color;
                objects = mix(objects, object5, object5.a);
	#endif

        vec4 color = mix(stars, objects, objects.a);
        color = mix(color, m_ClearColor, m_ClearColor.a);

        // Bright parts of objects shine through the clear color.
        color += objects * objects.a * (1.0 - m_ClearColor) * m_ClearColor.a;

	#ifdef HAS_CLOUDS0
		float density0 = texture2D(m_Clouds0AlphaMap, clouds0Coord).r;
		density0 *= m_Clouds0Color.a;
		color = mix(color, m_Clouds0Color, density0);
        #endif

	#ifdef HAS_CLOUDS1
		float density1 = texture2D(m_Clouds1AlphaMap, clouds1Coord).r;
		density1 *= m_Clouds1Color.a;
		color = mix(color, m_Clouds1Color, density1);
        #endif

	#ifdef HAS_CLOUDS2
		float density2 = texture2D(m_Clouds1AlphaMap, clouds2Coord).r;
		density2 *= m_Clouds2Color.a;
		color = mix(color, m_Clouds2Color, density2);
        #endif

	#ifdef HAS_CLOUDS3
		float density3 = texture2D(m_Clouds3AlphaMap, clouds3Coord).r;
		density3 *= m_Clouds3Color.a;
		color = mix(color, m_Clouds3Color, density3);
        #endif

	#ifdef HAS_CLOUDS4
		float density4 = texture2D(m_Clouds4AlphaMap, clouds4Coord).r;
		density4 *= m_Clouds4Color.a;
		color = mix(color, m_Clouds4Color, density4);
        #endif

	#ifdef HAS_CLOUDS5
		float density5 = texture2D(m_Clouds5AlphaMap, clouds5Coord).r;
		density5 *= m_Clouds5Color.a;
		color = mix(color, m_Clouds5Color, density5);
        #endif

	#ifdef HAS_HAZE
                float density = texture2D(m_HazeAlphaMap, skyTexCoord).r;
                density *= m_HazeColor.a;
	        color = mix(color, m_HazeColor, density);
	#endif

	gl_FragColor = color;
}