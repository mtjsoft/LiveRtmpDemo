package cn.mtjsoft.www.livertmpdemo;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.Image;
import android.os.Build;
import android.util.Log;

import java.nio.ByteBuffer;

import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageProxy;

public class ImageUtil {
    public static final int YUV420P = 0;
    public static final int YUV420SP = 1;
    public static final int NV21 = 2;
    private static final String TAG = "ImageUtil";

    /***
     * 此方法内注释以640*480为例
     * 未考虑CropRect的
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static byte[] getBytesFromImageAsType(Image image, int type) {
        try {
            //获取源数据，如果是YUV格式的数据planes.length = 3
            //plane[i]里面的实际数据可能存在byte[].length <= capacity (缓冲区总大小)
            final Image.Plane[] planes = image.getPlanes();

            //数据有效宽度，一般的，图片width <= rowStride，这也是导致byte[].length <= capacity的原因
            // 所以我们只取width部分
            int width = image.getWidth();
            int height = image.getHeight();

            //此处用来装填最终的YUV数据，需要1.5倍的图片大小，因为Y U V 比例为 4:1:1
            byte[] yuvBytes = new byte[width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
            //目标数组的装填到的位置
            int dstIndex = 0;

            //临时存储uv数据的
            byte uBytes[] = new byte[width * height / 4];
            byte vBytes[] = new byte[width * height / 4];
            int uIndex = 0;
            int vIndex = 0;

            int pixelsStride, rowStride;
            for (int i = 0; i < planes.length; i++) {
                pixelsStride = planes[i].getPixelStride();
                rowStride = planes[i].getRowStride();

                ByteBuffer buffer = planes[i].getBuffer();

                //如果pixelsStride==2，一般的Y的buffer长度=640*480，UV的长度=640*480/2-1
                //源数据的索引，y的数据是byte中连续的，u的数据是v向左移以为生成的，两者都是偶数位为有效数据
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);

                int srcIndex = 0;
                if (i == 0) {
                    //直接取出来所有Y的有效区域，也可以存储成一个临时的bytes，到下一步再copy
                    for (int j = 0; j < height; j++) {
                        System.arraycopy(bytes, srcIndex, yuvBytes, dstIndex, width);
                        srcIndex += rowStride;
                        dstIndex += width;
                    }
                } else if (i == 1) {
                    //根据pixelsStride取相应的数据
                    for (int j = 0; j < height / 2; j++) {
                        for (int k = 0; k < width / 2; k++) {
                            uBytes[uIndex++] = bytes[srcIndex];
                            srcIndex += pixelsStride;
                        }
                        if (pixelsStride == 2) {
                            srcIndex += rowStride - width;
                        } else if (pixelsStride == 1) {
                            srcIndex += rowStride - width / 2;
                        }
                    }
                } else if (i == 2) {
                    //根据pixelsStride取相应的数据
                    for (int j = 0; j < height / 2; j++) {
                        for (int k = 0; k < width / 2; k++) {
                            vBytes[vIndex++] = bytes[srcIndex];
                            srcIndex += pixelsStride;
                        }
                        if (pixelsStride == 2) {
                            srcIndex += rowStride - width;
                        } else if (pixelsStride == 1) {
                            srcIndex += rowStride - width / 2;
                        }
                    }
                }
            }

            //根据要求的结果类型进行填充
            switch (type) {
                case YUV420P:
                    System.arraycopy(uBytes, 0, yuvBytes, dstIndex, uBytes.length);
                    System.arraycopy(vBytes, 0, yuvBytes, dstIndex + uBytes.length, vBytes.length);
                    break;
                case YUV420SP:
                    for (int i = 0; i < vBytes.length; i++) {
                        yuvBytes[dstIndex++] = uBytes[i];
                        yuvBytes[dstIndex++] = vBytes[i];
                    }
                    break;
                case NV21:
                    for (int i = 0; i < vBytes.length; i++) {
                        yuvBytes[dstIndex++] = vBytes[i];
                        yuvBytes[dstIndex++] = uBytes[i];
                    }
                    break;
            }
            return yuvBytes;
        } catch (final Exception e) {
            Log.i(TAG, e.toString());
        } finally {
        }
        return null;
    }


    /**
     * YUV_420_888转NV21
     *
     * @param image CameraX ImageProxy
     * @return byte array
     */
    public static byte[] yuv420ToNv21(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        int size = image.getWidth() * image.getHeight();
        byte[] nv21 = new byte[size * 3 / 2];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);

        byte[] u = new byte[uSize];
        uBuffer.get(u);

        //每隔开一位替换V，达到VU交替
        int pos = ySize + 1;
        for (int i = 0; i < uSize; i++) {
            if (i % 2 == 0) {
                nv21[pos] = u[i];
                pos += 2;
            }
        }
        return nv21;
    }

    private byte[] rotation90(byte[] data, int mWidth, int mHeight, int mCameraId) {
        byte[] bytes = new byte[mWidth * mHeight * 3 / 2];
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
        return bytes;
    }

    /***
     * YUV420 转化成 RGB
     */
    public static int[] decodeYUV420SP(byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;
        int rgb[] = new int[frameSize];
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);
                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;
                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
                        | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
        return rgb;
    }

    public static byte[] rotateYUV420Degree270(byte[] data, int imageWidth,
                                               int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int nWidth = 0, nHeight = 0;
        int wh = 0;
        int uvHeight = 0;
        if (imageWidth != nWidth || imageHeight != nHeight) {
            nWidth = imageWidth;
            nHeight = imageHeight;
            wh = imageWidth * imageHeight;
            uvHeight = imageHeight >> 1;// uvHeight = height / 2
        }
        // ??Y
        int k = 0;
        for (int i = 0; i < imageWidth; i++) {
            int nPos = 0;
            for (int j = 0; j < imageHeight; j++) {
                yuv[k] = data[nPos + i];
                k++;
                nPos += imageWidth;
            }
        }
        for (int i = 0; i < imageWidth; i += 2) {
            int nPos = wh;
            for (int j = 0; j < uvHeight; j++) {
                yuv[k] = data[nPos + i];
                yuv[k + 1] = data[nPos + i + 1];
                k += 2;
                nPos += imageWidth;
            }
        }
        return rotateYUV420Degree180(yuv, imageWidth, imageHeight);
    }

    public static byte[] rotateYUV420Degree180(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int i = 0;
        int count = 0;
        for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
            yuv[count] = data[i];
            count++;
        }
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth
                * imageHeight; i -= 2) {
            yuv[count++] = data[i - 1];
            yuv[count++] = data[i];
        }
        return yuv;
    }

    public static byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i--;
            }
        }
        return yuv;
    }


    /**
     * YUV_420_888格式转换成NV21.
     * <p>
     * NV21 格式由一个包含 Y、U 和 V 值的单字节数组组成。
     * 对于大小为 S 的图像，数组的前 S 个位置包含所有 Y 值。其余位置包含交错的 V 和 U 值。
     * U 和 V 在两个维度上都进行了 2 倍的二次采样，因此有 S/4 U 值和 S/4 V 值。
     * 总之，NV21 数组将包含 S 个 Y 值，后跟 S/4 + S/4 VU 值: YYYYYYYYYYYYYY(...)YVUVUVUVU(...)VU
     * <p>
     * YUV_420_888 是一种通用格式，可以描述任何 YUV 图像，其中 U 和 V 在两个维度上都以 2 倍的因子进行二次采样。
     * {@link Image#getPlanes} 返回一个包含 Y、U 和 V 平面的数组
     * Y 平面保证不会交错，因此我们可以将其值复制到 NV21 数组的第一部分。U 和 V 平面可能已经具有 NV21 格式的表示。
     * 如果平面共享相同的缓冲区，则会发生这种情况，V 缓冲区位于 U 缓冲区之前的一个位置，并且平面的 pixelStride 为 2。
     * 如果是这种情况，我们可以将它们复制到 NV21 阵列中。
     */
    public static byte[] yuv420ThreePlanesToNV21(
            Image.Plane[] yuv420888planes, int width, int height) {
        int imageSize = width * height;
        byte[] out = new byte[imageSize + 2 * (imageSize / 4)];

        if (areUVPlanesNV21(yuv420888planes, width, height)) {
            // 复制 Y 的值
            yuv420888planes[0].getBuffer().get(out, 0, imageSize);
            // 从 V 缓冲区获取第一个 V 值，因为 U 缓冲区不包含它。
            yuv420888planes[2].getBuffer().get(out, imageSize, 1);
            // 从 U 缓冲区复制第一个 U 值和剩余的 VU 值。
            yuv420888planes[1].getBuffer().get(out, imageSize + 1, 2 * imageSize / 4 - 1);
        } else {
            // 回退到一个一个地复制 UV 值，这更慢但也有效。
            // 取 Y.
            unpackPlane(yuv420888planes[0], width, height, out, 0, 1);
            // 取 U.
            unpackPlane(yuv420888planes[1], width, height, out, imageSize + 1, 2);
            // 取 V.
            unpackPlane(yuv420888planes[2], width, height, out, imageSize, 2);
        }
        return out;
    }

    /**
     * 检查 YUV_420_888 图像的 UV 平面缓冲区是否为 NV21 格式。
     */
    private static boolean areUVPlanesNV21(Image.Plane[] planes, int width, int height) {
        int imageSize = width * height;

        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        // 备份缓冲区属性。
        int vBufferPosition = vBuffer.position();
        int uBufferLimit = uBuffer.limit();

        // 将 V 缓冲区推进 1 个字节，因为 U 缓冲区将不包含第一个 V 值。
        vBuffer.position(vBufferPosition + 1);
        // 切掉 U 缓冲区的最后一个字节，因为 V 缓冲区将不包含最后一个 U 值。
        uBuffer.limit(uBufferLimit - 1);

        // 检查缓冲区是否相等并具有预期的元素数量。
        boolean areNV21 = (vBuffer.remaining() == (2 * imageSize / 4 - 2)) && (vBuffer.compareTo(uBuffer) == 0);

        // 将缓冲区恢复到初始状态。
        vBuffer.position(vBufferPosition);
        uBuffer.limit(uBufferLimit);

        return areNV21;
    }

    /**
     * 将图像平面解压缩为字节数组。
     * <p>
     * 输入平面数据将被复制到“out”中，从“offset”开始，每个像素将被“pixelStride”隔开。 请注意，输出上没有行填充。
     */
    private static void unpackPlane(Image.Plane plane, int width, int height, byte[] out, int offset, int pixelStride) {
        ByteBuffer buffer = plane.getBuffer();
        buffer.rewind();

        // 计算当前平面的大小。假设它的纵横比与原始图像相同。
        int numRow = (buffer.limit() + plane.getRowStride() - 1) / plane.getRowStride();
        if (numRow == 0) {
            return;
        }
        int scaleFactor = height / numRow;
        int numCol = width / scaleFactor;

        // 提取输出缓冲区中的数据。
        int outputPos = offset;
        int rowStart = 0;
        for (int row = 0; row < numRow; row++) {
            int inputPos = rowStart;
            for (int col = 0; col < numCol; col++) {
                out[outputPos] = buffer.get(inputPos);
                outputPos += pixelStride;
                inputPos += plane.getPixelStride();
            }
            rowStart += plane.getRowStride();
        }
    }
}
