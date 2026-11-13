package com.bakery.bake_nest.model;

import jakarta.persistence.Entity;

@Entity
public class Cupcake extends Product {
    private String frostingFlavor;

    public Cupcake() {
        super();
    }

    public String getFrostingFlavor() {
        return frostingFlavor;
    }

    public void setFrostingFlavor(String frostingFlavor) {
        this.frostingFlavor = frostingFlavor;
    }
}