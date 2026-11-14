package com.bakery.bake_nest.model;

import jakarta.persistence.Entity;

@Entity
public class Pastry extends Product {
    private boolean isFilled; // e.g., chocolate or cream filled

    public Pastry() {
        super();
    }

    public boolean isFilled() {
        return isFilled;
    }

    public void setFilled(boolean filled) {
        isFilled = filled;
    }
}