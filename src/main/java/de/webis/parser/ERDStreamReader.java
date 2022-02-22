package de.webis.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.PriorityQueue;
import java.util.Queue;

public class ERDStreamReader extends CorpusStreamReader {
    private JsonNode queryLookup;
    private BufferedReader qrelsReader;

    private Queue<String> bufferedLines;

    public ERDStreamReader(){
        ObjectMapper jsonMapper = new ObjectMapper();
        bufferedLines = new PriorityQueue<>();

        try {
            queryLookup = jsonMapper.readValue(new File("./data/corpora/corpus-erd-elq/queries_ERD.json"), JsonNode.class);
            qrelsReader = new BufferedReader(new FileReader("./data/corpora/corpus-erd-elq/qrels_ERD_elq.txt"));

            String line;
            while((line = qrelsReader.readLine()) != null){
                bufferedLines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public Query next() {
        String line = bufferedLines.poll();
        String[] components = line.split("\t");
        boolean first = true;

        Query query = new Query();
        query.setId(components[0]);
        query.setText(queryLookup.get(query.getId()).asText());

        if(!bufferedLines.isEmpty())
        while (bufferedLines.peek().split("\t")[0].equals(query.getId()) || first){
            if(!first){
                components = bufferedLines.poll().split("\t");
            }

            for(int i = 2; i < components.length; i++){
                EntityAnnotation annotation = new EntityAnnotation();
                annotation.setUrl(extractURL(components[i]));
                annotation.setBegin(0);
                annotation.setEnd(query.getText().length());
                annotation.setMention(query.getText());
                query.addAnnotation(annotation);
            }

            first = false;
        }

        return query;

    }

    @Override
    public boolean hasNext() {
        return !bufferedLines.isEmpty();
    }

    private String extractURL(String component){
        return component.replace("<dbpedia:","https://en.wikipedia.org/wiki/")
                .replace(">","");
    }

    public static void main(String[] args) {
        CorpusStreamReader reader = new ERDStreamReader();

        while (reader.hasNext()){
            reader.next();
        }
    }
}
