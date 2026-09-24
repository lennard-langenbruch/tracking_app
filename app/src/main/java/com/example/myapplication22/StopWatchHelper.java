package com.example.myapplication22;

import android.widget.TextView;

import org.apache.commons.lang3.time.StopWatch;

public class StopWatchHelper {

    private final StopWatch stopWatch = new StopWatch();
    private final TextView textView;
    private volatile boolean running = false;
    private Thread updateThread;

    public StopWatchHelper(TextView textView) {
        this.textView = textView;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        stopWatch.start();

        // Separate thread for updating the UI
        updateThread = new Thread(() -> {
            while (running) {
                update();
                try {
                    Thread.sleep(100); // Update every 100 milliseconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        updateThread.start();
    }

    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        stopWatch.stop();
        try {
            updateThread.join(); // Wait for the update thread to finish
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void update() {
        if (stopWatch.isStarted()) {
            long elapsedMilliseconds = stopWatch.getTime();
            String time = String.format("Elapsed Time: %02d:%02d:%02d",
                    elapsedMilliseconds / 60000,
                    (elapsedMilliseconds / 1000) % 60,
                    (elapsedMilliseconds % 1000) / 10);

            textView.post(() -> textView.setText(time));
        }
    }
}
