package com.datpug;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.datpug.R;

public class InternalSensorActivity extends AppCompatActivity implements SensorEventListener {
    private TextView lightVal;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_internal_sensor);
        SensorManager sensorManager;
        Sensor envSensor;

        lightVal = findViewById(R.id.lightValue);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        envSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if(envSensor == null)
            Toast.makeText(getApplicationContext(), "Your device doesn't have a light sensor", Toast.LENGTH_SHORT).show();
        else
            sensorManager.registerListener(this, envSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float sensorValue = sensorEvent.values[0];
        String lightInfo = sensorValue + " SI lux units";
        lightVal.setText(lightInfo);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        String accuracyMsg = "";
        switch(accuracy){
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                accuracyMsg="Sensor has high accuracy";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                accuracyMsg="Sensor has medium accuracy";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                accuracyMsg="Sensor has low accuracy";
                break;
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                accuracyMsg="Sensor has unreliable accuracy";
                break;
            default:
                break;
        }

        Toast.makeText(getApplicationContext(), accuracyMsg, Toast.LENGTH_SHORT).show();

    }
}
