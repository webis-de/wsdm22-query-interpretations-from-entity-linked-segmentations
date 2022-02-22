package de.webis.datastructures;

import java.io.Serializable;

public class IndexedDocument implements Serializable{
    private String url;

    private int tfValue;

    public IndexedDocument(String url, int tfValue) {
        this.url = url;
        this.tfValue = tfValue;
    }

    public IndexedDocument(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getTfValue() {
        return tfValue;
    }

    public void setTfValue(int tfValue) {
        this.tfValue = tfValue;
    }

    @Override
    public String toString(){
        return url + " | " +tfValue;
    }
}
