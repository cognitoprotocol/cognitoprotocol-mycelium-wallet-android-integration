/*
 * Copyright (C) 2008 ZXing authors
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

package com.google.zxing.client.android.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.client.android.RotationUtil;
import com.google.zxing.client.android.camera.open.OpenCamera;
import com.google.zxing.client.android.camera.open.OpenCameraInterface;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one
 * talking to it. The implementation encapsulates the steps needed to take
 * preview-sized images, which are used for both preview and decoding.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CameraManager {

   private static final String TAG = CameraManager.class.getSimpleName();

   private static final int MIN_FRAME_WIDTH = 1;
   private static final int MIN_FRAME_HEIGHT = 1;
   private static final int MAX_FRAME_WIDTH = 960; // = 1920/2
   private static final int MAX_FRAME_HEIGHT = 540; // = 1080/2

   private final Context context;
   private final CameraConfigurationManager configManager;
   private OpenCamera camera;
   private AutoFocusManager autoFocusManager;
   private Rect framingRect;
   private Rect framingRectInPreview;
   private boolean initialized;
   private boolean previewing;
   private int requestedFramingRectWidth;
   private int requestedFramingRectHeight;
   private RotationUtil _rotationHelper;
   private int cameraId;

   /**
    * Preview frames are delivered here, which we pass on to the registered
    * handler. Make sure to clear the handler so it will only receive one
    * message.
    */
   private final PreviewCallback previewCallback;

   public CameraManager(Context context, int cameraId) {
      this.context = context;
      this.cameraId = cameraId;
      this.configManager = new CameraConfigurationManager(context);
      previewCallback = new PreviewCallback(configManager);
      _rotationHelper = new RotationUtil(context);
   }

   /**
    * Opens the camera driver and initializes the hardware parameters.
    * 
    * @param holder
    *           The surface object which the camera will draw preview frames
    *           into.
    * @throws IOException
    *            Indicates the camera driver failed to open.
    */
   public synchronized void openDriver(SurfaceHolder holder, boolean enableContinuousFocus)
         throws IOException {
      OpenCamera theCamera = camera;
      if (theCamera == null) {
         theCamera = OpenCameraInterface.open(cameraId);
         if (theCamera == null) {
            throw new IOException("Camera.open() failed to return object from driver");
         }
         camera = theCamera;
      }

      if (!initialized) {
         initialized = true;
         configManager.initFromCameraParameters(theCamera);
         if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) {
            setManualFramingRect(requestedFramingRectWidth, requestedFramingRectHeight);
            requestedFramingRectWidth = 0;
            requestedFramingRectHeight = 0;
         }
      }

      Camera cameraObject = theCamera.getCamera();
      Camera.Parameters parameters = cameraObject.getParameters();
      String parametersFlattened = parameters == null ? null : parameters.flatten(); // Save
                                                                                     // these,
                                                                                     // temporarily
      try {
         configManager.setDesiredCameraParameters(theCamera, false, enableContinuousFocus);
      } catch (RuntimeException re) {
         // Driver failed
         Log.w(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
         Log.i(TAG, "Resetting to saved camera params: " + parametersFlattened);
         // Reset:
         if (parametersFlattened != null) {
            parameters = cameraObject.getParameters();
            parameters.unflatten(parametersFlattened);
            try {
               cameraObject.setParameters(parameters);
               configManager.setDesiredCameraParameters(theCamera, true, enableContinuousFocus);
            } catch (RuntimeException re2) {
               // Well, darn. Give up
               Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
            }
         }
      }
      cameraObject.setPreviewDisplay(holder);
   }

   public synchronized boolean isOpen() {
      return camera != null;
   }

   /**
    * Closes the camera driver if still in use.
    */
   public synchronized void closeDriver() {
      if (camera != null) {
         camera.getCamera().release();
         camera = null;
         // Make sure to clear these each time we close the camera, so that any scanning rect
         // requested by intent is forgotten.
         framingRect = null;
         framingRectInPreview = null;
      }
   }

   /**
    * Asks the camera hardware to begin drawing preview frames to the screen.
    */
   public synchronized void startPreview() {
      OpenCamera theCamera = camera;
      if (theCamera != null && !previewing) {
         theCamera.getCamera().startPreview();
         previewing = true;
         autoFocusManager = new AutoFocusManager(context, theCamera.getCamera());
      }
   }

   /**
    * Tells the camera to stop drawing preview frames.
    */
   public synchronized void stopPreview() {
      if (autoFocusManager != null) {
         autoFocusManager.stop();
         autoFocusManager = null;
      }
      if (camera != null && previewing) {
         camera.getCamera().stopPreview();
         previewCallback.setHandler(null, 0);
         previewing = false;
      }
   }

   /**
    * Convenience method for
    * {@link com.google.zxing.client.android.CaptureActivity}
    */
   public synchronized void setTorch(boolean newSetting) {
      if (newSetting != configManager.getTorchState(camera.getCamera())) {
         if (camera != null) {
            if (autoFocusManager != null) {
               autoFocusManager.stop();
            }
            configManager.setTorch(camera.getCamera(), newSetting);
            if (autoFocusManager != null) {
               autoFocusManager.start();
            }
         }
      }
   }

   public synchronized boolean toggleTorch() {
      boolean newState = !configManager.getTorchState(camera.getCamera());
      setTorch(newState);
      return newState;
   }

   /**
    * A single preview frame will be returned to the handler supplied. The data
    * will arrive as byte[] in the message.obj field, with width and height
    * encoded as message.arg1 and message.arg2, respectively.
    * 
    * @param handler
    *           The handler to send the message to.
    * @param message
    *           The what field of the message to be sent.
    */
   public synchronized void requestPreviewFrame(Handler handler, int message) {
      Camera theCamera = camera.getCamera();
      if (theCamera != null && previewing) {
         previewCallback.setHandler(handler, message);
         theCamera.setOneShotPreviewCallback(previewCallback);
      }
   }

   /**
    * Calculates the framing rect which the UI should draw to show the user
    * where to place the barcode. This target helps with alignment as well as
    * forces the user to hold the device far enough away to ensure the image
    * will be in focus.
    * 
    * @return The rectangle to draw on screen in window coordinates.
    */
   public synchronized Rect getFramingRect() {
      if (framingRect == null) {
         if (camera == null) {
            return null;
         }
         Point screenResolution = configManager.getScreenResolution();
         if (screenResolution == null) {
            // Called early, before init even finished
            return null;
         }

         int width = findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
         int height = findDesiredDimensionInRange(screenResolution.y, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);

         // Make the view finder a square
         width = Math.min(width, height);
         height = width;
         
         int leftOffset = (screenResolution.x - width) / 2;
         int topOffset = (screenResolution.y - height) / 2;
         framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
         Log.d(TAG, "Calculated framing rect: " + framingRect);
      }
      return framingRect;
   }

   private static int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
      int dim = resolution / 2; // Target 50% of each dimension
      if (dim < hardMin) {
         return hardMin;
      }
      if (dim > hardMax) {
         return hardMax;
      }
      return dim;
   }

   /**
    * Like {@link #getFramingRect} but coordinates are in terms of the preview
    * frame, not UI / screen.
    */
   public synchronized Rect getFramingRectInPreview() {
      if (framingRectInPreview == null) {
         Rect framingRect = getFramingRect();
         if (framingRect == null) {
            return null;
         }
         Rect rect = new Rect(framingRect);
         Point cameraResolution = configManager.getCameraResolution();
         Point screenResolution = configManager.getScreenResolution();
         if (cameraResolution == null || screenResolution == null) {
            // Called early, before init even finished
            return null;
         }
         if (_rotationHelper.flipWidthAndHeight()) {
            rect.left = rect.left * cameraResolution.y / screenResolution.x;
            rect.right = rect.right * cameraResolution.y / screenResolution.x;
            rect.top = rect.top * cameraResolution.x / screenResolution.y;
            rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;
         } else {
            rect.left = rect.left * cameraResolution.x / screenResolution.x;
            rect.right = rect.right * cameraResolution.x / screenResolution.x;
            rect.top = rect.top * cameraResolution.y / screenResolution.y;
            rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
         }
         framingRectInPreview = rect;
      }
      return framingRectInPreview;
   }

   /**
    * Allows third party apps to specify the scanning rectangle dimensions,
    * rather than determine them automatically based on screen resolution.
    * 
    * @param width
    *           The width in pixels to scan.
    * @param height
    *           The height in pixels to scan.
    */
   public synchronized void setManualFramingRect(int width, int height) {
      if (initialized) {
         Point screenResolution = configManager.getScreenResolution();
         if (width > screenResolution.x) {
            width = screenResolution.x;
         }
         if (height > screenResolution.y) {
            height = screenResolution.y;
         }
         int leftOffset = (screenResolution.x - width) / 2;
         int topOffset = (screenResolution.y - height) / 2;
         framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
         Log.d(TAG, "Calculated manual framing rect: " + framingRect);
         framingRectInPreview = null;
      } else {
         requestedFramingRectWidth = width;
         requestedFramingRectHeight = height;
      }
   }

   /**
    * A factory method to build the appropriate LuminanceSource object based on
    * the format of the preview buffers, as described by Camera.Parameters.
    * 
    * @param data
    *           A preview frame.
    * @param width
    *           The width of the image.
    * @param height
    *           The height of the image.
    * @return A PlanarYUVLuminanceSource instance.
    */
   public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
      Rect rect = getFramingRectInPreview();
      if (rect == null) {
         return null;
      }
      // Go ahead and assume it's YUV rather than die.
      return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect.height(), false);
   }

   public int getCameraId() {
      if (camera != null) {
         // if everything worked, get the actual index reported by android
         return camera.getIndex();
      } else {
         // ...otherwise report the requested index back (might be -1)
         return cameraId;
      }
   }

   public boolean hasFlash() {
      // we assume the FEATURE_CAMERA_FLASH applies only to the back facing camera
      // TODO: With API21, Camera2 can provide availability of front side flashes separately.
      boolean backFacing;
      if (cameraId == -1) {
         backFacing = true;
      } else {
         Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
         Camera.getCameraInfo(cameraId, cameraInfo);
         backFacing = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK;
      }
      boolean hasFlash = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
      return backFacing && hasFlash;
   }
}
