package de.webis.annotator.ner;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.EntityAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class FlairNLPTagger implements EntityAnnotator {
    private Process process;

    private final ObjectMapper jsonMapper;

    public FlairNLPTagger(){
        jsonMapper = new ObjectMapper();
        jsonMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    @Override
    public Set<EntityAnnotation> annotate(Query query) {
        Set<EntityAnnotation> annotations = new HashSet<>();

        BufferedReader reader = null;
        if(process == null){
            try{
                process = Runtime.getRuntime().exec(new String[]{"envs/flairnlp/bin/python", "src_py/flair_nlp_tagger.py"});
            } catch (IOException e) {
                e.printStackTrace();
                return annotations;
            }

            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            try{
                while ((line = reader.readLine()) != null){
                    if(line.contains("READY")){
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else{
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        }

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
        writer.println(query.getText());
        writer.flush();

        String line;
        try{
            line = reader.readLine();
            JsonNode node = jsonMapper.readValue(line, JsonNode.class);

            for(JsonNode entityNode: node.get("entities")){
                EntityAnnotation entityAnnotation = new EntityAnnotation();

                entityAnnotation.setBegin(entityNode.get("start").asInt());
                entityAnnotation.setEnd(entityNode.get("end").asInt());
                entityAnnotation.setMention(entityNode.get("mention").asText());

                annotations.add(entityAnnotation);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return annotations;
    }

    @Override
    public String getAnnotationTag() {
        return "flair-nlp";
    }

}
