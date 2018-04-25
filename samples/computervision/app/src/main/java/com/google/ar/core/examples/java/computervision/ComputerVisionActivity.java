/*
 * Copyright 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.computervision;

import android.graphics.ImageFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/** This is a simple example that demonstrates cpu image access with ARCore. */
public class ComputerVisionActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
  private static final String TAG = ComputerVisionActivity.class.getSimpleName();

  // This app demonstrates two approaches to obtaining image data accessible on CPU:
  // 1. Access the CPU image directly from ARCore. This approach delivers a frame without latency
  //    (if available), but currently is lower resolution than the GPU image.
  // 2. Download the texture from GPU. This approach incurs a 1-frame latency, but allows a high
  //    resolution image.
  private enum ImageAcquisitionPath {
    CPU_DIRECT_ACCESS,
    GPU_DOWNLOAD
  };

  // Select the image acquisition path here.
  private final ImageAcquisitionPath imageAcquisitionPath = ImageAcquisitionPath.CPU_DIRECT_ACCESS;

  // Session management and rendering.
  private GLSurfaceView surfaceView;
  private Session session;
  private boolean installRequested;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private CpuImageDisplayRotationHelper cpuImageDisplayRotationHelper;
  private CpuImageTouchListener cpuImageTouchListener;
  private final CpuImageRenderer cpuImageRenderer = new CpuImageRenderer();
  private final EdgeDetector edgeDetector = new EdgeDetector();

  // The fields below are used for the GPU_DOWNLOAD image acquisition path.
  private final TextureReader textureReader = new TextureReader();
  private int gpuDownloadFrameBufferIndex = -1;

  // ARCore full resolution GL texture typically has a size of 1920 x 1080.
  private static final int TEXTURE_WIDTH = 1920;
  private static final int TEXTURE_HEIGHT = 1080;

  // We choose a lower sampling resolution.
  private static final int IMAGE_WIDTH = 1280;
  private static final int IMAGE_HEIGHT = 720;

  // My implementation
//  private SensorManager mSensorManager;
//  private final float[] mAccelerometerReading = new float[3];
//  private final float[] mMagnetometerReading = new float[3];
//
//  private final float[] mRotationMatrix = new float[9];
//  private final float[] mOrientationAngles = new float[3];
//
//  SensorEventListener _SensorEventListener =   new SensorEventListener() {
//    @Override
//    public void onSensorChanged(SensorEvent event) {
//
//    }
//
//    @Override
//    public void onAccuracyChanged(Sensor sensor, int accuracy) {
//
//    }
//  };


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    surfaceView = findViewById(R.id.surfaceview);
    cpuImageDisplayRotationHelper = new CpuImageDisplayRotationHelper(/*context=*/ this);
    cpuImageTouchListener =
        new CpuImageTouchListener(cpuImageDisplayRotationHelper, cpuImageRenderer);

    // Setup a touch listener to control the texture splitter position.
    surfaceView.setOnTouchListener(cpuImageTouchListener);

    // Set up renderer.
    surfaceView.setPreserveEGLContextOnPause(true);
    surfaceView.setEGLContextClientVersion(2);
    surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
    surfaceView.setRenderer(this);
    surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

    installRequested = false;

    // My implementation
//    mSensorManager = (SensorManager) getSystemService(this.SENSOR_SERVICE);
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (session == null) {
      Exception exception = null;
      String message = null;
      try {
        switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
          case INSTALL_REQUESTED:
            installRequested = true;
            return;
          case INSTALLED:
            break;
        }

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        session = new Session(/* context= */ this);
      } catch (UnavailableArcoreNotInstalledException
          | UnavailableUserDeclinedInstallationException e) {
        message = "Please install ARCore";
        exception = e;
      } catch (UnavailableApkTooOldException e) {
        message = "Please update ARCore";
        exception = e;
      } catch (UnavailableSdkTooOldException e) {
        message = "Please update this app";
        exception = e;
      } catch (Exception e) {
        message = "This device does not support AR";
        exception = e;
      }

      if (message != null) {
        messageSnackbarHelper.showError(this, message);
        Log.e(TAG, "Exception creating session", exception);
        return;
      }

      // My implementation
//      mSensorManager.registerListener(this, Sensor.TYPE_ACCELEROMETER,
//              SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
//      mSensorManager.registerListener(this, Sensor.TYPE_MAGNETIC_FIELD,
//              SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }

    // Note that order matters - see the note in onPause(), the reverse applies here.
    try {
      session.resume();
    } catch (CameraNotAvailableException e) {
      // In some cases (such as another camera app launching) the camera may be given to
      // a different app instead. Handle this properly by showing a message and recreate the
      // session at the next iteration.
      messageSnackbarHelper.showError(this, "Camera not available. Please restart the app.");
      session = null;
      return;
    }
    surfaceView.onResume();
    cpuImageDisplayRotationHelper.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
      // Note that the order matters - GLSurfaceView is paused first so that it does not try
      // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
      // still call session.update() and get a SessionPausedException.
      cpuImageDisplayRotationHelper.onPause();
      surfaceView.onPause();
      session.pause();

      // My implementation
//      mSensorManager.unregisterListener(this);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
          .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

    // Create the texture and pass it to ARCore session to be filled during update().
    try {
      cpuImageRenderer.createOnGlThread(/* context= */ this);

      // The image format can be either IMAGE_FORMAT_RGBA or IMAGE_FORMAT_I8.
      // Set keepAspectRatio to false so that the output image covers the whole viewport.
      textureReader.create(
          /* context= */ this,
          TextureReaderImage.IMAGE_FORMAT_I8,
          IMAGE_WIDTH,
          IMAGE_HEIGHT,
          false);

    } catch (IOException e) {
      Log.e(TAG, "Failed to read an asset file", e);
    }
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    cpuImageDisplayRotationHelper.onSurfaceChanged(width, height);
    GLES20.glViewport(0, 0, width, height);
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    // Clear screen to notify driver it should not load any pixels from previous frame.
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    if (session == null) {
      return;
    }
    // Notify ARCore session that the view size changed so that the perspective matrix and
    // the video background can be properly adjusted.
    cpuImageDisplayRotationHelper.updateSessionIfNeeded(session);

    try {
      session.setCameraTextureName(cpuImageRenderer.getTextureId());
      Frame frame = session.update();

      switch (imageAcquisitionPath) {
        case CPU_DIRECT_ACCESS:
          renderProcessedImageCpuDirectAccess(frame);
          break;
        case GPU_DOWNLOAD:
          renderProcessedImageGpuDownload(frame);
          break;
      }

    } catch (Exception t) {
      // Avoid crashing the application due to unhandled exceptions.
      Log.e(TAG, "Exception on the OpenGL thread", t);
    }
  }

  /* Demonstrates how to access a CPU image directly from ARCore. */
  private void renderProcessedImageCpuDirectAccess(Frame frame) {
    try (Image image = frame.acquireCameraImage()) {
      if (image.getFormat() != ImageFormat.YUV_420_888) {
        throw new IllegalArgumentException(
            "Expected image in YUV_420_888 format, got format " + image.getFormat());
      }

      // My implementation
      getRGBSamples(image);

      ByteBuffer processedImageBytesGrayscale =
          edgeDetector.detect(
              image.getWidth(),
              image.getHeight(),
              image.getPlanes()[0].getRowStride(),
              image.getPlanes()[0].getBuffer());

      cpuImageRenderer.drawWithCpuImage(
          frame,
          image.getWidth(),
          image.getHeight(),
          processedImageBytesGrayscale,
          cpuImageDisplayRotationHelper.getViewportAspectRatio(),
          cpuImageDisplayRotationHelper.getCameraToDisplayRotation());

    } catch (NotYetAvailableException e) {
      // This exception will routinely happen during startup, and is expected. cpuImageRenderer
      // will handle null image properly, and will just render the background.
      cpuImageRenderer.drawWithoutCpuImage();
    }
  }

  /* Demonstrates how to access a CPU image using a download from GPU */
  private void renderProcessedImageGpuDownload(Frame frame) {
    // If there is a frame being requested previously, acquire the pixels and process it.
    if (gpuDownloadFrameBufferIndex >= 0) {
      TextureReaderImage image = textureReader.acquireFrame(gpuDownloadFrameBufferIndex);

      if (image.format != TextureReaderImage.IMAGE_FORMAT_I8) {
        throw new IllegalArgumentException(
            "Expected image in I8 format, got format " + image.format);
      }

      //Log.d("B", "?");

      ByteBuffer processedImageBytesGrayscale =
          edgeDetector.detect(image.width, image.height, /* stride= */ image.width, image.buffer);

      // You should always release frame buffer after using. Otherwise the next call to
      // submitFrame() may fail.
      textureReader.releaseFrame(gpuDownloadFrameBufferIndex);

      cpuImageRenderer.drawWithCpuImage(
          frame,
          IMAGE_WIDTH,
          IMAGE_HEIGHT,
          processedImageBytesGrayscale,
          cpuImageDisplayRotationHelper.getViewportAspectRatio(),
          cpuImageDisplayRotationHelper.getCameraToDisplayRotation());

    } else {
      cpuImageRenderer.drawWithoutCpuImage();
    }

    // Submit request for the texture from the current frame.
    gpuDownloadFrameBufferIndex =
        textureReader.submitFrame(cpuImageRenderer.getTextureId(), TEXTURE_WIDTH, TEXTURE_HEIGHT);
  }

  private void getRGBSamples(Image image)
  {
    // YUV to RGB conversion
    ByteBuffer buffer0 = image.getPlanes()[0].getBuffer();
    byte[] Y1 = new byte[buffer0.remaining()];
    buffer0.get(Y1);

    ByteBuffer buffer1 = image.getPlanes()[1].getBuffer();
    byte[] U1 = new byte[buffer1.remaining()];
    buffer1.get(U1);

    ByteBuffer buffer2 = image.getPlanes()[2].getBuffer();
    byte[] V1 = new byte[buffer2.remaining()];
    buffer2.get(V1);

    int Width = image.getWidth();
    int Height = image.getHeight();

    //ByteBuffer output = image.getPlanes()[1].getBuffer();

    //byte[] ImageRGB = new byte[image.getHeight()*image.getWidth()*3];
    int[] RGB = new int[image.getHeight()*image.getWidth()*3];

    for(int i = 0; i<Height-1; i++){
      for (int j = 0; j<Width; j++){
        int Y = Y1[i*Width+j]&0xFF;
        int U = U1[(i/2)*(Width/2)+j/2]&0xFF;
        int V = V1[(i/2)*(Width/2)+j/2]&0xFF;

        int R,G,B;

        R = (int)(Y + 1.402 * (V - 128));
        G = (int)(Y - 0.344 * (U - 128) - 0.714 * (V - 128));
        B = (int)(Y + 1.772 * (U - 128));

        // Clip rgb values to 0-255
        R = R < 0 ? 0 : R > 255 ? 255 : R;
        G = G < 0 ? 0 : G > 255 ? 255 : G;
        B = B < 0 ? 0 : B > 255 ? 255 : B;

//        ImageRGB[i*3*Width+j*3] = (byte)(R - 128);
//        ImageRGB[i*3*Width+j*3+1] = (byte)(G - 128);
//        ImageRGB[i*3*Width+j*3+2] = (byte)(B - 128);
        //ImageRGB[i*4*Width+j*4+3] = (byte) 127;//-1;

        RGB[i*3*Width+j*3] = R;
        RGB[i*3*Width+j*3+1] = G;
        RGB[i*3*Width+j*3+2] = B;
        //RGB[i*4*Width+j*4+3] = -1;

        if(i == Width/2 && j == Height/2)
        {
          //Log.d("Color ", String.valueOf(R) + " " + String.valueOf(G) + " " + String.valueOf(B));
          //Log.d("Color2 ", String.valueOf((byte)R) + " " + String.valueOf((byte)G) + " " + String.valueOf((byte)B));
          Log.d("Color3 ", String.valueOf(RGB[i*3*Width+j*3]) + " " + String.valueOf(RGB[i*3*Width+j*3 + 1]) + " " + String.valueOf(RGB[i*3*Width+j*3 + 2]));
        }

      }
    }



//    mSensorManager = (SensorManager) getSystemService(this.SENSOR_SERVICE);
//    mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
  }
}
