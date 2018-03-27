package com.example.nvdang.analogwatchfaceopengl.simpleDialogWatchface;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.Gles2WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.SurfaceHolder;

import com.example.nvdang.analogwatchfaceopengl.R;
import com.example.nvdang.analogwatchfaceopengl.sample.ComplicationConfigActivity;
import com.example.nvdang.analogwatchfaceopengl.utils.Gles2Program;
import com.example.nvdang.analogwatchfaceopengl.utils.RawResourceReader;
import com.example.nvdang.analogwatchfaceopengl.utils.ShaderHelper;
import com.example.nvdang.analogwatchfaceopengl.utils.TextureHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Calendar;

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
public class WatchFaceService extends Gles2WatchFaceService {

    private static final String TAG = "MyOpenGLWatchFace";

     /*Create List Compllication*/

    // TODO: Step 2, intro 1
    private static final int LEFT_COMPLICATION_ID = 0;
    private static final int RIGHT_COMPLICATION_ID = 1;
    private static final int BOTTOM_COMPLICATION_ID = 2;

    private static final int[] COMPLICATION_IDS = {LEFT_COMPLICATION_ID,
            RIGHT_COMPLICATION_ID, BOTTOM_COMPLICATION_ID};

    private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;

    private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;

    // Left and right dial supported types.
    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            },
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            },

            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            }
    };


    // Used by {@link ComplicationConfigActivity} to retrieve id for complication locations and
    // to check if complication location is supported.
    // TODO: Step 3, expose complication information, part 1
    static int getComplicationId(
            ComplicationConfigActivity.ComplicationLocation complicationLocation) {
        switch (complicationLocation) {
            case LEFT:
                return LEFT_COMPLICATION_ID;
            case RIGHT:
                return RIGHT_COMPLICATION_ID;
            case BOTTOM:
                return BOTTOM_COMPLICATION_ID;
            default:
                return -1;
        }
    }

    // Used by {@link ComplicationConfigActivity} to retrieve all complication ids.
    // TODO: Step 3, expose complication information, part 2
    static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    // Used by {@link ComplicationConfigActivity} to retrieve complication types supported by
    // location.
    // TODO: Step 3, expose complication information, part 3
    static int[] getSupportedComplicationTypes(
            ComplicationConfigActivity.ComplicationLocation complicationLocation) {
        switch (complicationLocation) {
            case LEFT:
                return COMPLICATION_SUPPORTED_TYPES[0];
            case RIGHT:
                return COMPLICATION_SUPPORTED_TYPES[1];
            case BOTTOM:
                return COMPLICATION_SUPPORTED_TYPES[2];
            default:
                return new int[]{};
        }
    }

    /*----- Create Engine -----*/

    @Override
    public Engine onCreateEngine() {
        return new Engine();
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

        private int mProgramWireframeCubeHandle;
        private int mTextureDataHandle;
        private int mComplicationTextureDataHandle;


        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            Log.d(TAG, "onCreate");

            // Always call setWatchFaceStyle in here
            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setStatusBarGravity(Gravity.RIGHT | Gravity.TOP)
                    .setHotwordIndicatorGravity(Gravity.LEFT | Gravity.TOP)
                    .setShowSystemUiTime(false)
                    .build());

            initializeComplications();
        }

         /*-- Init Complications --*/

        // TODO: Step 2, initializeComplications()
        private void initializeComplications() {
            Log.d(TAG, "initializeComplications()");

            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            ComplicationDrawable leftComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            leftComplicationDrawable.setContext(getApplicationContext());

            ComplicationDrawable rightComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            rightComplicationDrawable.setContext(getApplicationContext());

            ComplicationDrawable bottomComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            bottomComplicationDrawable.setContext(getApplicationContext());


            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
            mComplicationDrawableSparseArray.put(LEFT_COMPLICATION_ID, leftComplicationDrawable);
            mComplicationDrawableSparseArray.put(RIGHT_COMPLICATION_ID, rightComplicationDrawable);
            mComplicationDrawableSparseArray.put(BOTTOM_COMPLICATION_ID, bottomComplicationDrawable);

            setActiveComplications(COMPLICATION_IDS);
            // mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
            // mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
        }

        /*-- Implementation onComplicationUpdate function --*/

        @Override
        public void onComplicationDataUpdate(int complicationId, ComplicationData complicationData) {
            Log.e(TAG, "onComplicationDataUpdate() id: " + complicationId);

            // Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);

            // Updates correct ComplicationDrawable with updated data.
            ComplicationDrawable complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
            complicationDrawable.setComplicationData(complicationData);
            complicationDrawable.setBorderColorActive(Color.BLUE);

            Rect bounds = new Rect();
            if (complicationData.getType() == ComplicationData.TYPE_LONG_TEXT) {
            } else {
            }
            complicationDrawable.setBounds(bounds);

            Bitmap bm = TextureHelper.drawableToBitmap(complicationDrawable);
            this.mTextureDataHandle = TextureHelper.getTextureFromBitmap(bm);

            invalidate();
        }

        @Override
        public void onGlSurfaceCreated(int width, int height) {
            super.onGlSurfaceCreated(width, height);

            Log.d(TAG, "onGlSurfaceCreated");
            // Set the background clear color to black.
            //GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

            // Use culling to remove back faces.
            //GLES20.glEnable(GLES20.GL_CULL_FACE);

            // Enable depth testing
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);


            final float aspectRatio = (float) (width / height);

            // Always set projection matrix in onGlSurfaceCreated
            Matrix.frustumM(mProjectionMatrix, 0, -aspectRatio, aspectRatio, -1, 1, 1, 10);

            Matrix.multiplyMM(mVpMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
            Matrix.multiplyMM(mAmbientVpMatrix, 0, mProjectionMatrix, 0, mAmbientViewMatrix, 0);

            setComplicationLocation(width, height);
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
            this.mMajorTicks = this.createMajorTicks(this.mProgram,
                    0.08f,
                    0.1f,
                    new float[]{
                            0f, 0f, 1f, 1f
                    });

            // create minor ticks
            this.mMinorTicks = this.createMinorTicks(this.mProgram,
                    0.04f,
                    0.07f,
                    new float[]{
                            1f, 1f, 1f, 1f
                    });


            // create hands
            this.mHourHand = this.createHand(
                    this.mProgram,
                    0.08f,
                    0.4f,
                    new float[]{
                            1f, 1f, 0f, 1f
                    });

            this.mMinuteHand = this.createHand(
                    this.mProgram,
                    0.08f,
                    0.6f,
                    new float[]{
                            1f, 1f, 1f, 1f
                    });

            this.mSecondHand = this.createHand(
                    this.mProgram,
                    0.08f,
                    1f,
                    new float[]{
                            1f, 1f, 1f, 1f
                    });

            this.initializeTextureData();
            this.setupTexture();

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

        //----------------------------------------------------------------------------
        // -- Init - setup
        //----------------------------------------------------------------------------

        private void initializeTextureData() {

            final float[] cubePositionData = {
                    2.0f, 2.0f, -2.0f,
                    2.0f, -2.0f, -2.0f,
                    -2.0f, 2.0f, -2.0f,
                    2.0f, -2.0f, -2.0f,
                    -2.0f, -2.0f, -2.0f,
                    -2.0f, 2.0f, -2.0f,
            };

            final float[] cubeColorData = {
                    // Blue
                    0.0f, 0.0f, 1.0f, 1.0f,
                    0.0f, 1.0f, 1.0f, 1.0f,
                    0.0f, 1.0f, 1.0f, 1.0f,
                    0.0f, 1.0f, 1.0f, 1.0f,
                    0.0f, 0.0f, 1.0f, 1.0f,
                    0.0f, 0.0f, 1.0f, 1.0f,
            };

            final float[] cubeTextureCoordinateData = {
                    0.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f,
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

            this.mProgramWireframeCubeHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                    new String[]{"a_Position", "a_Color", "a_Normal", "a_TexCoordinate"});


            this.mTextureDataHandle = TextureHelper.getTextureFromResourceId(getApplicationContext(), R.drawable.bg);

            mMVPMatrixTextureHandle = GLES20.glGetUniformLocation(this.mProgramWireframeCubeHandle, "u_MVPMatrix");
            mMVMatrixTextureHandle = GLES20.glGetUniformLocation(this.mProgramWireframeCubeHandle, "u_MVMatrix");
            mTextureUniformHandle = GLES20.glGetUniformLocation(this.mProgramWireframeCubeHandle, "u_Texture");
            mPositionHandle = GLES20.glGetAttribLocation(this.mProgramWireframeCubeHandle, "a_Position");
            mColorHandle = GLES20.glGetAttribLocation(this.mProgramWireframeCubeHandle, "a_Color");
            mTextureCoordinateHandle = GLES20.glGetAttribLocation(this.mProgramWireframeCubeHandle, "a_TexCoordinate");
        }

        private void setupTextureForComplication() {

        }

        private TriangleList createHand(Gles2Program program, float width, float height, float[] color) {
            float[] handCoord = new float[]{
                    0, height, 0,
                    -width / 2, 0, 0,
                    width / 2, 0, 0
            };

            return new TriangleList(program, handCoord, color);
        }

        private TriangleList createMajorTicks(Gles2Program program, float width, float height, float[] color) {
            float[] trianglesCoords = new float[9 * 4];

            for (int i = 0; i < 4; i++) {
                float[] triangleCoords = getTickTriangleCoords(width, height, i * 360 / 4);
                System.arraycopy(triangleCoords, 0, trianglesCoords, i * 9, triangleCoords.length);
            }

//            return new TriangleList(program, trianglesCoords, color);

            return new TriangleList(this.mProgram, trianglesCoords, color);
        }

        private TriangleList createMinorTicks(Gles2Program program, float width, float height, float[] color) {
            float[] trianglesCoords = new float[9 * (12 - 4)];
            int index = 0;

            for (int i = 0; i < 12; i++) {
                if (i % 3 == 0) {
                    continue;
                }
                float[] triangleCoords = getTickTriangleCoords(width, height, i * 360 / 12);

                System.arraycopy(triangleCoords, 0, trianglesCoords, index, triangleCoords.length);
                index += 9;
            }

            return new TriangleList(program, trianglesCoords, color);
        }

        private float[] getTickTriangleCoords(float width, float height, int angleDegree) {
            float[] coords = new float[]{
                    0, 1, 0,
                    width / 2, height + 1, 0,
                    -width / 2, height + 1, 0
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

         /*-- Set Complication location --*/

        private void setComplicationLocation(int width, int height) {

            Log.d(TAG, "set complication location was called");
            int sizeOfComplication = width / 4;
            int midpointOfScreen = width / 2;

            int horizontalOffset = (midpointOfScreen - sizeOfComplication) / 2;
            int verticalOffset = midpointOfScreen - (sizeOfComplication / 2);

            Rect leftBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            horizontalOffset,
                            verticalOffset,
                            (horizontalOffset + sizeOfComplication),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable leftComplicationDrawable = mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID);
            leftComplicationDrawable.setBounds(leftBounds);

            Rect rightBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (midpointOfScreen + horizontalOffset),
                            verticalOffset,
                            (midpointOfScreen + horizontalOffset + sizeOfComplication),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable rightComplicationDrawable =
                    mComplicationDrawableSparseArray.get(RIGHT_COMPLICATION_ID);
            rightComplicationDrawable.setBounds(rightBounds);

            Rect bottomBounds =
                    // Left, Top, Right, Bottom
                    new Rect((midpointOfScreen - sizeOfComplication),
                            (verticalOffset + sizeOfComplication),
                            (midpointOfScreen + horizontalOffset * 2),
                            (verticalOffset + sizeOfComplication * 2));

            ComplicationDrawable bottomComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BOTTOM_COMPLICATION_ID);
            bottomComplicationDrawable.setBounds(bottomBounds);
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

            GLES20.glClearColor(0.0f, 0.0f, 1.0f, 1);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);


            mCalendar.setTimeInMillis(System.currentTimeMillis());
            float seconds =
                    mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f;
            float minutes = mCalendar.get(Calendar.MINUTE) + seconds / 60f;
            float hours = mCalendar.get(Calendar.HOUR) + minutes / 60f;
            final int secIndex = (int) (seconds * 360 / 60f);
            final int minIndex = (int) (minutes / 60f * 360f);
            final int hoursIndex = (int) (hours / 12f * 360f);

            GLES20.glUseProgram(mProgramWireframeCubeHandle);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);
//
            GLES20.glUniform1i(mTextureUniformHandle, 0);
//
            Matrix.setIdentityM(mModelTextureMatrix, 0);
            Matrix.translateM(mModelTextureMatrix, 0, 0.0f, 0.0f, 2.7f);
//            Matrix.translateM(mModelTextureMatrix, 0, 0.0f, 0.0f, 4.0f);
//            long time = SystemClock.uptimeMillis() % 1000L;
//            float angleInDegrees = (360.0f / 1000.0f) * ((int) time);
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
            mCubePosition.position(0);
            GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                    0, mCubePosition);

            GLES20.glEnableVertexAttribArray(mPositionHandle);

            mCubeColors.position(0);
            GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                    0, mCubeColors);

            GLES20.glEnableVertexAttribArray(mColorHandle);

            mCubeTextureCoordinates.position(0);
            GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
                    0, mCubeTextureCoordinates);

            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

            Matrix.multiplyMM(mMVPTextureMatrix, 0, mViewMatrix, 0, mModelTextureMatrix, 0);

            GLES20.glUniformMatrix4fv(mMVMatrixTextureHandle, 1, false, mMVPTextureMatrix, 0);

            Matrix.multiplyMM(mMVPTextureMatrix, 0, mProjectionMatrix, 0, mMVPTextureMatrix, 0);

            GLES20.glUniformMatrix4fv(mMVPMatrixTextureHandle, 1, false, mMVPTextureMatrix, 0);

            // Draw the cube.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        }

        private void drawComplications() {
            long now = System.currentTimeMillis();
            int complicationId;
            ComplicationDrawable complicationDrawable;

            for (int id : COMPLICATION_IDS) {
                complicationId = id;
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
                //complicationDrawable.draw(canvas, now);

                Bitmap bm = TextureHelper.drawableToBitmap(complicationDrawable);
                this.mTextureDataHandle = TextureHelper.getTextureFromBitmap(bm);
            }
        }


    }
}
