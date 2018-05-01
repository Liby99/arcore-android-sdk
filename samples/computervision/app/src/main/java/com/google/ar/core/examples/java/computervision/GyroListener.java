package com.google.ar.core.examples.java.computervision;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class GyroListener extends Activity implements SensorEventListener {

    public SensorManager mSensorManager;
    public Sensor mAccelerometer;
    public Sensor mGyroscope;
    public Sensor mRotationSensor;

    private final float[] mAccelerometerReading = new float[3];
    //private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    private final float[] mRotationMatrix = new float[16];

    public void startGyro(){
        //mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        //mSensorManager.registerListener(this, Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mRotationSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stopGyro(){
        mSensorManager.unregisterListener(this);
        //Log.d("s", "stopped");
    }

    public void updateOrientationAngles(){
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor == mRotationSensor)
        {
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, sensorEvent.values);

            float[] tempRotationMatrix = new float[16];
            SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, tempRotationMatrix);

            float[] orientations = new float[3];
            SensorManager.getOrientation(tempRotationMatrix, orientations);
            float y = (float)Math.toDegrees(orientations[0]);
            float x = (float)Math.toDegrees(orientations[1]);
            float z = (float)Math.toDegrees(orientations[2]);

            Log.e("!", String.valueOf(x) + " " + String.valueOf(y) + " " + String.valueOf(z));

        }
//        if(sensorEvent.sensor == mAccelerometer)
//        {
//            System.arraycopy(sensorEvent.values, 0, mAccelerometerReading, 0, mAccelerometerReading.length);
//            //Log.e("A", String.valueOf(mAccelerometerReading[0]));
//            //float x = sensorEvent.values[0];
//            //float y = sensorEvent.values[1];
//            //float z = sensorEvent.values[2];
//            //int sth = sensorEvent.values.length;
//            //Log.e("A", String.valueOf(x) + " " + String.valueOf(y) + " " + String.valueOf(z) + " " + String.valueOf(sth));
//        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
