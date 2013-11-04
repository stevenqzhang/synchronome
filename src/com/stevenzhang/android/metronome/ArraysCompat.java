package com.stevenzhang.android.metronome;

//Rewrite of Arrays.copyOfRange since Android API <9 does not support it
// From http://stackoverflow.com/questions/16141334/using-arrays-copyofrange-for-below-api-9

public class ArraysCompat {
    public static double[] copyOfRange(double[] from, int start, int end){
        int length = end - start;
        double[] result = new double[length];
        System.arraycopy(from, start, result, 0, length);
        return result;
    }
}
