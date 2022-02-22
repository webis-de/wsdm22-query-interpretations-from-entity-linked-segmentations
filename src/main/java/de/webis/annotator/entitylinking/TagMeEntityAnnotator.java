package de.webis.annotator.entitylinking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.HttpGetAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class TagMeEntityAnnotator extends HttpGetAnnotator {
    private static ObjectMapper jsonMapper;

    public TagMeEntityAnnotator(){
        super("https", "tagme.d4science.org", 443, "/tagme/tag", "data/persistent/logging/tagme");
        defaultParams = new ArrayList<>();
        defaultParams.add(new BasicNameValuePair("lang","en"));
        defaultParams.add(new BasicNameValuePair("include_abstract","false"));
        defaultParams.add(new BasicNameValuePair("include_categories", "false"));
        defaultParams.add(new BasicNameValuePair("gcube-token", "139e0c63-d963-4173-9f0f-0b9f09f16b90-843339462"));

        jsonMapper = new ObjectMapper();
    }

    @Override
    protected void prepareRequest(Query query) {
        uriBuilder.addParameter("text", query.getText());
    }

    @Override
    public String getAnnotationTag() {
        return "tagme";
    }

    @Override
    protected void extractAnnotations(Query query, String response, Set<EntityAnnotation> annotations) throws IOException {
        JsonNode node = jsonMapper.readValue(response, JsonNode.class);
        Iterator<JsonNode> annotationIter = node.get("annotations").iterator();

        while(annotationIter.hasNext()){
            EntityAnnotation annotation = new EntityAnnotation();
            JsonNode annotationNode = annotationIter.next();

            annotation.setBegin(annotationNode.get("start").asInt());
            annotation.setEnd(annotationNode.get("end").asInt());
            annotation.setMention(annotationNode.get("spot").asText());
            annotation.setScore(annotationNode.get("rho").asDouble());

            String url = "https://en.wikipedia.org/wiki/"+
                    URLEncoder.encode(annotationNode.get("title").asText(), "utf-8").replace("+","_");

            annotation.setUrl(url);

            annotations.add(annotation);
        }
    }
}
