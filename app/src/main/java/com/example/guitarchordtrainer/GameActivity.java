package com.example.guitarchordtrainer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.jtransforms.fft.FloatFFT_1D;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    ArrayList<Chord> chords;
    ArrayList<Chord> selectedChords;
    String previousRandomChord = null; //used to prevent duplicates
    Random random = new Random();
    int transitions = 0;

    private static final int sampleRate = 44100; //guaranteed to be supported
    private static final int channels = AudioFormat.CHANNEL_IN_MONO;
    private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private static final int bufferSizeInBytes = 8192; //2^13 so 2^12 samples per buffer

    private AudioRecord recorder = null;
    private Thread recordingThread = null;

    private float[] pcp1 = new float[12];
    private float[] pcp2 = new float[12];
    private float[] pcp3 = new float[12];
    private float[] avgpcp = new float[12];
    private float[] prevpcp = new float[12];
    private int currentpcp = 0; //keep track of which pcp array we are updating

    private final double silenceThreshold = 150; //will this work on all devices?

    private CircularDataStructure results = new CircularDataStructure(2); //store previous guessed chords

    Chord currentChord;
    Chord nextChord;
    Chord nextNextChord;

    private TextView transitionCounterTxt;
    private TextView currentChordTxt;
    private TextView nextChordTxt;
    private TextView nextNextChordTxt;
    private Button showHintBtn;
    private TextView speedTxt;
    private ProgressBar microphoneProgressBar;
    private TextView currentPlayedChordTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getSupportActionBar().setTitle("Play Your Guitar");

        //request permissions
        String[] permissions = {"android.permission.RECORD_AUDIO"};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, 117);
        }

        //match components to code variables
        transitionCounterTxt = (TextView)findViewById(R.id.transitionCounterTxt);
        currentChordTxt = (TextView)findViewById(R.id.currentChordTxt);
        nextChordTxt = (TextView)findViewById(R.id.nextChordTxt);
        nextNextChordTxt = (TextView)findViewById(R.id.nextNextChordTxt);
        showHintBtn = (Button)findViewById(R.id.showHintBtn);
        speedTxt = (TextView)findViewById(R.id.speedTxt);
        microphoneProgressBar = (ProgressBar)findViewById(R.id.microphoneProgressBar);
        currentPlayedChordTxt = (TextView)findViewById(R.id.currentPlayedChordTxt);

        //get selected chords
        Intent intent = getIntent();
        selectedChords = (ArrayList<Chord>)intent.getSerializableExtra("selectedChords");
        chords = (ArrayList<Chord>)intent.getSerializableExtra("chords");

        //generate starter chords
        currentChord = getRandomChord();
        nextChord = getRandomChord();
        nextNextChord = getRandomChord();

        //update ui
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentChordTxt.setText(currentChord.name);
                nextChordTxt.setText(nextChord.name);
                nextNextChordTxt.setText(nextNextChord.name);
            }
        });

        runPcpSystem();
    }

    public void hintBtnClicked(View view) {
        String hint = currentChord.hint;
//        final String formattedHint = "E: " + hint.charAt(0) + " A: " + hint.charAt(1) +
//                " D: " + hint.charAt(2) + " G: " + hint.charAt(3) +
//                " B: " + hint.charAt(4) + " E: " + hint.charAt(5);
        final String formattedHint = hint.charAt(0) + " " + hint.charAt(1) + " " + hint.charAt(2) + " "
                + hint.charAt(3) + " " + hint.charAt(4) + " " + hint.charAt(5);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showHintBtn.setText(formattedHint);
            }
        });
    }

    void runPcpSystem() {
        System.out.println("run pcp system triggered");
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRate, channels, audioFormat, bufferSizeInBytes);
        recorder.startRecording();
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final short[] samples = new short[bufferSizeInBytes / 2];
                while (true) {
                    int result = recorder.read(samples, 0, bufferSizeInBytes / 2);
                    if (result > 0) { //checking if there are any samples to collect

                        //collect the samples
                        float[] floatSamples = new float[samples.length];
                        for (int i = 0; i < samples.length; i++) {
                            floatSamples[i] = (float) samples[i];
                        }

                        //perform fourier transform and collect results
                        FloatFFT_1D floatFFT_1D = new FloatFFT_1D(floatSamples.length);
                        floatFFT_1D.realForward(floatSamples);
                        float[] magnitudes = new float[floatSamples.length / 2]; //only fetch the real part
                        for (int s = 0; s < magnitudes.length; s++) {
                            magnitudes[s] = floatSamples[s * 2];
                        }

                        //calculate the average pcp
                        switch (currentpcp) {
                            case 0: pcp1 = generatePCP(sampleRate, 261.6f, magnitudes); break;
                            case 1: pcp2 = generatePCP(sampleRate, 261.6f, magnitudes); break;
                            case 2: pcp3 = generatePCP(sampleRate, 261.6f, magnitudes); break;
                        }
                        currentpcp = (currentpcp + 1) % 3;
                        for (int i = 0; i < 12; i++) avgpcp[i] = (pcp1[i] + pcp2[i] + pcp3[i]) / 3f;

                        //calculate loudness
                        double loudness= 0;
                        for (int i = 0; i < magnitudes.length; i++) loudness += magnitudes[i];
                        loudness = loudness / magnitudes.length;
                        loudness = Math.abs(loudness);

                        //update the microphone sensitivity bar
                        //final int progress =  (int)(loudness / 327.68f); //this is the correct value
                        final int progress =  (int)(loudness / 100f);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                microphoneProgressBar.setProgress(progress);
                            }
                        });

                        //update results
                        final String guessedChord = (loudness > silenceThreshold) ? guessChord(avgpcp) : "Silence";
                        results.add(guessedChord);

                        //update current played chord text
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                currentPlayedChordTxt.setText("Current Chord: " + guessedChord);
                            }
                        });

                        //cut off quiet noises and perform chord check
                        if (loudness > silenceThreshold) {
                            //perform chord check and make sure past pcp derived chords are equal
                            if (guessedChord.equals(currentChord.name) && results.allElementsEqual()) {
                                cycleChords();
                            }
                        }
                    }
                }
            }
        }, "Recording Thread");
        recordingThread.start();
    }

    public String guessChord(float[] pcp) {
        String greatestChord = "";
        double greatestScore = Double.MIN_VALUE;

        for (int i = 0; i < chords.size(); i++)
        {
            double score = 0; //aka weighted sum
            for (int p = 0; p < 12; p++)
            {
                double weight = chords.get(i).pcp[p];
                score += (weight * pcp[p]);
            }
            if (score > greatestScore)
            {
                greatestScore = score;
                greatestChord = chords.get(i).name;
            }
        }

        if (greatestScore < 0.5f) greatestChord = "No Chord"; //adds accuracy to system
        return greatestChord;
    }

    float[] generatePCP(int fs, float fref, float[] magnitudes) {
        float[] pcp = new float[12];
        int p = -1;
        float frequency = 0;

        for (int l = 0; l < magnitudes.length && frequency < 5000; l++) {
            frequency = ((float)l / (float)magnitudes.length) * ((float)sampleRate / 2f);

            p = mTable(l, fs, fref, magnitudes.length);
            if (p >= 0 && p < 12) {
                pcp[p] += Math.pow(Math.abs(magnitudes[l]), 2);
            }
        }

        double mag = 0;
        for (int j = 0; j < 12; j++) mag += pcp[j] * pcp[j];
        mag = Math.sqrt(mag);
        for (int k = 0; k < 12; k++) pcp[k] /= mag;

        return pcp;
    }

    int mTable(int l, int fs, float fref, int N) {
        if (l == 0) return -1;
        double a = ((double)fs * ((double)l / (double)N)) / (double)fref;
        return (int)Math.round(12f * logBaseTwo(a)) % 12;
    }

    double logBaseTwo(double num) {
        return Math.log(num) / Math.log(2);
    }

    void cycleChords() {
        transitions++;
        currentChord = nextChord;
        nextChord = nextNextChord;
        nextNextChord = getRandomChord();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                transitionCounterTxt.setText("Transitions: " + transitions);
                currentChordTxt.setText(currentChord.name);
                nextChordTxt.setText(nextChord.name);
                nextNextChordTxt.setText(nextNextChord.name);
                showHintBtn.setText("DISPLAY HINT");
            }
        });
    }

    Chord getRandomChord() {
        Chord selectedChord = null;
        do {
            selectedChord = selectedChords.get(random.nextInt(selectedChords.size()));
        } while (selectedChord.name.equals(previousRandomChord));
        previousRandomChord = selectedChord.name;
        return selectedChord;
    }
}