package id.gasken.ewaps.custom

class SliderItem() {

    private var image: Int = 0

    constructor(image: Int) : this() {
        this.image = image
    }

    public fun getImage(): Int{

        return this.image
    }

}