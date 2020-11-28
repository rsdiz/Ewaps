package id.gasken.ewaps.ui

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import id.gasken.ewaps.R
import id.gasken.ewaps.data.AccelerometerData
import id.gasken.ewaps.data.JobId
import id.gasken.ewaps.data.SettingsConfig
import id.gasken.ewaps.databinding.ActivitySettingBinding
import id.gasken.ewaps.job.SpeedMeterJobService
import id.gasken.ewaps.tool.viewBinding
import maes.tech.intentanim.CustomIntent

class SettingActivity :
    AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private var TAG = "SettingActivity"

    private lateinit var drawerLayout: DrawerLayout

    private lateinit var navView: NavigationView

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var speedMeterSwitch: Switch

    private val binding: ActivitySettingBinding by viewBinding()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        drawerLayout = binding.drawerLayout
        navView = binding.navView
        speedMeterSwitch = binding.speedMeterSwitch

        speedMeterSwitch.isChecked = SettingsConfig.speedMeterEnabled

        speedMeterSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                SettingsConfig.speedMeterEnabled = isChecked
                scheduleJob()
                Log.d(TAG, "============= $isChecked")
            } else {
                cancelJob()
                SettingsConfig.speedMeterEnabled = isChecked
                Log.d(TAG, "------------- $isChecked")
            }
        }

        navView.setNavigationItemSelectedListener(this)
        navView.menu.getItem(2).isChecked = true
    }

    private fun scheduleJob() {
        val componentName = ComponentName(this, SpeedMeterJobService::class.java) as ComponentName
        val info = JobInfo.Builder(JobId.speedMeterId, componentName)
            .setRequiresCharging(false)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
            .setPersisted(false)
            .setPeriodic(15 * 60 * 1000)
            .build()

        val scheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        val resultCode = scheduler.schedule(info) as Int

        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job Scheduled")
        } else {
            Log.d(TAG, "Job Scheduling failed")
        }
    }

    private fun cancelJob() {
        val scheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        scheduler.cancel(JobId.speedMeterId)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                startActivity(Intent(this, ViewMapsActivity::class.java))
                CustomIntent.customType(this, "left-to-right")
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

        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
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
