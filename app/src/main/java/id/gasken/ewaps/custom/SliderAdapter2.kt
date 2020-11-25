package id.gasken.ewaps.custom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewpager2.widget.ViewPager2
import com.makeramen.roundedimageview.RoundedImageView
import id.gasken.ewaps.R
import javax.annotation.Nonnull

internal class SliderAdapter2(private val sliderItemsBitmap: List<SliderItemBitmap>, val viewPager2: ViewPager2) :
    RecyclerView.Adapter<SliderAdapter2.SliderViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        return SliderViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.slide_item_container,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        holder.setImage(sliderItemsBitmap[position])
    }

    override fun getItemCount(): Int {
        return sliderItemsBitmap.size
    }

    internal inner class SliderViewHolder(@Nonnull itemView: View) :
        ViewHolder(itemView) {
        private val imageView: RoundedImageView
        fun setImage(sliderItemsBitmap: SliderItemBitmap) {
            imageView.setImageBitmap(sliderItemsBitmap.bitmap)
        }

        init {
            imageView = itemView.findViewById(R.id.imageSlide)

        }
    }
}