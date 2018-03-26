precision mediump float; // Set the default precision to medium. We don't need as high of a
                         //                            // precision in the fragment shader.
//varying vec4 v_Color; // This is the color from the vertex shader interpolated across the
                      //                            // triangle per fragment.

uniform vec4 u_Color;

void main() // The entry point for our fragment shader.
{
    gl_FragColor = u_Color; // Pass the color directly through the pipeline.
}