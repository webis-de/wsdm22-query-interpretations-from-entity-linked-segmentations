package de.webis.annotator.entitylinking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.HttpGetAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;
import de.webis.utils.WikiPageIDResolver;

import java.io.IOException;
import java.util.Set;

public class DexterEntityLinking extends HttpGetAnnotator {
    private final ObjectMapper jsonMapper;
    private final WikiPageIDResolver wikiPageIDResolver;

    public DexterEntityLinking(){
        super("http", "localhost", 8002, "/dexter-webapp/api/rest/annotate");

        jsonMapper = new ObjectMapper();
        wikiPageIDResolver = new WikiPageIDResolver();
    }

    @Override
    protected void prepareRequest(Query query) {
        uriBuilder.addParameter("text", query.getText());
    }

    @Override
    protected void extractAnnotations(Query query, String response, Set<EntityAnnotation> annotations) {
        try {
            JsonNode node = jsonMapper.readValue(response, JsonNode.class);

            for (JsonNode jsonNode : node.get("spots")) {
                EntityAnnotation annotation = new EntityAnnotation();
                node = jsonNode;

                annotation.setBegin(node.get("start").asInt());
                annotation.setEnd(node.get("end").asInt());
                annotation.setMention(node.get("mention").asText());
                String url = wikiPageIDResolver.resolvePageID(node.get("entity").asText());
                annotation.setUrl(url);
                annotation.setScore(node.get("score").asDouble());

                if (annotation.getUrl() != null)
                    annotations.add(annotation);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getAnnotationTag() {
        return "dexter";
    }

    public static void main(String[] args) {
        DexterEntityLinking annotator = new DexterEntityLinking();
        System.out.println(annotator.annotate(new Query("new york times square dance")));
        annotator.close();
    }
}
