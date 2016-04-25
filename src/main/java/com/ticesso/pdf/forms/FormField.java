package com.ticesso.pdf.forms;

import java.io.Serializable;

/**
 * Represents a form field.
 */
public class FormField implements Serializable {

    private final String name;
    private String type;

    private int page;

    private float left;
    private float top;
    private float width;
    private float height;

    public FormField(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public float getLeft() {
        return left;
    }

    public void setLeft(float left) {
        this.left = left;
    }

    public float getTop() {
        return top;
    }

    public void setTop(float top) {
        this.top = top;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
