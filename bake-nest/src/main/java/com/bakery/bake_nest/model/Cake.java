package com.bakery.bake_nest.model;

import jakarta.persistence.Entity;

@Entity
public class Cake extends Product {
    private String size;
    private String flavor;

    public Cake() {
        super(); // Calls the parent Product constructor
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getFlavor() {
        return flavor;
    }

    public void setFlavor(String flavor) {
        this.flavor = flavor;
    }
}
