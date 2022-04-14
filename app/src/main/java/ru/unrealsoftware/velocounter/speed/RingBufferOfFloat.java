package ru.unrealsoftware.velocounter.speed;

public class RingBufferOfFloat {

    private double[] array;
    private final int maxSize;
    private int writePointer = -1;
    private int currentSize = 0;

    public RingBufferOfFloat(int maxSize) {
        array = new double[maxSize];
        this.maxSize = maxSize;
    }

    public void add(double value) {
        synchronized (this) {
            currentSize++;
            if (currentSize > maxSize)
                currentSize = maxSize;

            writePointer++;
            if (writePointer == maxSize)
                writePointer = 0;
            array[writePointer] = value;
        }
    }

    public double[] getArray() {
        synchronized (this) {
            if (currentSize == maxSize)
                return array.clone();
            else {
                double[] newArray = new double[currentSize];
                System.arraycopy(array, 0, newArray, 0, currentSize);
                return newArray;
            }
        }
    }

    public void clear() {
        synchronized (this) {
            writePointer = -1;
            currentSize = 0;
            array = new double[maxSize];
        }
    }
}