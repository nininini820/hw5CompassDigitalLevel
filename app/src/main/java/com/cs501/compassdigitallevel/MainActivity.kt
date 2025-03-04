package com.cs501.compassdigitallevel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private var azimuth by mutableStateOf(0f)
    private var roll by mutableStateOf(0f)
    private var pitch by mutableStateOf(0f)

    private val accelerometerData = FloatArray(3)
    private val magnetometerData = FloatArray(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        setContent {
            CompassLevelApp(azimuth, roll, pitch)
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private var lastTimestamp: Long = 0L

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_GYROSCOPE -> {
                    val timestamp = event.timestamp

                    val dt = if (lastTimestamp == 0L) 0f else (timestamp - lastTimestamp) * 1.0f / 1_000_000_000.0f
                    lastTimestamp = timestamp

                    if (dt > 0) {
                        roll += Math.toDegrees(it.values[0].toDouble() * dt.toDouble()).toFloat()
                        pitch += Math.toDegrees(it.values[1].toDouble() * dt.toDouble()).toFloat()


                    }
                }

                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(it.values, 0, accelerometerData, 0, it.values.size)
                }

                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(it.values, 0, magnetometerData, 0, it.values.size)
                }
            }

            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)
            if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerData, magnetometerData)) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

                val rotation = windowManager.defaultDisplay.rotation
                azimuth = adjustAzimuthForRotation(azimuth, rotation)
            }
        }
    }


    fun adjustAzimuthForRotation(azimuth: Float, rotation: Int): Float {
        return when (rotation) {
            Surface.ROTATION_0 -> azimuth
            Surface.ROTATION_90 -> azimuth + 90
            Surface.ROTATION_180 -> azimuth + 180
            Surface.ROTATION_270 -> azimuth + 270
            else -> azimuth
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}


@Composable
fun CompassLevelApp(azimuth: Float, roll: Float, pitch: Float) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        CompassScreen(azimuth)
        Spacer(modifier = Modifier.height(16.dp))

        DigitalLevelScreen(roll, pitch)
    }
}


@Composable
fun CompassScreen(azimuth: Float) {
    Box(
        modifier = Modifier
            .size(300.dp)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img),
            contentDescription = "Compass Needle",
            modifier = Modifier.graphicsLayer(rotationZ = -azimuth)
        )
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "Heading: ${azimuth.toInt()}° ${getDirectionLabel(azimuth)}",
            color = Color.Black,
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}


fun getDirectionLabel(azimuth: Float): String {
    return when {
        azimuth in 337.5..360.0 || azimuth in 0.0..22.5 -> "N"
        azimuth in 22.5..67.5 -> "NE"
        azimuth in 67.5..112.5 -> "E"
        azimuth in 112.5..157.5 -> "SE"
        azimuth in 157.5..202.5 -> "S"
        azimuth in 202.5..247.5 -> "SW"
        azimuth in 247.5..292.5 -> "W"
        azimuth in 292.5..337.5 -> "NW"
        else -> ""
    }
}


@Composable
fun DigitalLevelScreen(roll: Float, pitch: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Gray)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Roll: ${roll.toInt()}°", color = Color.White, fontSize = 20.sp)
        Text(text = "Pitch: ${pitch.toInt()}°", color = Color.White, fontSize = 20.sp)
    }
}