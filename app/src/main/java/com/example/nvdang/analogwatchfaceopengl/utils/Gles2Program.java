package com.example.nvdang.analogwatchfaceopengl.utils;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.example.nvdang.analogwatchfaceopengl.R;
import com.example.nvdang.analogwatchfaceopengl.simpleDialogWatchface.TriangleList;

import java.nio.FloatBuffer;

import static com.example.nvdang.analogwatchfaceopengl.sample.Gles2ColoredTriangleList.checkGlError;

/**
 * Created by nvdang on 3/21/18.
 */

public class Gles2Program {
    public static final String TAG = "Gles2Program";

    public static final boolean CHECK_GL_ERRORS = false;

    private Context mContext;

    // use this Id to identify this program
    private int mProgramHandleId;

    private final int mMVPMatrixHandle;
    private final int mPositionHandle;
    private final int mColorHandle;

    // Constructor
    public Gles2Program(Context context) {
        this.mContext = context;

        String vertexShaderCode = this.getVertexShaderCode();
        String fragmentShaderCode = this.getFragmentShaderCode();

        int vertexShader = this.compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = this.compileShader(GLES20.GL_FRAGMENT_SHADER,fragmentShaderCode);

        this.mProgramHandleId = this.createAndLinkProgram(vertexShader, fragmentShader, new String[]{"a_Position", "a_Color"});

        this.mMVPMatrixHandle = GLES20.glGetUniformLocation(this.mProgramHandleId, "u_MVPMatrix");
        this.mPositionHandle = GLES20.glGetAttribLocation(this.mProgramHandleId, "a_Position");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        this.mColorHandle = GLES20.glGetAttribLocation(this.mProgramHandleId, "a_Color");
    }

    // Tell OpenGL use this program
    public void use() {
        GLES20.glUseProgram(this.mProgramHandleId);
    }

    public void bind(float[] mvpMatrix, FloatBuffer vertexData, float[] color) {
        // Pass the VBO with the triangle list's vertices to OpenGL.
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        if (CHECK_GL_ERRORS) checkGlError("glEnableVertexAttribArray");

        GLES20.glVertexAttribPointer(mPositionHandle, TriangleList.COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false , TriangleList.BYTES_PER_VERTEX, vertexData);


        // Pass the triangle list's color to OpenGL.
        GLES20.glEnableVertexAttribArray(mColorHandle);
//        if (CHECK_GL_ERRORS) checkGlError("glVertexAttribPointer");
        GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 28, vertexData);
        GLES20.glUniform4fv(mColorHandle, 1 /* count */, color, 0 /* offset */);

//         Pass MVP matrix into OpenGL
        GLES20.glUniformMatrix4fv(this.mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        if (CHECK_GL_ERRORS) checkGlError("glUniformMatrix4fv");

    }

    private String getVertexShaderCode() {
        return RawResourceReader.readTextFileFromRawResource(this.mContext, R.raw.per_pixel_vertex_shader);
    }

    private String getFragmentShaderCode() {
        return RawResourceReader.readTextFileFromRawResource(this.mContext, R.raw.per_pixel_fragment_shader);
    }

    private int compileShader(final int shaderType, final String shaderSource) {
        int shaderHandle = GLES20.glCreateShader(shaderType);
        if (CHECK_GL_ERRORS) checkGlError("glCreateShader");

        if (shaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(shaderHandle, shaderSource);
            if (CHECK_GL_ERRORS) checkGlError("glShaderSource");

            // Compile the shader.
            GLES20.glCompileShader(shaderHandle);
            if (CHECK_GL_ERRORS) checkGlError("glCompileShader");

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }

        if (shaderHandle == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shaderHandle;
    }

    private int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) {
        int programHandle = GLES20.glCreateProgram();
        if (CHECK_GL_ERRORS) checkGlError("glCreateProgram");

        if (programHandle != 0) {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);
            if (CHECK_GL_ERRORS) checkGlError("glAttachShader");

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);
            if (CHECK_GL_ERRORS) checkGlError("glAttachShader");

            // Bind attributes
            if (attributes != null) {
                final int size = attributes.length;
                for (int i = 0; i < size; i++) {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
                }
            }

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle);
            if (CHECK_GL_ERRORS) checkGlError("glLinkProgram");

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (CHECK_GL_ERRORS) checkGlError("glGetUniformLocation");

            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }

        return programHandle;
    }

}
