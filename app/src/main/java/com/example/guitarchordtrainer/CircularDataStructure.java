package com.example.guitarchordtrainer;

import java.util.Arrays;

public class CircularDataStructure {

    public final int size;
    private String[] data;
    private int position = 0;

    public CircularDataStructure(int size) {
        this.size = size;
        data = new String[size];
    }

    public String get(int index) { return data[index]; }

    public void add(String dataToAdd) {
        data[position] = dataToAdd;
        position = (position + 1) % size;
    }

    public Boolean allElementsEqual() {
        for (int i = 0; i < size - 1; i++) {
            if (!data[i].equals(data[i + 1])) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "CircularDataStructure{" +
                "size=" + size +
                ", data=" + Arrays.toString(data) +
                ", position=" + position +
                '}';
    }
}
