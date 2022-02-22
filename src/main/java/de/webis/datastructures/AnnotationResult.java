package de.webis.datastructures;

import java.util.HashSet;
import java.util.Set;

public class AnnotationResult {
    private long id;
    private String query;

    private long runtimeMillis;

    private final Set<EntityAnnotation> entities;

    public AnnotationResult() {
        entities = new HashSet<>();
    }

    public AnnotationResult(String query, long runtimeMillis) {
        this.id = (System.currentTimeMillis() + query).hashCode();
        this.query = query;
        this.runtimeMillis = runtimeMillis;
        this.entities = new HashSet<>();
    }

    public void addEntity(EntityAnnotation annotation){
        entities.add(annotation);
    }

    public long getId() {
        return id;
    }

    public String getQuery() {
        return query;
    }

    public long getRuntimeMillis() {
        return runtimeMillis;
    }

    public Set<EntityAnnotation> getEntities() {
        return entities;
    }

//    @JsonProperty("entities")
//    private void unpackEntities(Map<String, Object> entities){
//        System.out.println(entities);
//        for(Map.Entry<String, Object> entity: entities.entrySet()){
//            System.out.println(entity.getValue());
//        }
//    }
}
