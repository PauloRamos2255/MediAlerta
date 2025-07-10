package com.pauloramos.myapplication;

public class Medication {
    private String name;
    private String dose;
    private String time;
    private String repeat;

    public Medication() {
    }

    public Medication(String name, String dose, String time, String repeat) {
        this.name = name;
        this.dose = dose;
        this.time = time;
        this.repeat = repeat;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDose() {
        return dose;
    }

    public void setDose(String dose) {
        this.dose = dose;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getRepeat() {
        return repeat;
    }

    public void setRepeat(String repeat) {
        this.repeat = repeat;
    }
}
