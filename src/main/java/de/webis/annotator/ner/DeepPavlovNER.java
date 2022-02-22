package de.webis.annotator.ner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.HttpPostAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;

import java.io.IOException;
import java.util.Set;

public class DeepPavlovNER extends HttpPostAnnotator {
    private final ObjectMapper jsonMapper;

    public DeepPavlovNER() {
        super("http", "localhost", 8006, "model");
        jsonMapper = new ObjectMapper();
    }

    @Override
    protected void extractAnnotations(Query query, String response, Set<EntityAnnotation> annotations) throws IOException {
        JsonNode node = jsonMapper.readValue(response, JsonNode.class);

        JsonNode queryNode = node.get(0).get(0);
        JsonNode annotationNode = node.get(0).get(1);

        StringBuilder mentionBuilder = new StringBuilder();

        for (int i=0; i < annotationNode.size(); i++){
            EntityAnnotation annotation = new EntityAnnotation();
            String annotationTag = annotationNode.get(i).asText();

            if(annotationTag.equals("O")){
                if(mentionBuilder.length() > 0){
                    String mention = mentionBuilder.toString().trim();
                    annotation.setMention(mention);

                    annotation.setBegin(query.getText().indexOf(mention));
                    annotation.setEnd(annotation.getBegin() + mention.length());

                    mentionBuilder = new StringBuilder();
                    annotations.add(annotation);
                }
            } else{
                if(annotationTag.startsWith("I")){
                    mentionBuilder.append(" ").append(queryNode.get(i).asText());
                } else{
                    if(mentionBuilder.length() > 0) {
                        String mention = mentionBuilder.toString().trim();
                        annotation.setMention(mention);

                        annotation.setBegin(query.getText().indexOf(mention));
                        annotation.setEnd(annotation.getBegin() + mention.length());

                        mentionBuilder = new StringBuilder();
                        annotations.add(annotation);
                    }

                    mentionBuilder.append(queryNode.get(i).asText());
                }

            }
        }

        if(mentionBuilder.length() > 0) {
            EntityAnnotation annotation = new EntityAnnotation();
            String mention = mentionBuilder.toString().trim();
            annotation.setMention(mention);

            annotation.setBegin(query.getText().indexOf(mention));
            annotation.setEnd(annotation.getBegin() + mention.length());

            annotations.add(annotation);
        }
    }

    @Override
    protected void prepareRequest(Query query) {
        entityBuilder.setText("{\"x\": [\"" + query.getText() +"\"]}");
    }

    @Override
    public String getAnnotationTag() {
        return "deeppavlov-ner";
    }
}
