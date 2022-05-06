package de.webis.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.InterpretationAnnotation;
import de.webis.datastructures.Query;
import de.webis.datastructures.WebisCorpusQuery;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class WebisJsonStreamReader extends CorpusStreamReader {
    private final ObjectMapper jsonMapper;
    private Iterator<JsonNode> queryIter;

    private static final String CORPUS_TEST_PATH =
            "./data/corpora/webis-qinc-22/test/webis-qinc-22-test.json";

    private static final String CORPUS_TRAIN_PATH =
            "./data/corpora/webis-qinc-22/train/webis-qinc-22-train.json";

    public WebisJsonStreamReader() {
        jsonMapper = new ObjectMapper();
        try {
            JsonNode node = jsonMapper.readValue(
                    new File(CORPUS_TEST_PATH),
                    JsonNode.class);

            queryIter = node.get("queries").iterator();
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: Corpus file \"" + CORPUS_TEST_PATH + "\" not found!");
        } catch (IOException e) {
            System.err.println("ERROR: Unexpected error while parsing \"" + CORPUS_TEST_PATH + "\"!");
            e.printStackTrace();
        }
    }

    public WebisJsonStreamReader(CorpusType corpusType) {
        jsonMapper = new ObjectMapper();
        File corpusFile = null;

        try {
            if (corpusType == CorpusType.TRAIN) {
                corpusFile = new File(CORPUS_TRAIN_PATH);
            } else {
                corpusFile = new File(CORPUS_TEST_PATH);
            }

            JsonNode node = jsonMapper.readValue(
                    corpusFile,
                    JsonNode.class);

            queryIter = node.get("queries").iterator();
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: Corpus file \"" + corpusFile.getPath() + "\" not found!");
        } catch (IOException e){
            System.err.println("ERROR: Unexpected error while parsing \"" + corpusFile.getPath() + "\"!");
        }
    }

    @Override
    public Query next() {
        WebisCorpusQuery query = new WebisCorpusQuery();

        JsonNode currentQuery = queryIter.next();

        query.setId(currentQuery.get("id").asText());
        query.setText(currentQuery.get("query").asText());
        query.setDifficulty(currentQuery.get("difficulty").asInt());

        Set<String> categories = new HashSet<>();

        Iterator<JsonNode> elementIter = currentQuery.get("categories").iterator();

        while(elementIter.hasNext()){
            categories.add(elementIter.next().asText());
        }

        query.setCategories(categories);

        elementIter = currentQuery.get("explicit_entities").iterator();

        while(elementIter.hasNext()){
            JsonNode element = elementIter.next();
            EntityAnnotation annotation = new EntityAnnotation();

            annotation.setMention(element.get("mention").asText());
            annotation.setBegin(query.getText().indexOf(annotation.getMention()));
            annotation.setEnd(annotation.getBegin() + annotation.getMention().length());

            Iterator<JsonNode> entityIter = element.get("entity").iterator();
            annotation.setUrl(entityIter.next().asText());
            annotation.setScore(element.get("relevance").asDouble());

            if(annotation.getUrl().matches("^http(s)?://en.wikipedia.org/wiki/(.)*")) {
                query.addExplicitEntity(annotation);
            }
        }

        elementIter = currentQuery.get("implicit_entities").iterator();

        while(elementIter.hasNext()){
            JsonNode element = elementIter.next();
            EntityAnnotation annotation = new EntityAnnotation();

            annotation.setMention(element.get("mention").asText());
            annotation.setBegin(query.getText().indexOf(annotation.getMention()));
            annotation.setEnd(annotation.getBegin() + annotation.getMention().length());

            Iterator<JsonNode> entityIter = element.get("entity").iterator();
            annotation.setUrl(entityIter.next().asText());
            annotation.setScore(element.get("relevance").asDouble());

            if(annotation.getUrl().matches("^http(s)?://en.wikipedia.org/wiki/(.)*")) {
                query.addImplicitEntity(annotation);
            }
        }

        elementIter = currentQuery.get("interpretations").iterator();

        while(elementIter.hasNext()){
            JsonNode element = elementIter.next();
            InterpretationAnnotation annotation = new InterpretationAnnotation();
            List<String> interpretation = new LinkedList<>();

            annotation.setId(element.get("id").asInt());
            element.get("interpretation").iterator().forEachRemaining(
                    jsonNode -> interpretation.add(jsonNode.asText()));

            annotation.setInterpretation(interpretation);
            annotation.setRelevance(element.get("relevance").asDouble());
            if (annotation.getRelevance() > 1) {
                query.addInterpretation(annotation);
            }
        }


        return query;
    }

    @Override
    public boolean hasNext() {
        return queryIter.hasNext();
    }
}