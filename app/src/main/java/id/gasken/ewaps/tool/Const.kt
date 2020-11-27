package id.gasken.ewaps.tool

import com.google.firebase.Timestamp

object Const {
    val DB_POINTS = "points"
    val TITLE = "title"
    val POSITION = "position"
    val NOTE = "note"
    val LASTUPDATE = "lastUpdate"
    val IMAGEPATH = "imagePath"
    val LATITUDE = "latitude"
    val LONGITUDE = "longitude"

    val currentTimestamp: Timestamp
        get() = Timestamp.now()
}
