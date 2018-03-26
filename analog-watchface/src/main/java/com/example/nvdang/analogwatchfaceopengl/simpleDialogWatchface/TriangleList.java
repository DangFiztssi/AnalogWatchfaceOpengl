package com.example.nvdang.analogwatchfaceopengl.simpleDialogWatchface;

import android.opengl.GLES20;
import android.util.Log;

import com.example.nvdang.analogwatchfaceopengl.sample.Gles2ColoredTriangleList;
import com.example.nvdang.analogwatchfaceopengl.utils.Gles2Program;

import org.w3c.dom.ProcessingInstruction;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static com.example.nvdang.analogwatchfaceopengl.sample.Gles2ColoredTriangleList.checkGlError;
import static com.example.nvdang.analogwatchfaceopengl.utils.Gles2Program.CHECK_GL_ERRORS;

/**
 * Created by nvdang on 3/21/18.
 */


/*
*
* This class is used to draw same triangles by get Gles2Program, a list contains all coords of all vertex
*
* */
public class TriangleList {
    public static final int COORDS_PER_VERTEX = 3;
    public static final int BYTES_PER_FLOAT = 4;

    public static final int BYTES_PER_VERTEX = COORDS_PER_VERTEX*BYTES_PER_FLOAT;

    private FloatBuffer mVertexBuffer;

    private FloatBuffer mColorBuffer;
    private float[] mColor;

    private Gles2Program mProgram;

    private int mNumberCoords;

    public TriangleList(Gles2Program program, float[] trianglesCoords, float[] color) {

        //TODO: check list coords if validate in here, do it later
        this.mProgram = program;

        this.mVertexBuffer = ByteBuffer.allocateDirect(trianglesCoords.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.mVertexBuffer.put(trianglesCoords).position(0);

        this.mColorBuffer = ByteBuffer.allocateDirect(color.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.mColorBuffer.put(color).position(0);

        this.mColor = color;

        this.mNumberCoords = trianglesCoords.length/COORDS_PER_VERTEX;
    }

    public void draw(float[] mvpMatrix) {
        this.mProgram.bind(mvpMatrix, this.mVertexBuffer, this.mColorBuffer, this.mColor);
//        this.mProgram.bind(mvpMatrix, mVertexBuffer, mColor);

        // Draw the triangle list.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, this.mNumberCoords);

        if (CHECK_GL_ERRORS)
            checkGlError("glDrawArrays");
    }

}
