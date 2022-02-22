package de.webis.annotator.entitylinking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.EntityAnnotator;
import de.webis.annotator.HttpPostAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;

import java.io.IOException;
import java.util.Set;

//import it.unimi.dsi.fastutil.objects.Object2BooleanMap;

public class RadboudEntityLinker extends HttpPostAnnotator {
    private final ObjectMapper jsonMapper;

    public RadboudEntityLinker(){
        super("http", "gem.cs.ru.nl", 80, "/api", "data/persistent/logging/radboud");
        jsonMapper = new ObjectMapper();
    }

    @Override
    protected void extractAnnotations(Query query, String response, Set<EntityAnnotation> annotations) throws IOException {
        JsonNode node = jsonMapper.readValue(response, JsonNode.class);

        for(JsonNode entityNode: node){
            EntityAnnotation entityAnnotation = new EntityAnnotation();
            entityAnnotation.setBegin(entityNode.get(0).asInt());
            entityAnnotation.setEnd(entityNode.get(0).asInt() + entityNode.get(1).asInt());
            entityAnnotation.setMention(entityNode.get(2).asText());
            entityAnnotation.setUrl("https://en.wikipedia.org/wiki/" + entityNode.get(3).asText());
            entityAnnotation.setScore(entityNode.get(4).asDouble());

            annotations.add(entityAnnotation);
        }
    }

    @Override
    protected void prepareRequest(Query query) {
        entityBuilder.setText("{\"text\": \"" + query.getText() + "\", \"spans\": []}");
    }

    @Override
    public String getAnnotationTag() {
        return "radboud-entity-linker";
    }

    public static void main(String[] args) {
        EntityAnnotator annotator = new RadboudEntityLinker();
        System.out.println(annotator.annotate(new Query("If you're going to try, go all the way - Charles Bukowski")));
    }
}
