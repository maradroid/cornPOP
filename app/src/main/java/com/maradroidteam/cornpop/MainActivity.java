package com.maradroidteam.cornpop;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;


public class MainActivity extends Activity {

    Button record;
    SeekBar seekBar, alarmSB, sampleSB;
    TextView thold,count,alarm_tv, sample_tv;
    int THolder = 10000;
    int counter = 0;
    int alarm = 0;
    int sample = 100;
    boolean counting = false;
    private MediaRecorder mRecorder = null;
    private static String mFileName = null;
    MediaPlayer buzzer = null;

    private static final String LOG_TAG = "AudioRecordTest";

    Thread nit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        record = (Button) findViewById(R.id.record_btn);
        seekBar = (SeekBar) findViewById(R.id.thold_sb);
        alarmSB = (SeekBar) findViewById(R.id.alarm_sb);
        sampleSB = (SeekBar) findViewById(R.id.sample_sb);
        thold = (TextView) findViewById(R.id.thold_tv);
        count = (TextView) findViewById(R.id.count_tv);
        alarm_tv = (TextView) findViewById(R.id.alarm_tv);
        sample_tv = (TextView) findViewById(R.id.sample_tv);

        count.setText(""+counter);
        thold.setText("Osjetljivost: 1");
        record.setText("Start");
        alarm_tv.setText("Alarm: Off");
        sample_tv.setText("Vrijeme uzorkovanja: 0.1s");

        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/audiorecordtest.3gp";

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar.getProgress() == 0) {
                    thold.setText("Osjetljivost 1");
                }else if(seekBar.getProgress() == 1){
                    thold.setText("Osjetljivost 2");
                }else if(seekBar.getProgress() == 2){
                    thold.setText("Osjetljivost 3");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                THolder = (seekBar.getProgress()+1)* 10000;
                Log.e("efs",""+THolder);
            }
        });

        alarmSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar.getProgress() == 0) {
                    alarm_tv.setText("Alarm: Off");
                }else{
                    alarm_tv.setText("Alarm at " + seekBar.getProgress());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                alarm = seekBar.getProgress();
            }
        });

        sampleSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sample_tv.setText("Vrijeme uzorkovanja: 0."+ (seekBar.getProgress()+1)+"s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sample = (seekBar.getProgress()+1)*100;
                Log.e("sample",""+sample);
            }
        });
    }

    public void Record(View v){

        if(counting == false && mRecorder == null){
            counting = true;
            seekBar.setEnabled(false);
            alarmSB.setEnabled(false);
            sampleSB.setEnabled(false);
            counter = 0;
            count.setText("" + counter);
            record.setText("Stop");

            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setOutputFile(mFileName);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                mRecorder.prepare();
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed");
            }

            nit = new Thread(new Runnable() {
                @Override
                public void run() {
                    mRecorder.start();
                    int startAmplitude = mRecorder.getMaxAmplitude();
                    int finishAmplitude = 0;
                    Log.d(LOG_TAG, "starting amplitude: " + startAmplitude);

                    while (counting)
                    {
                        //Log.d(TAG, "waiting while recording...");
                        finishAmplitude = mRecorder.getMaxAmplitude();


                        int ampDifference = finishAmplitude - startAmplitude;

                        if (ampDifference >= THolder)
                        {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    counter++;
                                    count.setText(""+counter);
                                    if (counter >= alarm && alarm!=0){

                                        if(buzzer==null){
                                            buzzer = MediaPlayer.create(MainActivity.this, R.raw.error);
                                            buzzer.start();
                                            buzzer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                                @Override
                                                public void onCompletion(MediaPlayer mp) {
                                                    buzzer.stop();
                                                    buzzer.release();
                                                    buzzer = null;
                                                    Log.e("sdfs","buzzer terminated");
                                                }
                                            });
                                        }
                                        record.setText("Start");
                                        seekBar.setEnabled(true);
                                        alarmSB.setEnabled(true);
                                        sampleSB.setEnabled(true);
                                        counting = false;
                                    }
                                }
                            });
                        }


                        try {
                            synchronized (this) {
                                wait(sample);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(LOG_TAG, "finishing amplitude: " + finishAmplitude + " diff: "
                                + ampDifference);
                    }

                        mRecorder.stop();
                        mRecorder.release();
                        mRecorder = null;

                }
            });
            nit.start();


        }else{
            counting = false;
            seekBar.setEnabled(true);
            alarmSB.setEnabled(true);
            sampleSB.setEnabled(true);
            record.setText("Start");
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        counting = false;
        record.setText("Start");

        //if(nit.isAlive()) nit.interrupt();

        /*if(buzzer!=null){
            buzzer.stop();
            buzzer.release();
            buzzer = null;
        }

        if(mRecorder!=null){
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
            record.setText("Start");
        }*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        counting = false;

       /* if(buzzer!=null){
            buzzer.stop();
            buzzer.release();
            buzzer = null;
        }

        if(mRecorder!=null){
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }*/
    }
}
