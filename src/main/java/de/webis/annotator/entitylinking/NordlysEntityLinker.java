package de.webis.annotator.entitylinking;

import com.fasterxml.jackson.databind.JsonNode;
import de.webis.datastructures.EntityAnnotation;

import java.io.IOException;
import java.util.Set;

public class NordlysEntityLinker extends NordlysToolkit {

    public NordlysEntityLinker() {
        super("/el");
    }

    @Override
    protected void parseAnnotations(String jsonString, Set<EntityAnnotation> annotations) {
        try {
            JsonNode node = jsonMapper.readValue(jsonString, JsonNode.class);

            for (JsonNode jsonNode : node.get("results")) {
                EntityAnnotation annotation = new EntityAnnotation();

                String url = jsonNode.get("entity").asText();
                url = url.replaceAll("[<>]", "").replace("dbpedia:", "https://en.wikipedia.org/wiki/");
                annotation.setUrl(url);
                annotation.setMention(jsonNode.get("mention").asText());
                annotation.setScore(jsonNode.get("score").asDouble());

                annotations.add(annotation);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getAnnotationTag() {
        return "nordlys-el";
    }

}
