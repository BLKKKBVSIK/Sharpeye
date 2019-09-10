/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.detection;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;
import android.view.textclassifier.TextClassificationSessionId;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.customview.OverlayView.DrawCallback;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();

  // Configuration values for the prepackaged SSD model.
  private static final int TF_OD_API_INPUT_SIZE = 300;
  private static final boolean TF_OD_API_IS_QUANTIZED = true;
  private static final String TF_OD_API_MODEL_FILE = "detect_global.tflite";
  private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap_cars.txt";
  private static final DetectorMode MODE = DetectorMode.TF_OD_API;
  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
  private static final boolean MAINTAIN_ASPECT = false;
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;


  private static final int TF_OD_API_INPUT_SIZE_BIS = 300;
  private static final boolean TF_OD_API_IS_QUANTIZED_BIS = true;
  private static final String TF_OD_API_MODEL_FILE_BIS = "signDetectSmallData.tflite";
  private static final String TF_OD_API_LABELS_FILE_BIS = "file:///android_asset/labelsign.txt";
  private static final DetectorMode MODE_BIS = DetectorMode.TF_OD_API;
  private static final float MINIMUM_CONFIDENCE_TF_OD_API_BIS = 0.6f;

  private static final int TF_OD_API_INPUT_SIZE_SIGN = 300;
  private static final boolean TF_OD_API_IS_QUANTIZED_SIGN = true;
  private static final String TF_OD_API_MODEL_FILE_SIGN = "signDetectorSmall.tflite";
  private static final String TF_OD_API_LABELS_FILE_SIGN = "file:///android_asset/labelsigndetector.txt";
  private static final DetectorMode MODE_SIGN = DetectorMode.TF_OD_API;
  private static final float MINIMUM_CONFIDENCE_TF_OD_API_SIGN = 0.6f;





  OverlayView trackingOverlay;
  private Integer sensorOrientation;

  private Classifier detector;
  private Classifier detector_bis;
  private Classifier detector_sign;

  private boolean alternate = false;

  private Classifier signDetector;

  private long lastProcessingTimeMs;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private boolean computingDetection = false;
  private boolean computingDetectionBis = false;

  private boolean canCompute = false;
  private boolean canComputeBis = false;

  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;

  private BorderedText borderedText;

  boolean canRunSignDetector = true;
  Classifier.Recognition replacedResult = null;

  List<Long> fpsComputer;

  List<Bitmap> bufferedSigns;
  List<Classifier.Recognition> bufferedResults;

  private static final int BUFFER_SIZE = 5;

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    fpsComputer = new ArrayList<>();
    for (int i = 0; i < 5; ++i)
      fpsComputer.add(0, (long)0);
    tracker = new MultiBoxTracker(this);

    int cropSize = TF_OD_API_INPUT_SIZE;

    bufferedSigns = new ArrayList<>();
    bufferedResults = new ArrayList<>();

    try {
      detector =
          TFLiteObjectDetectionAPIModel.create(
              getAssets(),
              TF_OD_API_MODEL_FILE,
              TF_OD_API_LABELS_FILE,
              TF_OD_API_INPUT_SIZE,
              TF_OD_API_IS_QUANTIZED);
      cropSize = TF_OD_API_INPUT_SIZE;

      detector_bis =
              TFLiteObjectDetectionAPIModel.create(
                      getAssets(),
                      TF_OD_API_MODEL_FILE_BIS,
                      TF_OD_API_LABELS_FILE_BIS,
                      TF_OD_API_INPUT_SIZE_BIS,
                      TF_OD_API_IS_QUANTIZED_BIS);

      detector_sign =
              TFLiteObjectDetectionAPIModel.create(
                      getAssets(),
                      TF_OD_API_MODEL_FILE_SIGN,
                      TF_OD_API_LABELS_FILE_SIGN,
                      TF_OD_API_INPUT_SIZE_SIGN,
                      TF_OD_API_IS_QUANTIZED_SIGN);
    } catch (final IOException e) {
      e.printStackTrace();
      LOGGER.e(e, "Exception initializing classifier!");
      Toast toast =
          Toast.makeText(
              getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
      toast.show();
      finish();
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
    trackingOverlay.addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            tracker.draw(canvas);
            if (isDebug()) {
              tracker.drawDebug(canvas);
            }
          }
        });

    tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
  }

  public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
    int width = bm.getWidth();
    int height = bm.getHeight();
    float scaleWidth = ((float) newWidth) / width;
    float scaleHeight = ((float) newHeight) / height;
    // CREATE A MATRIX FOR THE MANIPULATION
    Matrix matrix = new Matrix();
    // RESIZE THE BIT MAP
    matrix.postScale(scaleWidth, scaleHeight);

    // "RECREATE" THE NEW BITMAP
    Bitmap resizedBitmap = Bitmap.createBitmap(
            bm, 0, 0, width, height, matrix, false);
    bm.recycle();
    return resizedBitmap;
  }

  @Override
  protected void processImage() {
    ++timestamp;
    final long currTimestamp = timestamp;
    trackingOverlay.postInvalidate();

    // No mutex needed as this method is not reentrant.
    if (computingDetection && computingDetectionBis) {
      readyForNextImage();
      return;
    }

    canCompute = false;
    canComputeBis = false;

    if (!computingDetection) {
      canCompute = true;
      computingDetection = true;
    }

    if (!computingDetectionBis) {
      canComputeBis = true;
      computingDetectionBis = true;
    }

    LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);


    readyForNextImage();

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP && false) {
      ImageUtils.saveBitmap(croppedBitmap);
    }


    if (canComputeBis) {
      runInBackgroundBis(new Runnable() {
        @Override
        public void run() {
          LOGGER.i("Running detection bis on image " + currTimestamp);
          final long startTime = SystemClock.uptimeMillis();

          List<Classifier.Recognition> results;
          results = detector_bis.recognizeImage(croppedBitmap);

          for (int i = 0; i < results.size(); ++i) {
            if (results.get(i).getConfidence() > MINIMUM_CONFIDENCE_TF_OD_API_BIS) {
              RectF rect = results.get(i).getLocation();
              System.out.println(rect.top + " - " + rect.bottom + " - " + rect.left + " - " + rect.right);
              Bitmap croppedcropBitmap = Bitmap.createBitmap((int)rect.right - (int)rect.left, (int)rect.bottom - (int)rect.top, Config.ARGB_8888);
              new Canvas(croppedcropBitmap).drawBitmap(croppedBitmap, (int)-rect.left, (int)-rect.top, null);
              if (SAVE_PREVIEW_BITMAP) {
                  ImageUtils.saveBitmap(croppedBitmap);
                  ImageUtils.saveBitmap(croppedcropBitmap);
              }
              Bitmap resizedBm = getResizedBitmap(croppedcropBitmap, TF_OD_API_INPUT_SIZE_SIGN, TF_OD_API_INPUT_SIZE_SIGN);
              List<Classifier.Recognition> detectedResults = detector_sign.recognizeImage(resizedBm);
              for (Classifier.Recognition result : detectedResults) {
                if (result.getConfidence() > MINIMUM_CONFIDENCE_TF_OD_API_SIGN) {
                  results.add(new Classifier.Recognition(results.get(i).getId(), result.getTitle(), result.getConfidence(), results.get(i).getLocation()));
                  results.remove(i);
                  break;
                }
              }
            }
          }

          final List<Classifier.Recognition> mappedRecognitions =
                  new LinkedList<Classifier.Recognition>();

          final Paint paint = new Paint();
          paint.setColor(Color.RED);
          paint.setStyle(Style.STROKE);
          paint.setStrokeWidth(2.0f);

          lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

          long sum = 0;
          float fps;

          fpsComputer.add(0, lastProcessingTimeMs);
          fpsComputer.remove(5);

          for (int i = 0; i < 5; ++i) {
            sum += fpsComputer.get(i);
          }

          System.out.println(1.0f / (sum / 5000.0f));

          cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
          final Canvas canvas = new Canvas(cropCopyBitmap);

          float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
          switch (MODE) {
            case TF_OD_API:
              minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
              break;
          }

          for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() > MINIMUM_CONFIDENCE_TF_OD_API_BIS && !result.getTitle().equals("traffic sign")) {
              canvas.drawRect(location, paint);

              cropToFrameTransform.mapRect(location);

              result.setLocation(location);
              mappedRecognitions.add(result);
            }
          }

          tracker.trackResults(mappedRecognitions, currTimestamp);
          trackingOverlay.postInvalidate();

          computingDetectionBis = false;

          runOnUiThread(
                  new Runnable() {
                    @Override
                    public void run() {
                      showFrameInfo(previewWidth + "x" + previewHeight);
                      showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                      showInference(lastProcessingTimeMs + "ms");
                    }
                  });
        }
      });
    }

    if (canCompute) {
      runInBackground(
              new Runnable() {
                @Override
                public void run() {
                  LOGGER.i("Running detection on image " + currTimestamp);
                  final long startTime = SystemClock.uptimeMillis();


                  List<Classifier.Recognition> results;
                  results = detector.recognizeImage(croppedBitmap);

                  //for (int i = 0; i < bufferedResults.size(); ++i) {
                  //  results.add(new Classifier.Recognition(bufferedResults.get(i).getId(), bufferedResults.get(i).getTitle(), bufferedResults.get(i).getConfidence(), bufferedResults.get(i).getLocation()));
                  //}

                  //bufferedResults = results;

                  final List<Classifier.Recognition> mappedRecognitions =
                          new LinkedList<Classifier.Recognition>();

                  final Paint paint = new Paint();
                  paint.setColor(Color.RED);
                  paint.setStyle(Style.STROKE);
                  paint.setStrokeWidth(2.0f);

                  //System.out.println("FPS: " + fps);

                  lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                  long sum = 0;
                  float fps;

                  fpsComputer.add(0, lastProcessingTimeMs);
                  fpsComputer.remove(5);

                  for (int i = 0; i < 5; ++i) {
                    sum += fpsComputer.get(i);
                  }

                  System.out.println(1.0f / (sum / 5000.0f));

                  cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                  final Canvas canvas = new Canvas(cropCopyBitmap);

                  float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                  switch (MODE) {
                    case TF_OD_API:
                      minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                      break;
                  }

                  for (final Classifier.Recognition result : results) {
                    final RectF location = result.getLocation();
                    if (location != null && result.getConfidence() > MINIMUM_CONFIDENCE_TF_OD_API && (result.getTitle().equals("person") || result.getTitle().equals("car"))) {
                      canvas.drawRect(location, paint);

                      cropToFrameTransform.mapRect(location);

                      result.setLocation(location);
                      mappedRecognitions.add(result);
                    }
                  }

                  tracker.trackResults(mappedRecognitions, currTimestamp);
                  trackingOverlay.postInvalidate();

                  computingDetection = false;

                  runOnUiThread(
                          new Runnable() {
                            @Override
                            public void run() {
                              showFrameInfo(previewWidth + "x" + previewHeight);
                              showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                              showInference(lastProcessingTimeMs + "ms");
                            }
                          });
                }
              });
    }
  }

  @Override
  protected int getLayoutId() {
    return R.layout.camera_connection_fragment_tracking;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.
  private enum DetectorMode {
    TF_OD_API;
  }

  @Override
  protected void setUseNNAPI(final boolean isChecked) {
    runInBackground(() -> detector.setUseNNAPI(isChecked));
  }

  @Override
  protected void setNumThreads(final int numThreads) {
    runInBackground(() -> detector.setNumThreads(numThreads));
  }
}
