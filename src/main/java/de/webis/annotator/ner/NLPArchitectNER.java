package de.webis.annotator.ner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.HttpPostAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Set;

public class NLPArchitectNER extends HttpPostAnnotator {
    private URI serviceURL;
    private final ObjectMapper jsonMapper;

    public NLPArchitectNER(){
        super("http", "localhost", 8123,"/inference");

        jsonMapper = new ObjectMapper();
    }

    @Override
    public String getAnnotationTag() {
        return "intel-nlp-architect";
    }

    @Override
    protected void extractAnnotations(Query query, String response, Set<EntityAnnotation> annotations) throws IOException {
        String[] responseLines = response.split("\n");

        for(String line: responseLines){
            JsonNode node = jsonMapper.readValue(line, JsonNode.class);
            node = node.get(0).get("doc");

            Iterator<JsonNode> spanIterator = node.get("spans").iterator();

            while(spanIterator.hasNext()){
                EntityAnnotation annotation = new EntityAnnotation();
                JsonNode span = spanIterator.next();
                annotation.setBegin(span.get("start").asInt());
                annotation.setEnd(span.get("end").asInt());

                try {
                    annotation.setMention(query.getText().substring(annotation.getBegin(), Math.min(annotation.getEnd(), query.getText().length())));
                    annotations.add(annotation);
                } catch (StringIndexOutOfBoundsException e){
                    e.printStackTrace();
                }
            }

        }
    }

    @Override
    protected void prepareRequest(Query query) {
        entityBuilder.setText("{\"model_name\": \"ner\", \"docs\": [{\"id\": 1, \"doc\": \"" + query.getText() + "\"}]}");
    }
}
