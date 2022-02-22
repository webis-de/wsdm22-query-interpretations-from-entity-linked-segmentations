package de.webis.annotator.entitylinking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.EntityAnnotator;
import de.webis.annotator.HttpGetAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

public class BabelfyEntityLinker extends HttpGetAnnotator {
    private final ObjectMapper jsonMapper;

    public BabelfyEntityLinker(){
        super("https", "babelfy.io", 443, "/v1/disambiguate", "data/persistent/logging/babelfy");

        jsonMapper = new ObjectMapper();

        Properties properties = new Properties();
        try {
            properties.load(BabelfyEntityLinker.class.getResourceAsStream("/babelfy.properties"));
        } catch (IOException e) {
            System.err.println("ERROR: [BABELFY] Error parsing " + BabelfyEntityLinker.class.getResource("/babelfy.properties"));
            e.printStackTrace();
        }

        defaultParams.add(new BasicNameValuePair("lang", "EN"));
        defaultParams.add(new BasicNameValuePair("key", properties.getProperty("API_KEY", "")));

    }

    @Override
    protected void prepareRequest(Query query) {
        uriBuilder.addParameter("text", query.getText());
    }

    @Override
    protected void extractAnnotations(Query query, String response, Set<EntityAnnotation> annotations) throws IOException {
        JsonNode node = jsonMapper.readValue(response, JsonNode.class);

        Iterator<JsonNode> resultIterator = node.elements();

        while(resultIterator.hasNext()){
            node = resultIterator.next();
            if(node.asText().equals("Your key is not valid or the daily requests limit has been reached. Please visit http://babelfy.org.")){
                throw new IOException("Key is invalid or daily limit exceeded!");
            }

            if(node.has("DBpediaURL"))
            if(!node.get("DBpediaURL").asText().isEmpty()){
                EntityAnnotation annotation = new EntityAnnotation();

                annotation.setBegin(node.get("charFragment").get("start").asInt());
                annotation.setEnd(node.get("charFragment").get("end").asInt() + 1);
                annotation.setMention(query.getText().substring(annotation.getBegin(), annotation.getEnd()));
                annotation.setUrl(node.get("DBpediaURL").asText()
                        .replaceAll("http://dbpedia.org/resource/", "https://en.wikipedia.org/wiki/"));
                annotation.setScore(node.get("score").asDouble());

                annotations.add(annotation);
            }
        }

    }

    @Override
    public String getAnnotationTag() {
        return "babelfy";
    }

    public static void main(String[] args){
        EntityAnnotator annotator = new BabelfyEntityLinker();
        Set<EntityAnnotation> annotations = annotator.annotate(new Query("new york times square dance"));

        System.out.println(annotations);
    }
}
