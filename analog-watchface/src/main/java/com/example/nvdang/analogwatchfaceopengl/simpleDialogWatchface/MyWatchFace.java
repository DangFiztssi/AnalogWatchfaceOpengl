package com.example.nvdang.analogwatchfaceopengl.simpleDialogWatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.support.wearable.watchface.Gles2WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;

import com.example.nvdang.analogwatchfaceopengl.R;
import com.example.nvdang.analogwatchfaceopengl.sample.Gles2ColoredTriangleList;
import com.example.nvdang.analogwatchfaceopengl.utils.Gles2Program;
import com.example.nvdang.analogwatchfaceopengl.utils.RawResourceReader;
import com.example.nvdang.analogwatchfaceopengl.utils.ShaderHelper;
import com.example.nvdang.analogwatchfaceopengl.utils.TextureHelper;

import org.w3c.dom.ProcessingInstruction;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Calendar;
import java.util.TimeZone;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class MyWatchFace extends Gles2WatchFaceService {

    private static final String TAG = "MyOpenGLWatchFace";

    private static final long FRAME_PER_SECOND = 60;

    //        @Override
    //        public EngineBackground onCreateEngine() {
    //            return new EngineBackground();
    //        }


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class EngineBackground extends Gles2WatchFaceService.Engine {
        /** Used for debug logs. */
        private static final String TAG = "LessonFourRenderer";


        /**
         * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
         * of being located at the center of the universe) to world space.
         */
        private float[] mModelMatrix = new float[16];

        /**
         * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
         * it positions things relative to our eye.
         */
        private float[] mViewMatrix = new float[16];

        /** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
        private float[] mProjectionMatrix = new float[16];

        /** Allocate storage for the final combined matrix. This will be passed into the shader program. */
        private float[] mMVPMatrix = new float[16];

        /**
         * Stores a copy of the model matrix specifically for the light position.
         */
        //	private float[] mLightModelMatrix = new float[16];

        /** Store our model data in a float buffer. */
        private  FloatBuffer mCubePositions;
        private  FloatBuffer mCubeColors;
        //	private final FloatBuffer mCubeNormals;
        private FloatBuffer mCubeTextureCoordinates;

        /** This will be used to pass in the transformation matrix. */
        private int mMVPMatrixHandle;

        /** This will be used to pass in the modelview matrix. */
        private int mMVMatrixHandle;

        /** This will be used to pass in the light position. */
        //	private int mLightPosHandle;

        /** This will be used to pass in the texture. */
        private int mTextureUniformHandle;

        /** This will be used to pass in model position information. */
        private int mPositionHandle;

        /** This will be used to pass in model color information. */
        private int mColorHandle;

        /** This will be used to pass in model normal information. */
        private int mNormalHandle;

        /** This will be used to pass in model texture coordinate information. */
        private int mTextureCoordinateHandle;

        /** How many bytes per float. */
        private final int mBytesPerFloat = 4;

        /** Size of the position data in elements. */
        private final int mPositionDataSize = 3;

        /** Size of the color data in elements. */
        private final int mColorDataSize = 4;

        /** Size of the normal data in elements. */
        //	private final int mNormalDataSize = 3;

        /** Size of the texture coordinate data in elements. */
        private final int mTextureCoordinateDataSize = 2;

        /** Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
         *  we multiply this by our transformation matrices. */
        //	private final float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};

        /** Used to hold the current position of the light in world space (after transformation via model matrix). */
        //	private final float[] mLightPosInWorldSpace = new float[4];

        /** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix) */
        //	private final float[] mLightPosInEyeSpace = new float[4];

        /** This is a handle to our cube shading program. */
        private int mProgramHandle;

        /** This is a handle to our light point program. */
        //	private int mPointProgramHandle;

        /** This is a handle to our texture data. */
        private int mTextureDataHandle;

        @Override
        public void onGlContextCreated() {
            super.onGlContextCreated();

            // Define points for a cube.

            // X, Y, Z
            final float[] cubePositionData =
                    {
                            // Front face
                            -1.0f, 1.0f, 1.0f,
                            -1.0f, -1.0f, 1.0f,
                            1.0f, 1.0f, 1.0f,
                            -1.0f, -1.0f, 1.0f,
                            1.0f, -1.0f, 1.0f,
                            1.0f, 1.0f, 1.0f,

                            // Right face
                            1.0f, 1.0f, 1.0f,
                            1.0f, -1.0f, 1.0f,
                            1.0f, 1.0f, -1.0f,
                            1.0f, -1.0f, 1.0f,
                            1.0f, -1.0f, -1.0f,
                            1.0f, 1.0f, -1.0f,

                            // Back face
                            1.0f, 1.0f, -1.0f,
                            1.0f, -1.0f, -1.0f,
                            -1.0f, 1.0f, -1.0f,
                            1.0f, -1.0f, -1.0f,
                            -1.0f, -1.0f, -1.0f,
                            -1.0f, 1.0f, -1.0f,

                            // Left face
                            -1.0f, 1.0f, -1.0f,
                            -1.0f, -1.0f, -1.0f,
                            -1.0f, 1.0f, 1.0f,
                            -1.0f, -1.0f, -1.0f,
                            -1.0f, -1.0f, 1.0f,
                            -1.0f, 1.0f, 1.0f,

                            // Top face
                            -1.0f, 1.0f, -1.0f,
                            -1.0f, 1.0f, 1.0f,
                            1.0f, 1.0f, -1.0f,
                            -1.0f, 1.0f, 1.0f,
                            1.0f, 1.0f, 1.0f,
                            1.0f, 1.0f, -1.0f,

                            // Bottom face
                            1.0f, -1.0f, -1.0f,
                            1.0f, -1.0f, 1.0f,
                            -1.0f, -1.0f, -1.0f,
                            1.0f, -1.0f, 1.0f,
                            -1.0f, -1.0f, 1.0f,
                            -1.0f, -1.0f, -1.0f,
                    };

            // R, G, B, A
            final float[] cubeColorData =
                    {
                            // Front face (red)
                            0.0f, 1.0f, 0.0f, 1.0f,
                            1.0f, 0.0f, 0.0f, 1.0f,
                            1.0f, 0.0f, 0.0f, 1.0f,
                            1.0f, 0.0f, 0.0f, 1.0f,
                            1.0f, 0.0f, 0.0f, 1.0f,
                            1.0f, 0.0f, 0.0f, 1.0f,

                            // Right face (green)
                            0.0f, 1.0f, 0.0f, 1.0f,
                            0.0f, 1.0f, 0.0f, 1.0f,
                            0.0f, 1.0f, 0.0f, 1.0f,
                            0.0f, 1.0f, 0.0f, 1.0f,
                            0.0f, 1.0f, 0.0f, 1.0f,
                            0.0f, 1.0f, 0.0f, 1.0f,

                            // Back face (blue)
                            0.0f, 0.0f, 1.0f, 1.0f,
                            0.0f, 0.0f, 1.0f, 1.0f,
                            0.0f, 0.0f, 1.0f, 1.0f,
                            0.0f, 0.0f, 1.0f, 1.0f,
                            0.0f, 0.0f, 1.0f, 1.0f,
                            0.0f, 0.0f, 1.0f, 1.0f,

                            // Left face (yellow)
                            1.0f, 1.0f, 0.0f, 1.0f,
                            1.0f, 1.0f, 0.0f, 1.0f,
                            1.0f, 1.0f, 0.0f, 1.0f,
                            1.0f, 1.0f, 0.0f, 1.0f,
                            1.0f, 1.0f, 0.0f, 1.0f,
                            1.0f, 1.0f, 0.0f, 1.0f,

                            // Top face (cyan)
                            0.0f, 1.0f, 1.0f, 1.0f,
                            0.0f, 1.0f, 1.0f, 1.0f,
                            0.0f, 1.0f, 1.0f, 1.0f,
                            0.0f, 1.0f, 1.0f, 1.0f,
                            0.0f, 1.0f, 1.0f, 1.0f,
                            0.0f, 1.0f, 1.0f, 1.0f,

                            // Bottom face (magenta)
                            1.0f, 0.0f, 1.0f, 1.0f,
                            1.0f, 0.0f, 1.0f, 1.0f,
                            1.0f, 0.0f, 1.0f, 1.0f,
                            1.0f, 0.0f, 1.0f, 1.0f,
                            1.0f, 0.0f, 1.0f, 1.0f,
                            1.0f, 0.0f, 1.0f, 1.0f
                    };

            // S, T (or X, Y)
            // Texture coordinate data.
            // Because images have a Y axis pointing downward (values increase as you move down the image) while
            // OpenGL has a Y axis pointing upward, we adjust for that here by flipping the Y axis.
            // What's more is that the texture coordinates are the same for every face.
            final float[] cubeTextureCoordinateData =
                    {
                            // Front face
                            0.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 1.0f,
                            1.0f, 0.0f,

                            //				// Right face
                            0.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 1.0f,
                            1.0f, 0.0f,

                            // Back face
                            0.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 1.0f,
                            1.0f, 0.0f,

                            // Left face
                            0.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 1.0f,
                            1.0f, 0.0f,

                            // Top face
                            0.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 1.0f,
                            1.0f, 0.0f,

                            // Bottom face
                            0.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 1.0f,
                            1.0f, 0.0f
                    };

            // Initialize the buffers.
            mCubePositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytesPerFloat)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mCubePositions.put(cubePositionData).position(0);

            mCubeColors = ByteBuffer.allocateDirect(cubeColorData.length * mBytesPerFloat)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mCubeColors.put(cubeColorData).position(0);

            mCubeTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinateData.length * mBytesPerFloat)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mCubeTextureCoordinates.put(cubeTextureCoordinateData).position(0);
        }

        protected String getVertexShader()
        {
            return RawResourceReader.readTextFileFromRawResource(getApplicationContext(), R.raw.texture_vertex_shader);
        }

        protected String getFragmentShader()
        {
            return RawResourceReader.readTextFileFromRawResource(getApplicationContext(), R.raw.texture_fragment_shader);
        }

        @Override
        public void onGlSurfaceCreated(int width, int height) {
            super.onGlSurfaceCreated(width, height);

            // Set the background clear color to black.
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

            // Use culling to remove back faces.
            GLES20.glEnable(GLES20.GL_CULL_FACE);

            // Enable depth testing
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);

            // The below glEnable() call is a holdover from OpenGL ES 1, and is not needed in OpenGL ES 2.
            // Enable texture mapping
            // GLES20.glEnable(GLES20.GL_TEXTURE_2D);

            // Position the eye in front of the origin.
            final float eyeX = 0.0f;
            final float eyeY = 0.0f;
            final float eyeZ = -0.5f;

            // We are looking toward the distance
            final float lookX = 0.0f;
            final float lookY = 0.0f;
            final float lookZ = -5.0f;

            // Set our up vector. This is where our head would be pointing were we holding the camera.
            final float upX = 0.0f;
            final float upY = 1.0f;
            final float upZ = 0.0f;

            // Set the view matrix. This matrix can be said to represent the camera position.
            // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
            // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
            Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

            final String vertexShader = RawResourceReader.readTextFileFromRawResource(getApplicationContext(), R.raw.texture_vertex_shader);
            final String fragmentShader = RawResourceReader.readTextFileFromRawResource(getApplicationContext(), R.raw.texture_fragment_shader);

            final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
            final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

            mProgramHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                    new String[] {"a_Position",  "a_Color", "a_Normal", "a_TexCoordinate"});

            // Define a simple shader program for our point.
            //        final String pointVertexShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_vertex_shader);
            //        final String pointFragmentShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_fragment_shader);

            //        final int pointVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
            //        final int pointFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
            //        mPointProgramHandle = ShaderHelper.createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle,
            //        		new String[] {"a_Position"});

            // Load the texture
            mTextureDataHandle = TextureHelper.loadTexture(getApplicationContext(), R.drawable.bg);


            // Set the OpenGL viewport to the same size as the surface.
            //                GLES20.glViewport(0, 0, width, height);

            // Create a new perspective projection matrix. The height will stay the same
            // while the width will vary as per aspect ratio.
            final float ratio = (float) width / height;
            final float left = -ratio;
            final float right = ratio;
            final float bottom = -1.0f;
            final float top = 1.0f;
            final float near = 1.0f;
            final float far = 10.0f;

            Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
        }

        @Override
        public void onDraw() {
            super.onDraw();


            GLES20.glClearColor(0.0f, 0.2f, 0.2f, 1);

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            // Do a complete rotation every 10 seconds.
            long time = SystemClock.uptimeMillis() % 10000L;
            float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

            // Set our per-vertex lighting program.
            GLES20.glUseProgram(mProgramHandle);

            // Set program handles for cube drawing.
            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
            mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
            //        mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
            mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
            mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
            mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
            //        mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");
            mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");

            // Set the active texture unit to texture unit 0.
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

            // Bind the texture to this unit.
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);

            // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
            GLES20.glUniform1i(mTextureUniformHandle, 0);

            // Calculate position of the light. Rotate and then push into the distance.
            //        Matrix.setIdentityM(mLightModelMatrix, 0);
            //        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -5.0f);
            //        Matrix.rotateM(mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
            //        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 2.0f);

            //        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
            //        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);

            // Draw some cubes.
            //        Matrix.setIdentityM(mModelMatrix, 0);
            //        Matrix.translateM(mModelMatrix, 0, 4.0f, 0.0f, -7.0f);
            //        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 0.0f, 0.0f);
            //        drawCube();
            //
            //        Matrix.setIdentityM(mModelMatrix, 0);
            //        Matrix.translateM(mModelMatrix, 0, -4.0f, 0.0f, -7.0f);
            //        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
            //        drawCube();
            //
            //        Matrix.setIdentityM(mModelMatrix, 0);
            //        Matrix.translateM(mModelMatrix, 0, 0.0f, 4.0f, -7.0f);
            //        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f);
            //        drawCube();

            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -7.0f);
            //		Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
            drawCube();

            //        Matrix.setIdentityM(mModelMatrix, 0);
            //        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -5.0f);
            //        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 1.0f, 0.0f);
            //        drawCube();

            // Draw a point to indicate the light.
            //        GLES20.glUseProgram(mPointProgramHandle);
            //        drawLight();
        }

        /**
         * Draws a cube.
         */
        private void drawCube()
        {
            // Pass in the position information
            mCubePositions.position(0);
            GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                    0, mCubePositions);

            GLES20.glEnableVertexAttribArray(mPositionHandle);

            // Pass in the color information
            mCubeColors.position(0);
            GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                    0, mCubeColors);

            GLES20.glEnableVertexAttribArray(mColorHandle);

            // Pass in the normal information
            //        mCubeNormals.position(0);
            //        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
            //        		0, mCubeNormals);

            //        GLES20.glEnableVertexAttribArray(mNormalHandle);

            // Pass in the texture coordinate information
            mCubeTextureCoordinates.position(0);
            GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
                    0, mCubeTextureCoordinates);

            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

            // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
            // (which currently contains model * view).
            Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

            // Pass in the modelview matrix.
            GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

            // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
            // (which now contains model * view * projection).
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

            // Pass in the combined matrix.
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

            // Pass in the light position in eye space.
            //        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

            // Draw the cube.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        }
    }

    private class Engine extends Gles2WatchFaceService.Engine {

        private final int BYTE_PER_FLOAT = 4;

        // Matrices for draw object by OpenGL
        private final float[] mMVPMatrix = new float[16];
        private float[] mViewMatrix = new float[16];
        private float[] mAmbientViewMatrix = new float[16];

        private float[][] mModelMatrix = new float[360][16];
        private float[] mProjectionMatrix = new float[16];

        private float[] mVpMatrix = new float[16];
        private float[] mAmbientVpMatrix = new float[16];

        // Instance calendar for update timer
        private Calendar mCalendar = Calendar.getInstance();

        private Gles2Program mProgram;

        private FloatBuffer mTriangle3Vertices;

        private TriangleList mMajorTicks;
        private TriangleList mMinorTicks;
        private TriangleList mHourHand;
        private TriangleList mMinuteHand;
        private TriangleList mSecondHand;

//         TEXTURE SURFACE
        private float[] mModelTextureMatrix = new float[16];
        private float[] mMVPTextureMatrix = new float[16];

        private FloatBuffer mCubePosition;
        private FloatBuffer mCubeColors;
        private FloatBuffer mCubeTextureCoordinates;

        private int mTextureUniformHandle;
        private int mPositionHandle;
        private int mColorHandle;
        private int mTextureCoordinateHandle;

        private int mMVPMatrixTextureHandle;
        private int mMVMatrixTextureHandle;

        private final int mPositionDataSize = 3;
        private final int mColorDataSize = 4;
        private final int mTextureCoordinateDataSize = 2;

        private int mProgrameWireframeCubeHandle;
        private int mTextureDataHandle;

        private Engine() {
        }


        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            Log.d(TAG, "onCreate");

            // Always call setWatchFaceStyle in here
            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setStatusBarGravity(Gravity.RIGHT | Gravity.TOP)
                    .setHotwordIndicatorGravity(Gravity.LEFT | Gravity.TOP)
                    .setShowSystemUiTime(false)
                    .build());
        }

        @Override
        public void onGlContextCreated() {
            super.onGlContextCreated();

            Log.d(TAG, "onGlContextCreated");

            // Set position camera in always - on and ambient
            Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -1.25f, 0, 0, 0f, 0, 1f, 0);
            Matrix.setLookAtM(mAmbientViewMatrix, 0, 0, 0, -3f, 0, 0, 0, 0, 1f, 0);


            // model matrix for per degree
            for (int i = 0; i < mModelMatrix.length; i++) {
                Matrix.setRotateM(mModelMatrix[i], 0, i, 0, 0, 1);
            }

            this.mProgram = new Gles2Program(getApplicationContext());
            mProgram.use();

            // create major ticks
            this.mMajorTicks = this.createMajorTicks(this.mProgram, 0.03f, 0.09f, new float[] { 1f, 1f, 1f, 1f});

            // create minor ticks
            this.mMinorTicks = this.createMinorTicks(this.mProgram, 0.02f, 0.06f, new float[]{
                    0.5f /* red */,
                    0.5f /* green */,
                    0.5f /* blue */,
                    1.0f /* alpha */
            });


            // create hands
            this.mHourHand = this.createHand(
                    this.mProgram,
                    0.1f,
                    0.6f,
                    new float[]{
                            1.0f, 0.0f, 0.0f, 1.0f
                    }
            );

            this.mMinuteHand = this.createHand(
                    this.mProgram,
                    0.06f,
                    0.7f,
                    new float[]{
                            0f, 0f, 0f, 1f
//                            0.9f /* red */,
//                            0.9f /* green */,
//                            0.9f /* blue */,
//                            1.0f /* alpha */
                    }
            );

            this.mSecondHand = this.createHand(
                    this.mProgram,
                    0.06f,
                    1f,
                    new float[]{
                            0f, 0f, 0f, 1f
                    }
            );

            this.initializeTextureData();
            this.setupTexture();

        }

        private void initializeTextureData() {

            final float[] cubePositionData = {
//                    // Front face
//                    -2.0f, 2.0f, 2.0f,
//                    -2.0f, -2.0f, 2.0f,
//                    2.0f, 2.0f, 2.0f,
//                    -2.0f, -2.0f, 2.0f,
//                    2.0f, -2.0f, 2.0f,
//                    2.0f, 2.0f, 2.0f,
//
//                    // Right face
//                    2.0f, 2.0f, 2.0f,
//                    2.0f, -2.0f, 2.0f,
//                    2.0f, 2.0f, -2.0f,
//                    2.0f, -2.0f, 2.0f,
//                    2.0f, -2.0f, -2.0f,
//                    2.0f, 2.0f, -2.0f,

                    // Back face
                    2.0f, 2.0f, -2.0f,
                    2.0f, -2.0f, -2.0f,
                    -2.0f, 2.0f, -2.0f,
                    2.0f, -2.0f, -2.0f,
                    -2.0f, -2.0f, -2.0f,
                    -2.0f, 2.0f, -2.0f,

//                    // Left face
//                    -2.0f, 2.0f, -2.0f,
//                    -2.0f, -2.0f, -2.0f,
//                    -2.0f, 2.0f, 2.0f,
//                    -2.0f, -2.0f, -2.0f,
//                    -2.0f, -2.0f, 2.0f,
//                    -2.0f, 2.0f, 2.0f,
//
//                    // Top face
//                    -2.0f, 2.0f, -2.0f,
//                    -2.0f, 2.0f, 2.0f,
//                    2.0f, 2.0f, -2.0f,
//                    -2.0f, 2.0f, 2.0f,
//                    2.0f, 2.0f, 2.0f,
//                    2.0f, 2.0f, -2.0f,
//
//                    // Bottom face
//                    2.0f, -2.0f, -2.0f,
//                    2.0f, -2.0f, 2.0f,
//                    -2.0f, -2.0f, -2.0f,
//                    2.0f, -2.0f, 2.0f,
//                    -2.0f, -2.0f, 2.0f,
//                    -2.0f, -2.0f, -2.0f,
            };

            final float[] cubeColorData = {
//                    // Front face (red)
//                    1.0f, 0.0f, 0.0f, 1.0f,
//                    1.0f, 0.0f, 0.0f, 1.0f,
//                    1.0f, 0.0f, 0.0f, 1.0f,
//                    1.0f, 0.0f, 0.0f, 1.0f,
//                    1.0f, 0.0f, 0.0f, 1.0f,
//                    1.0f, 0.0f, 0.0f, 1.0f,
//
//                    // Right face (green)
//                    0.0f, 1.0f, 0.0f, 1.0f,
//                    0.0f, 1.0f, 0.0f, 1.0f,
//                    0.0f, 1.0f, 0.0f, 1.0f,
//                    0.0f, 1.0f, 0.0f, 1.0f,
//                    0.0f, 1.0f, 0.0f, 1.0f,
//                    0.0f, 1.0f, 0.0f, 1.0f,

                    // Back face (blue)
                    0.0f, 0.0f, 1.0f, 1.0f,
                    0.0f, 1.0f, 1.0f, 1.0f,
                    0.0f, 1.0f, 1.0f, 1.0f,
                    0.0f, 1.0f, 1.0f, 1.0f,
                    0.0f, 0.0f, 1.0f, 1.0f,
                    0.0f, 0.0f, 1.0f, 1.0f,

//                    // Left face (yellow)
//                    1.0f, 1.0f, 0.0f, 1.0f,
//                    1.0f, 1.0f, 0.0f, 1.0f,
//                    1.0f, 1.0f, 0.0f, 1.0f,
//                    1.0f, 1.0f, 0.0f, 1.0f,
//                    1.0f, 1.0f, 0.0f, 1.0f,
//                    1.0f, 1.0f, 0.0f, 1.0f,
//
//                    // Top face (cyan)
//                    0.0f, 1.0f, 1.0f, 1.0f,
//                    0.0f, 1.0f, 1.0f, 1.0f,
//                    0.0f, 1.0f, 1.0f, 1.0f,
//                    0.0f, 1.0f, 1.0f, 1.0f,
//                    0.0f, 1.0f, 1.0f, 1.0f,
//                    0.0f, 1.0f, 1.0f, 1.0f,
//
//                    // Bottom face (magenta)
//                    1.0f, 0.0f, 1.0f, 1.0f,
//                    1.0f, 0.0f, 1.0f, 1.0f,
//                    1.0f, 0.0f, 1.0f, 1.0f,
//                    1.0f, 0.0f, 1.0f, 1.0f,
//                    1.0f, 0.0f, 1.0f, 1.0f,
//                    1.0f, 0.0f, 1.0f, 1.0f
            };

            final float[] cubeTextureCoordinateData = {
//                    // Front face
//                    0.0f, 0.0f,
//                    0.0f, 1.0f,
//                    1.0f, 0.0f,
//                    0.0f, 1.0f,
//                    1.0f, 1.0f,
//                    1.0f, 0.0f,
//
//
//                    //				// Right face
//                    0.0f, 0.0f,
//                    0.0f, 1.0f,
//                    1.0f, 0.0f,
//                    0.0f, 1.0f,
//                    1.0f, 1.0f,
//                    1.0f, 0.0f,

                    // Back face
                    0.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f,

//                    // Left face
//                    0.0f, 0.0f,
//                    0.0f, 1.0f,
//                    1.0f, 0.0f,
//                    0.0f, 1.0f,
//                    1.0f, 1.0f,
//                    1.0f, 0.0f,
//
//                    // Top face
//                    0.0f, 0.0f,
//                    0.0f, 1.0f,
//                    1.0f, 0.0f,
//                    0.0f, 1.0f,
//                    1.0f, 1.0f,
//                    1.0f, 0.0f,
//
//                    // Bottom face
//                    0.0f, 0.0f,
//                    0.0f, 1.0f,
//                    1.0f, 0.0f,
//                    0.0f, 1.0f,
//                    1.0f, 1.0f,
//                    1.0f, 0.0f
            };

            this.mCubePosition = ByteBuffer.allocateDirect(cubePositionData.length * BYTE_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mCubePosition.put(cubePositionData).position(0);

            this.mCubeColors = ByteBuffer.allocateDirect(cubeColorData.length * BYTE_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mCubeColors.put(cubeColorData).position(0);

            this.mCubeTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinateData.length * BYTE_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mCubeTextureCoordinates.put(cubeTextureCoordinateData).position(0);
        }

        private void setupTexture() {
            final String vertexShader = RawResourceReader.readTextFileFromRawResource(getApplicationContext(), R.raw.texture_vertex_shader);
            final String fragmentShader = RawResourceReader.readTextFileFromRawResource(getApplicationContext(), R.raw.texture_fragment_shader);

            final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
            final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

            this.mProgrameWireframeCubeHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                    new String[] {"a_Position",  "a_Color", "a_Normal", "a_TexCoordinate"});


            this.mTextureDataHandle = TextureHelper.loadTexture(getApplicationContext(), R.drawable.bg);

            mMVPMatrixTextureHandle = GLES20.glGetUniformLocation(this.mProgrameWireframeCubeHandle, "u_MVPMatrix");
            mMVMatrixTextureHandle = GLES20.glGetUniformLocation(this.mProgrameWireframeCubeHandle, "u_MVMatrix");
            mTextureUniformHandle = GLES20.glGetUniformLocation(this.mProgrameWireframeCubeHandle, "u_Texture");
            mPositionHandle = GLES20.glGetAttribLocation(this.mProgrameWireframeCubeHandle, "a_Position");
            mColorHandle = GLES20.glGetAttribLocation(this.mProgrameWireframeCubeHandle, "a_Color");
            mTextureCoordinateHandle = GLES20.glGetAttribLocation(this.mProgrameWireframeCubeHandle, "a_TexCoordinate");
        }

        private TriangleList createHand(Gles2Program program, float width, float height, float[] color) {
            float[] handCoord = new float[] {
                    0, height, 0,
                    -width/2, 0, 0,
                    width/2, 0, 0
            };

            return new TriangleList(program, handCoord, color);
        }

        private TriangleList createMajorTicks(Gles2Program program, float width, float height, float[] color) {
            float[] trianglesCoords = new float[9*4];

            for (int i = 0; i < 4; i++) {
                float[] triagleCoords = getTickTriangleCoords(0.03f, 0.09f, i*360/4);
                System.arraycopy(triagleCoords, 0, trianglesCoords, i*9, triagleCoords.length);
            }

            return new TriangleList(program, trianglesCoords, color);
        }

        private TriangleList createMinorTicks(Gles2Program program, float width, float height, float[] color) {
            float[] trianglesCoords = new float[9 * (12 - 4)];
            int index = 0;

            for (int i = 0; i < 12; i++) {
                if (i % 3 == 0) {
                    continue;
                }
                float[] triangleCoords = getTickTriangleCoords(0.02f, 0.06f, i * 360 / 12);
                System.arraycopy(triangleCoords, 0, trianglesCoords, index, triangleCoords.length);
                index += 9;
            }

            return new TriangleList(program, trianglesCoords, color);
        }

        private float[] getTickTriangleCoords(float width, float height, int angleDegree) {
            float[] coords = new float[] {
                    0, 1, 0,
                    width/2, height + 1, 0,
                    -width/2, height + 1, 0
            };

            this.rotateCoords(coords, angleDegree);

            return coords;
        }

        // TODO: need to diving into this function
        private void rotateCoords(float[] coords, int angleDegrees) {
            double angleRadians = Math.toRadians(angleDegrees);
            double cos = Math.cos(angleRadians);
            double sin = Math.sin(angleRadians);
            for (int i = 0; i < coords.length; i += 3) {
                float x = coords[i];
                float y = coords[i + 1];
                coords[i] = (float) (cos * x - sin * y);
                coords[i + 1] = (float) (sin * x + cos * y);
            }
        }

        @Override
        public void onGlSurfaceCreated(int width, int height) {
            super.onGlSurfaceCreated(width, height);

            Log.d(TAG, "onGlSurfaceCreated");
            // Set the background clear color to black.
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

//             Use culling to remove back faces.
//            GLES20.glEnable(GLES20.GL_CULL_FACE);

//             Enable depth testing
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);


            final float aspectRatio = (float) (width / height);

            // Always set projection matrix in onGlSurfaceCreated
            Matrix.frustumM(mProjectionMatrix, 0, -aspectRatio, aspectRatio, -1, 1, 1, 10);

            Matrix.multiplyMM(mVpMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
            Matrix.multiplyMM(mAmbientVpMatrix, 0, mProjectionMatrix, 0, mAmbientViewMatrix, 0);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();

            Log.d(TAG, "onTimeTick");

            invalidate();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            Log.d(TAG, "onAmbientModeChanged");

            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            Log.d(TAG, "onVisibilityChanged");

            //TODO: implement later
            //            if (visible) {
            //                registerReceiver();
            //
            //                mCalendar.setTimeZone(TimeZone.getDefault());
            //
            invalidate();
            //            } else {
            //                unregisterReceiver();
            //            }

        }

        private void registerReceiver() {
            // TODO: implement later
        }

        private void unregisterReceiver() {
            // TODO: implement later
        }


        @Override
        public void onDraw() {
            super.onDraw();

            Log.d(TAG, "onDraw");

            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);


            mCalendar.setTimeInMillis(System.currentTimeMillis());
            float seconds =
                    mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f;
            float minutes = mCalendar.get(Calendar.MINUTE) + seconds / 60f;
            float hours = mCalendar.get(Calendar.HOUR) + minutes / 60f;
            final int secIndex = (int) (seconds *360 / 60f);
            final int minIndex = (int) (minutes / 60f * 360f);
            final int hoursIndex = (int) (hours / 12f * 360f);

            GLES20.glUseProgram(mProgrameWireframeCubeHandle);
//
//            mMVPMatrixTextureHandle = GLES20.glGetUniformLocation(this.mProgrameWireframeCubeHandle, "u_MVPMatrix");
//            mMVMatrixTextureHandle = GLES20.glGetUniformLocation(this.mProgrameWireframeCubeHandle, "u_MVMatrix");
//            mTextureUniformHandle = GLES20.glGetUniformLocation(this.mProgrameWireframeCubeHandle, "u_Texture");
//            mPositionHandle = GLES20.glGetAttribLocation(this.mProgrameWireframeCubeHandle, "a_Position");
//            mColorHandle = GLES20.glGetAttribLocation(this.mProgrameWireframeCubeHandle, "a_Color");
//            mTextureCoordinateHandle = GLES20.glGetAttribLocation(this.mProgrameWireframeCubeHandle, "a_TexCoordinate");
//
//            // Set the active texture unit to texture unit 0.
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//
//            // Bind the texture to this unit.
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);
//
//            // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
            GLES20.glUniform1i(mTextureUniformHandle, 0);
//
            Matrix.setIdentityM(mModelTextureMatrix, 0);
            Matrix.translateM(mModelTextureMatrix, 0, 0.0f, 0.0f, 2.7f);
//            Matrix.translateM(mModelTextureMatrix, 0, 0.0f, 0.0f, 4.0f);
            long time = SystemClock.uptimeMillis() % 1000L;
            float angleInDegrees = (360.0f / 1000.0f) * ((int) time);
//            Matrix.rotateM(mModelTextureMatrix, 0, angleInDegrees , 0.0f, 0.0f, 1.0f);
            drawCube();


            this.mProgram.use();
            // Draw hour hand
            {
                Matrix.multiplyMM(this.mMVPMatrix, 0, mVpMatrix, 0, this.mModelMatrix[hoursIndex], 0);
                this.mHourHand.draw(this.mMVPMatrix);
            }

            // Draw minute hand
            {
                Matrix.multiplyMM(this.mMVPMatrix, 0, mVpMatrix, 0, this.mModelMatrix[minIndex], 0);
                this.mMinuteHand.draw(this.mMVPMatrix);
            }

            // Draw second hand
            {
                Matrix.multiplyMM(this.mMVPMatrix, 0, mVpMatrix, 0, this.mModelMatrix[secIndex], 0);
                this.mSecondHand.draw(this.mMVPMatrix);
            }

            {
                this.mMajorTicks.draw(this.mVpMatrix);

                this.mMinorTicks.draw(this.mVpMatrix);
            }

            if (isVisible() && !isInAmbientMode()) {
                invalidate();
            }
        }

        private void drawCube() {

            // Pass in the position information
            mCubePosition.position(0);
            GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                    0, mCubePosition);

            GLES20.glEnableVertexAttribArray(mPositionHandle);

            // Pass in the color information
            mCubeColors.position(0);
            GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                    0, mCubeColors);

            GLES20.glEnableVertexAttribArray(mColorHandle);

            // Pass in the normal information
            //        mCubeNormals.position(0);
            //        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
            //        		0, mCubeNormals);

            //        GLES20.glEnableVertexAttribArray(mNormalHandle);

            // Pass in the texture coordinate information
            mCubeTextureCoordinates.position(0);
            GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
                    0, mCubeTextureCoordinates);

            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

            // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
            // (which currently contains model * view).
            Matrix.multiplyMM(mMVPTextureMatrix, 0, mViewMatrix, 0, mModelTextureMatrix, 0);

            // Pass in the modelview matrix.
            GLES20.glUniformMatrix4fv(mMVMatrixTextureHandle, 1, false, mMVPTextureMatrix, 0);

            // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
            // (which now contains model * view * projection).
            Matrix.multiplyMM(mMVPTextureMatrix, 0, mProjectionMatrix, 0, mMVPTextureMatrix, 0);

            // Pass in the combined matrix.
            GLES20.glUniformMatrix4fv(mMVPMatrixTextureHandle, 1, false, mMVPTextureMatrix, 0);

            // Pass in the light position in eye space.
            //        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

            // Draw the cube.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        }


        //        private void drawTriangle(int indexRotation, final FloatBuffer aTriangleBuffer) {
        //
        //            Log.d(TAG, "drawTriangle");
        //
        //            aTriangleBuffer.position(mPositionOffset);
        //            GLES20.glVertexAttribPointer(mProgram.mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
        //                    mStrideBytes, aTriangleBuffer);
        //
        //            GLES20.glEnableVertexAttribArray(mProgram.mPositionHandle);
        //
        //            // Pass in the color information
        //            aTriangleBuffer.position(mColorOffset);
        //            GLES20.glVertexAttribPointer(mProgram.mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
        //                    mStrideBytes, aTriangleBuffer);
        //
        //            GLES20.glEnableVertexAttribArray(mProgram.mColorHandle);
        //
        //            Matrix.multiplyMM(mMVPMatrix, 0, mVpMatrix, 0, mModelMatrix[indexRotation], 0);
        //
        //            GLES20.glUniformMatrix4fv(mProgram.mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        //            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
        //        }

        //        protected String getVertexShader() {
        //            return RawResourceReader.readTextFileFromRawResource(getApplicationContext(), R.raw.per_pixel_vertex_shader);
        //
        //        }
        //
        //        protected String getFragmentShader() {
        //            return RawResourceReader.readTextFileFromRawResource(getApplicationContext(), R.raw.per_pixel_fragment_shader);
        //        }
        //
        //        private int compileShader(final int shaderType, final String shaderSource) {
        //            int shaderHandle = GLES20.glCreateShader(shaderType);
        //
        //            if (shaderHandle != 0) {
        //                // Pass in the shader source.
        //                GLES20.glShaderSource(shaderHandle, shaderSource);
        //
        //                // Compile the shader.
        //                GLES20.glCompileShader(shaderHandle);
        //
        //                // Get the compilation status.
        //                final int[] compileStatus = new int[1];
        //                GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        //
        //                // If the compilation failed, delete the shader.
        //                if (compileStatus[0] == 0) {
        //                    Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
        //                    GLES20.glDeleteShader(shaderHandle);
        //                    shaderHandle = 0;
        //                }
        //            }
        //
        //            if (shaderHandle == 0) {
        //                throw new RuntimeException("Error creating shader.");
        //            }
        //
        //            return shaderHandle;
        //        }

        //        private int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) {
        //            int programHandle = GLES20.glCreateProgram();
        //
        //            if (programHandle != 0) {
        //                // Bind the vertex shader to the program.
        //                GLES20.glAttachShader(programHandle, vertexShaderHandle);
        //
        //                // Bind the fragment shader to the program.
        //                GLES20.glAttachShader(programHandle, fragmentShaderHandle);
        //
        //                // Bind attributes
        //                if (attributes != null) {
        //                    final int size = attributes.length;
        //                    for (int i = 0; i < size; i++) {
        //                        GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
        //                    }
        //                }
        //
        //                // Link the two shaders together into a program.
        //                GLES20.glLinkProgram(programHandle);
        //
        //                // Get the link status.
        //                final int[] linkStatus = new int[1];
        //                GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
        //
        //                // If the link failed, delete the program.
        //                if (linkStatus[0] == 0) {
        //                    Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
        //                    GLES20.glDeleteProgram(programHandle);
        //                    programHandle = 0;
        //                }
        //            }
        //
        //            if (programHandle == 0) {
        //                throw new RuntimeException("Error creating program.");
        //            }
        //
        //            return programHandle;
        //        }

    }
}
