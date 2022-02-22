package de.webis.annotator.entitylinking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.EntityAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;
import de.webis.datastructures.persistent.PersistentStore;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SmaphEntityLinker implements EntityAnnotator {
    private static URIBuilder uriBuilder;
    private static List<NameValuePair> defaultParams;
    private static ObjectMapper jsonMapper;

    private static PersistentStore<String, String> logger;
    private BufferedWriter runtimeLog;

    public SmaphEntityLinker(){
        uriBuilder = new URIBuilder();
        uriBuilder
                .setScheme("http")
                .setHost("localhost")
                .setPort(8005)
                .setPath("/smaph/annotate");

        defaultParams = new ArrayList<>();
        defaultParams.add(new BasicNameValuePair("google-cse-id", ""));
        defaultParams.add(new BasicNameValuePair("google-api-key", ""));

        jsonMapper = new ObjectMapper();

        logger = new PersistentStore<>("./data/persistent/logging/smaph3");
        try {
            runtimeLog = new BufferedWriter(new FileWriter("./data/persistent/logging/smaph3/runtimeLog.txt", true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<EntityAnnotation> annotate(Query query) {
        System.out.println("Annotate \""+query+"\" ...");
        Set<EntityAnnotation> annotations = new HashSet<>();

        if(logger.contains(query.getText())){
            try {
                extractAnnotations(query, logger.get(query.getText()), annotations);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return annotations;
        }

        uriBuilder.addParameters(defaultParams);
        uriBuilder.addParameter("q",query.getText());

        URL url = null;

        try {
            url = uriBuilder.build().toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
        }

        if(url != null){
            try {
                long start = System.currentTimeMillis();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setUseCaches(false);
                connection.setDoOutput(true);


                int status = connection.getResponseCode();

                if(status == 200){
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();

                    logger.put(query.getText(), content.toString());

                    extractAnnotations(query, content.toString(), annotations);
                    runtimeLog.write(String.valueOf(System.currentTimeMillis() - start));
                    runtimeLog.newLine();
                }
                else{
                    System.err.println(connection.getResponseCode()+": "+connection.getResponseMessage());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        uriBuilder.clearParameters();

        return annotations;
    }

    private void extractAnnotations(Query query, String json, Set<EntityAnnotation> annotations) throws IOException {
        JsonNode node = jsonMapper.readValue(json, JsonNode.class);

        for (JsonNode jsonNode : node.get("annotations")) {
            EntityAnnotation annotation = new EntityAnnotation();

            annotation.setBegin(jsonNode.get("begin").asInt());
            annotation.setEnd(jsonNode.get("end").asInt());
            annotation.setMention(query.getText().substring(annotation.getBegin(), annotation.getEnd()));
            annotation.setUrl(URLDecoder.decode(jsonNode.get("url").asText(), "utf-8").replace(" ", "_"));
            annotation.setScore(jsonNode.get("score").asDouble());

            annotations.add(annotation);
        }
    }

    public void close(){
        try {
            runtimeLog.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.close();
    }

    @Override
    public String getAnnotationTag() {
        return "smaph-3-greedy";
    }
}
