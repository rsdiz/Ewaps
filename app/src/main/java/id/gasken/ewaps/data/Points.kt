package id.gasken.ewaps.data

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import id.gasken.ewaps.tool.Const
import kotlinx.parcelize.Parcelize

@Parcelize
data class Points(
    var title: String = "",
    var position: LatLng = LatLng(0.0, 0.0),
    var note: String = "",
    var imagePath: String = "",
    var videoPath: String = "",
    var lastUpdate: Timestamp = Const.currentTimestamp
) : Parcelable
