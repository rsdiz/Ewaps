package id.gasken.ewaps.custom

import android.graphics.Bitmap
import android.util.Log

class ImageResizer {

     fun reduceBitmapSize(bitmap: Bitmap, height: Int, width: Int): Bitmap{
        val bitmapHeight = bitmap.height
        val bitmapWidth = bitmap.width

        val ratioSquare: Double = ((bitmapHeight * bitmapWidth) / (height * width)).toDouble()

        if (ratioSquare <= 1){
            return bitmap
        }

        val ratio = Math.sqrt(ratioSquare)

        Log.d("ratio", "Ratio: " + ratio)

        val requireHeight = Math.round(bitmapHeight / ratio).toInt()
        val requireWidth = Math.round(bitmapWidth / ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, requireWidth, requireHeight, true)
    }
}