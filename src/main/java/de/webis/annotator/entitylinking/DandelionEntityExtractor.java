package de.webis.annotator.entitylinking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.EntityAnnotator;
import de.webis.annotator.HttpGetAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;

public class DandelionEntityExtractor extends HttpGetAnnotator {
    private final ObjectMapper jsonMapper;

    public DandelionEntityExtractor(){
        super("https", "api.dandelion.eu", 443, "/datatxt/nex/v1/", "./data/persistent/logging/dandelion");
        jsonMapper = new ObjectMapper();

        Properties properties = new Properties();
        try {
            properties.load(DandelionEntityExtractor.class.getResourceAsStream("/dandelion.properties"));
        } catch (IOException e) {
            System.err.println("ERROR: [DANDELION] Error parsing "
                    + DandelionEntityExtractor.class.getResource("/dandelion.properties"));
            e.printStackTrace();
        }

        defaultParams.add(new BasicNameValuePair("lang","en"));
        defaultParams.add(new BasicNameValuePair("token", properties.getProperty("API_KEY", "")));

    }

    @Override
    protected void prepareRequest(Query query) {
        uriBuilder.addParameter("text", query.getText());
    }

    @Override
    protected void extractAnnotations(Query query, String resonse, Set<EntityAnnotation> annotations) throws IOException {
        JsonNode node;

        node = jsonMapper.readValue(resonse, JsonNode.class);

        if(node != null){
            if(node.has("results")){

                for (JsonNode jsonNode : node.get("results")) {
                    node = jsonNode;

                    toAnnotation(node, annotations);
                }
            }
            else{
                toAnnotation(node, annotations);
            }
        }
    }

    private void toAnnotation(JsonNode node, Set<EntityAnnotation> annotations) throws IOException {
        for (JsonNode jsonNode : node.get("annotations")) {
            EntityAnnotation annotation = new EntityAnnotation();
            node = jsonNode;

            annotation.setBegin(node.get("start").asInt());
            annotation.setEnd(node.get("end").asInt());
            annotation.setMention(node.get("spot").asText());
            annotation.setUrl(node.get("uri").asText());
            annotation.setScore(node.get("confidence").asDouble());


            annotations.add(annotation);
        }
    }

    @Override
    public String getAnnotationTag() {
        return "dandelion";
    }

    public static void main(String[] args){
        EntityAnnotator annotator = new DandelionEntityExtractor();
        Set<EntityAnnotation> annotations = annotator.annotate(new Query("new york times square dance"));

        System.out.println(annotations);
    }
}
