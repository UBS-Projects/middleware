package com.middleware.backend.example;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "constant")
public class Constant {

    @Id
    private String key;

    private String value;

    public Constant() {
    }

    public Constant(String key, String value) {
        this.key = key;
        this.value = value;
    }
// Getters and setters

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
