package com.example.guitarchordtrainer;

import java.io.Serializable;
import java.util.Arrays;

public class Chord implements Serializable {
    public String name;
    public double[] pcp;
    public String hint;

    public Chord(String name, double[] pcp, String hint)
    {
        this.name = name;
        this.pcp = pcp;
        this.hint = hint;
    }

    @Override
    public String toString() {
        return "Chord{" +
                "name='" + name + '\'' +
                ", pcp=" + Arrays.toString(pcp) +
                ", hint='" + hint + '\'' +
                '}';
    }
}