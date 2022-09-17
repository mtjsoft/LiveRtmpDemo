package com.frank.live.stream;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.frank.live.listener.CameraListener;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class CameraHelper implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private Activity mActivity;
    private int mHeight;
    private int mWidth;
    private int mCameraId;
    private Camera mCamera;
    private byte[] buffer;
    private SurfaceHolder mSurfaceHolder;
    private Camera.PreviewCallback mPreviewCallback;
    private int mRotation;
    private OnChangedSizeListener mOnChangedSizeListener;
    private byte[] bytes;
    /**
     * 事件回调
     */
    private CameraListener cameraListener;
    private int displayOrientation;
    private Camera.Size previewSize;
    /**
     * 额外的旋转角度（用于适配一些定制设备）
     */
    private int additionalRotation;
    /**
     * 屏幕的长宽，在选择最佳相机比例时用到
     */
    private Point previewViewSize;
    private Point specificPreviewSize;

    CameraHelper(Activity activity, int cameraId, int width, int height) {
        mActivity = activity;
        mCameraId = cameraId;
        mWidth = width;
        mHeight = height;
        previewViewSize = new Point(width,height);
    }

    void switchCamera() {
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        stopPreview();
        startPreview();
    }

    private void stopPreview() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            if (cameraListener != null) {
                cameraListener.onCameraClosed();
            }
        }
    }

    private void startPreview() {
        try {
            mCamera = Camera.open(mCameraId);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);
            setPreviewSize(parameters);
            displayOrientation = setPreviewOrientation(parameters);
            mCamera.setParameters(parameters);
            buffer = new byte[mWidth * mHeight * 3 / 2];
            bytes = new byte[buffer.length];
            mCamera.addCallbackBuffer(buffer);
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
            if (cameraListener != null) {
                cameraListener.onCameraOpened(mCamera, mCameraId, displayOrientation, false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (cameraListener != null) {
                cameraListener.onCameraError(ex);
            }
        }
    }

    public void setCameraListener(CameraListener cameraListener) {
        this.cameraListener = cameraListener;
    }

    private int setPreviewOrientation(Camera.Parameters parameters) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        mRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (mRotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                mOnChangedSizeListener.onChanged(mHeight, mWidth);
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                mOnChangedSizeListener.onChanged(mWidth, mHeight);
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                mOnChangedSizeListener.onChanged(mWidth, mHeight);
                break;
        }
        additionalRotation /= 90;
        additionalRotation *= 90;
        degrees += additionalRotation;
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
        return result;
    }

    private void setPreviewSize(Camera.Parameters parameters) {
//        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
//        Camera.Size size = supportedPreviewSizes.get(0);
//        //select the best resolution of camera
//        int m = Math.abs(size.height * size.width - mWidth * mHeight);
//        supportedPreviewSizes.remove(0);
//        for (Camera.Size next : supportedPreviewSizes) {
//            int n = Math.abs(next.height * next.width - mWidth * mHeight);
//            if (n < m) {
//                m = n;
//                size = next;
//            }
//        }
//        mWidth = size.width;
//        mHeight = size.height;
//        parameters.setPreviewSize(mWidth, mHeight);

        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        if (supportedPreviewSizes != null && supportedPreviewSizes.size() > 0) {
            previewSize = getBestSupportedSize(supportedPreviewSizes, previewViewSize);
        }
        mWidth = previewSize.width;
        mHeight = previewSize.height;
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        //对焦模式设置
        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.size() > 0) {
            if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
        }
    }

    private Camera.Size getBestSupportedSize(List<Camera.Size> sizes, Point previewViewSize) {
        if (sizes == null || sizes.size() == 0) {
            return mCamera.getParameters().getPreviewSize();
        }
        Camera.Size[] tempSizes = sizes.toArray(new Camera.Size[0]);
        Arrays.sort(tempSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size o1, Camera.Size o2) {
                if (o1.width > o2.width) {
                    return -1;
                } else if (o1.width == o2.width) {
                    return o1.height > o2.height ? -1 : 1;
                } else {
                    return 1;
                }
            }
        });
        sizes = Arrays.asList(tempSizes);

        Camera.Size bestSize = sizes.get(0);
        float previewViewRatio;
        if (previewViewSize != null) {
            previewViewRatio = (float) previewViewSize.x / (float) previewViewSize.y;
        } else {
            previewViewRatio = (float) bestSize.width / (float) bestSize.height;
        }

        if (previewViewRatio > 1) {
            previewViewRatio = 1 / previewViewRatio;
        }
        boolean isNormalRotate = (additionalRotation % 180 == 0);

        for (Camera.Size s : sizes) {
            if (specificPreviewSize != null && specificPreviewSize.x == s.width && specificPreviewSize.y == s.height) {
                return s;
            }
            if (isNormalRotate) {
                if (Math.abs((s.height / (float) s.width) - previewViewRatio) < Math.abs(bestSize.height / (float) bestSize.width - previewViewRatio)) {
                    bestSize = s;
                }
            } else {
                if (Math.abs((s.width / (float) s.height) - previewViewRatio) < Math.abs(bestSize.width / (float) bestSize.height - previewViewRatio)) {
                    bestSize = s;
                }
            }
        }
        return bestSize;
    }


    void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
        mSurfaceHolder.addCallback(this);
    }

    void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        stopPreview();
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (cameraListener != null) {
            cameraListener.onPreview(data, camera);
        }
        switch (mRotation) {
            case Surface.ROTATION_0:
                rotation90(data);
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
            default:
                break;
        }
        mPreviewCallback.onPreviewFrame(bytes, camera);
        camera.addCallbackBuffer(buffer);
    }

    private void rotation90(byte[] data) {
        int index = 0;
        int ySize = mWidth * mHeight;
        int uvHeight = mHeight / 2;
        //back camera rotate 90 deree
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {

            for (int i = 0; i < mWidth; i++) {
                for (int j = mHeight - 1; j >= 0; j--) {
                    bytes[index++] = data[mWidth * j + i];
                }
            }

            for (int i = 0; i < mWidth; i += 2) {
                for (int j = uvHeight - 1; j >= 0; j--) {
                    // v
                    bytes[index++] = data[ySize + mWidth * j + i];
                    // u
                    bytes[index++] = data[ySize + mWidth * j + i + 1];
                }
            }
        } else {
            //rotate 90 degree
            for (int i = 0; i < mWidth; i++) {
                int nPos = mWidth - 1;
                for (int j = 0; j < mHeight; j++) {
                    bytes[index++] = data[nPos - i];
                    nPos += mWidth;
                }
            }
            //u v
            for (int i = 0; i < mWidth; i += 2) {
                int nPos = ySize + mWidth - 1;
                for (int j = 0; j < uvHeight; j++) {
                    bytes[index++] = data[nPos - i - 1];
                    bytes[index++] = data[nPos - i];
                    nPos += mWidth;
                }
            }
        }
    }


    void setOnChangedSizeListener(OnChangedSizeListener listener) {
        mOnChangedSizeListener = listener;
    }

    public void release() {
        mSurfaceHolder.removeCallback(this);
        stopPreview();
    }

    public interface OnChangedSizeListener {
        void onChanged(int w, int h);
    }
}
