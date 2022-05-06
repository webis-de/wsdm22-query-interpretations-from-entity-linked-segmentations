package de.webis.datastructures;

import org.apache.commons.io.FilenameUtils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class EntityAnnotation implements Annotation {
    private int begin;
    private int end;
    private String mention;

    private final List<String> url;

    private double score;

    public EntityAnnotation() {
        url = new ArrayList<>();
    }

    public EntityAnnotation(int begin, int end, String mention, String url) {
        this.begin = begin;
        this.end = end;
        this.mention = mention;
        this.url = new ArrayList<>();
        this.url.add(url);

    }

    public EntityAnnotation(int begin, int end, String mention, String url, double score) {
        this.begin = begin;
        this.end = end;
        this.mention = mention;
        this.url = new ArrayList<>();
        this.url.add(url);
        this.score = score;
    }

    @Override
    public String toString(){
        String encodedBaseName = null;
        try {
            URL urlObj = new URL(url.get(0));
            encodedBaseName = URLEncoder.encode(
                    FilenameUtils.getBaseName(urlObj.getPath()),
                    "utf-8");
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String url = "https://en.wikipedia.org/wiki/" + encodedBaseName;
        return String.format("%4d | %4d | %30s | %100s | %2.4f", begin, end, mention, url, score);
    }

    @Override
    public int hashCode(){
        if(!hasUrl()){
            return mention.hashCode();
        }

        try {
            URL urlObj = new URL(url.get(0));
            String encodedBaseName = URLDecoder.decode(
                    FilenameUtils.getBaseName(urlObj.getPath()).toLowerCase(),
                    "utf-8");

            return encodedBaseName.hashCode();
        } catch (MalformedURLException | UnsupportedEncodingException | IllegalArgumentException e) {
            return url.get(0).hashCode();
        }
    }

    @Override
    public boolean equals(Object other){
        if(other instanceof EntityAnnotation) {
            return this.hashCode() == other.hashCode();
        }

        return false;
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    public String getMention() {
        return mention;
    }

    public String getUrl() {
        if (!hasUrl()){
            return null;
        }

        return url.get(0);
    }

    @Override
    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void setMention(String mention) {
        this.mention = mention;
    }

    public void setUrl(String url) {
        if (this.url.isEmpty()){
            this.url.add(url);
        }
        else {
            this.url.set(0, url);
        }
    }

    public boolean hasUrl() {
        if (this.url.isEmpty()) {
            return false;
        }

        return url.get(0) != null;
    }

    public static void main(String[] args) {
        EntityAnnotation entityEncoded = new EntityAnnotation();
        entityEncoded.setUrl("http://en.wikipedia.org/wiki/Fritz_M%C3%B6ller");


        EntityAnnotation entityDecoded = new EntityAnnotation();
        entityDecoded.setUrl("https://en.wikipedia.org/wiki/Fritz_Möller");

        System.out.println(entityEncoded.equals(entityDecoded));
    }
}
