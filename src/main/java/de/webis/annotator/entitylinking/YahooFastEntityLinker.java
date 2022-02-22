package de.webis.annotator.entitylinking;


import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.HttpGetAnnotator;
import de.webis.datastructures.AnnotationResult;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;

import java.io.IOException;
import java.util.Set;

public class YahooFastEntityLinker extends HttpGetAnnotator {
    private final ObjectMapper jsonMapper;

    public YahooFastEntityLinker(){
        super("http", "localhost", 8003, "/annotate/yahoo-fel");
        jsonMapper = new ObjectMapper();
    }

    @Override
    protected void extractAnnotations(Query query, String response, Set<EntityAnnotation> annotations) throws IOException {
        AnnotationResult annotationResult = jsonMapper.readerFor(AnnotationResult.class).readValue(response);

        annotations.addAll(annotationResult.getEntities());
    }

    @Override
    protected void prepareRequest(Query query) {
        this.uriBuilder.addParameter("query", query.getText());
    }

    @Override
    public String getAnnotationTag() {
        return "yahoo-fel";
    }

    public static void main(String[] args) {
        YahooFastEntityLinker linker = new YahooFastEntityLinker();

        linker.annotate(new Query("new york times square dance"));
    }


}
