package ru.unrealsoftware.velocounter.speed;

import java.util.Arrays;

public class PidGPSSpeedCounter {

    private final RingBufferOfFloat speedBuffer;

    public PidGPSSpeedCounter(int secondsForCounting) {
        speedBuffer = new RingBufferOfFloat(secondsForCounting);
    }

    public double getSpeed(double speed) {
        speedBuffer.add(speed);
        double[] averageSpeedSumBufferArray = speedBuffer.getArray();
        return calculateSpeed(averageSpeedSumBufferArray);
    }

    public void restart() {
        speedBuffer.clear();
    }

    private double calculateSpeed(double[] speed) {
        Arrays.sort(speed);
        double median;
        if (speed.length % 2 == 0)
            median = (speed[speed.length / 2] + speed[speed.length / 2 - 1]) / 2;
        else
            median = speed[speed.length / 2];
        return median;
    }
}
