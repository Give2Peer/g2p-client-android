package org.give2peer.karma.factory;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import java.io.File;


/**
 * A wrapper for `android.graphics.BitmapFactory` to allow us to generate size-constrained
 * thumbnails with the proper orientation.
 */
public class BitmapFactory
{

    /**
     * Generate a Bitmap object from the picture file at `filepath`, whose dimensions have been
     * constrained to `maxWidth` and `maxHeight`, which MUST be positive integers. No zeroes.
     *
     * Right now, you MUST provide both constraints. If you need more flexibility, hack away !
     *
     * Also corrects the orientation of the Bitmap.
     * Orientation, depending of the device, may not be correctly set in the EXIF data of the taken
     * picture when it is saved into disk. We try to compensate that, so far so good.
     *
     * This will probably require a lot more work to support other devices.
     *
     * Explanation:
     * 	Camera orientation is not working ok (as is when capturing an image) because OEMs do not
     * 	adhere to a standard. So, each company does this their own way. Hell ensues.
     *
     * @param filepath	filepath to the file
     */
    public static Bitmap getThumbBitmap(String filepath, int maxWidth, int maxHeight)
    {
        Bitmap bitmap = null;

        try {

            File f = new File(filepath);
            //FileInputStream fis = new FileInputStream(f);

            ExifInterface exif = new ExifInterface(f.getPath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            int rotate = 0;
            switch(orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate += 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate += 90;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate += 90;
            }
            Matrix mat = new Matrix();
            mat.postRotate(rotate);

            Bitmap bmp = decodeSampledBitmapFromFile(filepath, maxWidth, maxHeight);

            if (null == bmp) {
                throw new Exception("Could not decode the bitmap stream.");
            }
            bitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), mat, true);

        } catch (OutOfMemoryError e) {
            Log.e("G2P", "getThumbBitmap('"+filepath+"') [OutOfMemory!]: " + e.getMessage(), e);
        } catch (Throwable e) {
            Log.e("G2P","getThumbBitmap('"+filepath+"'): " + e.getMessage(), e);
        }

        return bitmap;
    }

    public static int calculateInSampleSize(int inWidth, int inHeight, int outWidth, int outHeight)
    {
        int inSampleSize = 1;

        if (inHeight > outHeight || inWidth > outWidth) {

            final int halfHeight = inHeight / 2;
            final int halfWidth  = inWidth  / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > outHeight &&
                    (halfWidth / inSampleSize) > outWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromFile(String pathname, int reqWidth, int reqHeight)
    {
        // First decode with inJustDecodeBounds=true to check dimensions
        final android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeFile(pathname, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight,
                reqWidth,         reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return android.graphics.BitmapFactory.decodeFile(pathname, options);
    }

}
