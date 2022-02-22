package de.webis.datastructures;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Query {
    private String id;
    private String text;
    private Set<String> categories;
    private int difficulty;
    private List<EntityAnnotation> annotations;

    public Query() {
        annotations = new ArrayList<>();
    }

    public Query(String text) {
        this.text = text;
        this.id = String.valueOf(text.hashCode());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public List<EntityAnnotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<EntityAnnotation> annotations) {
        this.annotations = annotations;
    }

    public void addAnnotation(EntityAnnotation annotation){
        annotations.add(annotation);
    }

    @Override
    public String toString() {
        return text;
    }
}
