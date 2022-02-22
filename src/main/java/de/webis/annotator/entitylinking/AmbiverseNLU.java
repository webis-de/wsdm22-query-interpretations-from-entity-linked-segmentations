package de.webis.annotator.entitylinking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.EntityAnnotator;
import de.webis.annotator.HttpPostAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Ambiverse
 */
public class AmbiverseNLU extends HttpPostAnnotator {
    private int queryIncrement;

    private final ObjectMapper jsonMapper;

    public AmbiverseNLU(){
        super("http", "localhost", 8001, "/entitylinking/analyze");

        queryIncrement = 0;
        jsonMapper = new ObjectMapper();
    }

    @Override
    public String getAnnotationTag() {
        return "ambiverse-nlu";
    }

    @Override
    protected void extractAnnotations(Query query, String response, Set<EntityAnnotation> annotations) throws IOException {
        String[] responseLines = response.split("\n");

        for(String line: responseLines){
            JsonNode results = jsonMapper.readValue(line, JsonNode.class);

            for (JsonNode matchNode : results.get("matches")) {
                String url;
                double score = 0.0;

                if (matchNode.get("entity").has("id")) {
                    url = matchNode.get("entity").get("id").asText();

                    if (results.has("entities")) {
                        for (JsonNode entityNode : results.get("entities")) {
                            if (entityNode.get("id").asText().equals(url)) {
                                url = entityNode.get("url").asText();

                                score = entityNode.get("salience").asDouble();
                            }
                        }
                    }

                    EntityAnnotation entityAnnotation = new EntityAnnotation(
                            matchNode.get("charOffset").asInt(),
                            matchNode.get("charOffset").asInt() + matchNode.get("charLength").asInt(),
                            matchNode.get("text").asText(),
                            URLDecoder.decode(url, StandardCharsets.UTF_8.name()).replaceAll("[ ]", "_"),
                            score);

                    annotations.add(entityAnnotation);
                }
            }
        }

        queryIncrement++;
    }

    @Override
    protected void prepareRequest(Query query) {
        entityBuilder.setText("{\"docId\": \""+queryIncrement+"\", \"language\": \"en\", \"text\": \"" + query.getText() + "\", \"extractConcepts\": \"false\"}");
    }

    public static void main(String[] args) {
        EntityAnnotator entityAnnotator = new AmbiverseNLU();
        Set<EntityAnnotation> annotations = entityAnnotator.annotate(new Query("new york times square dance"));

        System.out.println(annotations);
    }
}
