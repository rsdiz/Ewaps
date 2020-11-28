package id.gasken.ewaps.job

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import id.gasken.ewaps.data.AccelerometerData
import id.gasken.ewaps.databinding.ActivitySettingBinding
import id.gasken.ewaps.ui.SettingActivity

class SpeedMeterJobService: JobService() {

    private val TAG = "SpeedMeterJobService"
    var jobCancelled = false

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Job Started")

        doBackgroundWork(params)

        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Job cancelled before completion")
        jobCancelled = true
        return true
    }

    private fun doBackgroundWork(params: JobParameters?) {

        Log.d(TAG, "onCreate init Sensor Service")
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorManager.registerListener(
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (jobCancelled) {
                        return
                    }
                    AccelerometerData.X_AXIS = event!!.values[0]
                    AccelerometerData.Y_AXIS = event.values[1]
                    AccelerometerData.Z_AXIS = event.values[2]

                    Log.d(TAG, "Accelerometer X: ${event.values[0]}, Y: ${event.values[1]}, Z: ${event.values[2]}, state: $jobCancelled")
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
//                                TODO("Not yet implemented")
                }
            },
            accelerometer, SensorManager.SENSOR_DELAY_NORMAL
        )

//        val thread = Thread(
//            Runnable {
//                kotlin.run {
//
//                    if(jobCancelled){
//                        return@run
//                    }
//
//                    sensorManager.registerListener(
//                        object : SensorEventListener {
//                            override fun onSensorChanged(event: SensorEvent?) {
//                                Log.d(TAG, "Accelerometer X: ${event!!.values[0]}, Y: ${event.values[1]}, Z: ${event.values[2]}, state: $jobCancelled")
//                            }
//
//                            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
// //                                TODO("Not yet implemented")
//                            }
//                        },
//                        accelerometer, SensorManager.SENSOR_DELAY_NORMAL
//                    )
//
//                    Log.d(TAG, "Job Finished")
//                    jobFinished(params, false)
//                }
//            }
//        )
//
//        if (jobCancelled) {
//            thread.interrupt()
//            return
//        } else {
//            thread.start()
//        }
    }
}
