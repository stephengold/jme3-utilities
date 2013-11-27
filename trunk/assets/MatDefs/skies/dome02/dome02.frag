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

/*
 * fragment shader used by dome02.j3md
 */
uniform vec4 m_ClearColor;
varying vec2 skyTexCoord;

#ifdef HAS_STARS
        uniform sampler2D m_StarsColorMap;
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

#ifdef HAS_HAZE
        uniform sampler2D m_HazeAlphaMap;
        uniform vec4 m_HazeColor;
#endif

void main(){
        #ifdef HAS_STARS
                vec4 stars = texture2D(m_StarsColorMap, skyTexCoord);
        #else
                vec4 stars = 0;
        #endif

        vec4 color = mix(stars, m_ClearColor, m_ClearColor.a);

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

	#ifdef HAS_HAZE
                float density = texture2D(m_HazeAlphaMap, skyTexCoord).r;
                density *= m_HazeColor.a;
	        color = mix(color, m_HazeColor, density);
	#endif

	gl_FragColor = color;
}