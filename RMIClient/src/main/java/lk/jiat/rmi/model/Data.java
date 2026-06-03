package lk.jiat.rmi.model;

import java.io.Serializable;

public class Data implements Serializable {
    private int id;
    private String name;

    public Data() {
    }

    public Data(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
