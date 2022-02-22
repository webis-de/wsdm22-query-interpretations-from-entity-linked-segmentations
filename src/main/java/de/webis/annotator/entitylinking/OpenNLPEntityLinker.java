package de.webis.annotator.entitylinking;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.EntityAnnotator;
import de.webis.annotator.HttpGetAnnotator;
import de.webis.datastructures.AnnotationResult;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;

import java.io.IOException;
import java.util.Set;

public class OpenNLPEntityLinker extends HttpGetAnnotator {
    private final ObjectMapper jsonMapper;

    public OpenNLPEntityLinker(){
        super("http", "localhost", 8003, "annotate/apache-opennlp");
        jsonMapper = new ObjectMapper();
    }

    @Override
    public String getAnnotationTag() {
        return "apache-opennlp";
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

    public static void main(String[] args) {
        EntityAnnotator annotator = new OpenNLPEntityLinker();
        System.out.println(annotator.annotate(new Query("new york times square dance")));
    }
}
