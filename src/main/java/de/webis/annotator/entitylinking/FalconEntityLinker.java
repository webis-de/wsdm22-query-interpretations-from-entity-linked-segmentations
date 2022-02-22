package de.webis.annotator.entitylinking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.EntityAnnotator;
import de.webis.annotator.HttpPostAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public class FalconEntityLinker extends HttpPostAnnotator {
    private final ObjectMapper jsonMapper;

    public FalconEntityLinker(){
        super("https", "labs.tib.eu", 443, "/falcon/api", "data/persistent/logging/falcon");

        defaultParams.add(new BasicNameValuePair("mode", "short"));
        jsonMapper = new ObjectMapper();
    }

    @Override
    public String getAnnotationTag() {
        return "falcon";
    }

    @Override
    protected void extractAnnotations(Query query, String response, Set<EntityAnnotation> annotations) throws IOException {
        String[] responseLines = response.split("\n");

        for(String line: responseLines){
            JsonNode entityNode = jsonMapper.readValue(line, JsonNode.class).get("entities");

            for (JsonNode node : entityNode) {
                URI uri = null;
                try {
                    uri = new URI(node.get(0).asText());
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }

                if (uri != null) {
                    EntityAnnotation annotation = new EntityAnnotation();

                    annotation.setBegin(0);
                    annotation.setEnd(query.getText().length() - 1);
                    annotation.setMention(query.getText());
                    annotation.setUrl(uri.getPath().replaceFirst("/resource/", "http://en.wikipedia.org/wiki/"));
                    annotation.setScore(node.get(1).asDouble());

                    annotations.add(annotation);
                }
            }
        }
    }

    @Override
    protected void prepareRequest(Query query) {
        entityBuilder.setText("{\"text\":\"" + query.getText() + "\"}");
    }

    public static void main(String[] args) {
        EntityAnnotator annotator = new FalconEntityLinker();
        System.out.println(annotator.annotate(new Query("new york times square dance")));
    }


}
