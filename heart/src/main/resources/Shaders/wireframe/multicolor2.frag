/*
 Copyright (c) 2017, Stephen Gold
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
 * fragment shader used by multicolor2.j3md
 */

#import "Common/ShaderLib/GLSLCompat.glsllib"

#ifdef DISCARD_ALPHA
        uniform float m_AlphaDiscardThreshold;
#endif
#ifdef MATERIAL_COLOR
        uniform vec4 m_Color;
#endif
#ifdef POINT_SHAPE
        uniform sampler2D m_PointShape;
#endif
#ifdef VERTEX_COLOR
        varying vec4 vertexColor;
#endif

void main(){
        #ifdef VERTEX_COLOR
                vec4 color = vertexColor;
        #else
                vec4 color = vec4(1.0);
        #endif

        #if defined(DISCARD_ALPHA)
                if(color.a < m_AlphaDiscardThreshold){
                        discard;
                }
        #endif

        #ifdef MATERIAL_COLOR
                color = m_Color;
        #endif

        #ifdef POINT_SHAPE
                vec4 sample = texture2D(m_PointShape, gl_PointCoord);
                color *= sample;
        #endif

        gl_FragColor = color;
}