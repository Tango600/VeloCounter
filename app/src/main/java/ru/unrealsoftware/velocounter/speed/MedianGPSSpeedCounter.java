package ru.unrealsoftware.velocounter.speed;

import java.util.Arrays;

public class MedianGPSSpeedCounter {

    private final RingBufferOfFloat speedBuffer;

    public MedianGPSSpeedCounter(int secondsForCounting) {
        speedBuffer = new RingBufferOfFloat(secondsForCounting);
    }

    public double getSpeed(double speed) {
        speedBuffer.add(speed);
        double[] averageSpeedSumBufferArray = speedBuffer.getArray();
        return calculateMedianSpeed(averageSpeedSumBufferArray);
    }

    public void restart() {
        speedBuffer.clear();
    }

    private double calculateMedianSpeed(double[] speed) {
        Arrays.sort(speed);
        double median;
        if (speed.length % 2 == 0)
            median = (speed[speed.length / 2] + speed[speed.length / 2 - 1]) / 2;
        else
            median = speed[speed.length / 2];
        return median;
    }
}
