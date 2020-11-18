package id.gasken.ewaps.data

object TabLayout {

    private var homeStateVal = false

    private var mapsStateVal = false

    private var userInputStateVal = false

    var homeState: Boolean = false
        set(value) {
            homeStateVal = value
            field = value
        }
        get() = this.homeStateVal

    var mapsState: Boolean = false
        set(value) {
            mapsStateVal = value
            field = value
        }
        get() = this.mapsStateVal

    var userInputState: Boolean = false
        set(value) {
            userInputStateVal = value
            field = value
        }
        get() = this.userInputStateVal
}
