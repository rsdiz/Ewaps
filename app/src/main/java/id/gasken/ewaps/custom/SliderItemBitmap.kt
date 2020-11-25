package id.gasken.ewaps.custom

import android.graphics.Bitmap

class SliderItemBitmap {

    private lateinit var bitmapData: Bitmap

    var bitmap: Bitmap
        get() = bitmapData
        set(value) {
            bitmapData = value
        }
}