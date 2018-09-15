package com.xu.servicequalityrater;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.helper.ItemTouchHelper;
import java.io.IOException;
import java.io.InputStream;

//Class designed to help process the image
public class ImageHelper {
    private static final double FACE_RECT_SCALE_RATIO = 1.3d;
    private static final int IMAGE_MAX_SIDE_LENGTH = 1280;

    public static Bitmap loadSizeLimitedBitmapFromUri(Uri imageUri, ContentResolver contentResolver) {
        try {
            InputStream imageInputStream = contentResolver.openInputStream(imageUri);
            Options options = new Options();
            options.inJustDecodeBounds = true;
            Rect outPadding = new Rect();
            BitmapFactory.decodeStream(imageInputStream, outPadding, options);
            int maxSideLength = options.outWidth > options.outHeight ? options.outWidth : options.outHeight;
            options.inSampleSize = 1;
            options.inSampleSize = calculateSampleSize(maxSideLength, IMAGE_MAX_SIDE_LENGTH);
            options.inJustDecodeBounds = false;
            imageInputStream.close();
            Bitmap bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri), outPadding, options);
            double ratio = 1280.0d / ((double) (bitmap.getWidth() > bitmap.getHeight() ? bitmap.getWidth() : bitmap.getHeight()));
            if (ratio < 1.0d) {
                bitmap = Bitmap.createScaledBitmap(bitmap, (int) (((double) bitmap.getWidth()) * ratio), (int) (((double) bitmap.getHeight()) * ratio), false);
            }
            return rotateBitmap(bitmap, getImageRotationAngle(imageUri, contentResolver));
        } catch (Exception e) {
            return null;
        }
    }

    private static int calculateSampleSize(int maxSideLength, int expectedMaxImageSideLength) {
        int inSampleSize = 1;
        while (maxSideLength > expectedMaxImageSideLength * 2) {
            maxSideLength /= 2;
            inSampleSize *= 2;
        }
        return inSampleSize;
    }

    private static int getImageRotationAngle(Uri imageUri, ContentResolver contentResolver) throws IOException {
        int angle = 0;
        Cursor cursor = contentResolver.query(imageUri, new String[]{"orientation"}, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() == 1) {
                cursor.moveToFirst();
                angle = cursor.getInt(0);
            }
            cursor.close();

        }else {
            ExifInterface exif = new ExifInterface(imageUri.getPath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_180:
                    angle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    angle = 270;
                    break;
                default:
                    break;
            }
        }
        return angle;
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int angle) {
        if (angle == 0) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate((float) angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
