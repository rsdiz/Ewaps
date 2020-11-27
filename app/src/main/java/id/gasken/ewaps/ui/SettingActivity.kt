package id.gasken.ewaps.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import id.gasken.ewaps.R
import id.gasken.ewaps.databinding.ActivitySettingBinding
import maes.tech.intentanim.CustomIntent

class SettingActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener{

    private lateinit var drawerLayout: DrawerLayout

    private lateinit var navView: NavigationView

    private lateinit var binding: ActivitySettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivitySettingBinding.inflate(layoutInflater)

        setContentView(binding.root)

        drawerLayout = binding.drawerLayout

        navView = binding.navView


        navView.setNavigationItemSelectedListener(this)

        navView.menu.getItem(2).isChecked = true

    }
//


    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.nav_home -> {
//                Home / Maps Activity
            }
            R.id.nav_feedback -> {
                startActivity(Intent(this, UserInputActivity::class.java))
                CustomIntent.customType(this, "left-to-right")
            }
            R.id.nav_settings -> {
//                Settings Activity
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)

        return true
    }



    override fun onBackPressed() {

        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        else{
            super.onBackPressed()
        }

    }

    override fun onResume() {
        navView.menu.getItem(2).isChecked = true
        super.onResume()
    }

    override fun finish() {

        super.finish()
        CustomIntent.customType(this, "right-to-left")
    }

}