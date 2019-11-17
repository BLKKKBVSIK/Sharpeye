/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package sharpeye.sharpeye.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import java.util.LinkedList;
import java.util.List;

/** A simple View providing a render callback to other classes. */
public class OverlayView extends View {
  private final List<DrawCallback> callbacks = new LinkedList<DrawCallback>();

  private int ratioWidth = 0;
  private int ratioHeight = 0;

  public OverlayView(final Context context, final AttributeSet attrs) {
    super(context, attrs);
  }

  public void addCallback(final DrawCallback callback) {
    callbacks.add(callback);
  }

  /**
   * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
   * calculated from the parameters. Note that the actual sizes of parameters don't matter, that is,
   * calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
   *
   * @param width Relative horizontal size
   * @param height Relative vertical size
   */
  public void setAspectRatio(final int width, final int height) {
    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Size cannot be negative.");
    }
    ratioWidth = width;
    ratioHeight = height;
    requestLayout();
  }

  @Override
  public synchronized void draw(final Canvas canvas) {
    for (final DrawCallback callback : callbacks) {
      callback.drawCallback(canvas);
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    final int width = MeasureSpec.getSize(widthMeasureSpec);
    final int height = MeasureSpec.getSize(heightMeasureSpec);
    if (0 == ratioWidth || 0 == ratioHeight) {
      setMeasuredDimension(width, height);
    } else if (width < height * ratioWidth / ratioHeight) {
      setMeasuredDimension(height * ratioWidth / ratioHeight, height);
      setTranslationX((height * ratioWidth / ratioHeight - height)/2);
    } else {
      setMeasuredDimension(width, width * ratioHeight / ratioWidth);
      setTranslationY((width * ratioHeight / ratioWidth - width)/2);
    }
  }

  /** Interface defining the callback for client classes. */
  public interface DrawCallback {
    public void drawCallback(final Canvas canvas);
  }
}
