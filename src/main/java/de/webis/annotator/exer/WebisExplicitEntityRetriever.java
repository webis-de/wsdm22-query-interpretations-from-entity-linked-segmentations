package de.webis.annotator.exer;

import de.webis.annotator.EntityAnnotator;
import de.webis.annotator.LoggedAnnotator;
import de.webis.annotator.exer.strategies.AllNGrams;
import de.webis.annotator.exer.strategies.ExerStrategy;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;
import de.webis.datastructures.persistent.PersistentStore;
import de.webis.metrics.EntityCommonness;
import de.webis.metrics.Metric;
import de.webis.utils.StreamSerializer;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class WebisExplicitEntityRetriever implements EntityAnnotator, LoggedAnnotator {
    private final static String INDEX_PATH = "data/persistent/wiki-entity-index";

    private final PersistentStore<String, Set<String>> index;

    private final ExerStrategy strategy;
    private final Metric entityCommonness;

    public WebisExplicitEntityRetriever() {
        this(new AllNGrams());
    }

    public WebisExplicitEntityRetriever(ExerStrategy strategy) {
        this.strategy = strategy;

        if(!new File(INDEX_PATH).exists()){
            try {
                throw new FileNotFoundException("Index (" + INDEX_PATH + ") does not exist!");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        index = new PersistentStore<>(INDEX_PATH);
        index.setSerializer(StreamSerializer.class);

        entityCommonness = EntityCommonness.getInstance();
    }

    @Override
    public Set<EntityAnnotation> annotate(Query query) {
        Set<EntityAnnotation> entityAnnotations = new LinkedHashSet<>();

        Set<String> segments = strategy.apply(query);
        List<String> sortedSegments = new LinkedList<>(segments);
        sortedSegments.sort(Comparator.comparingInt(String::length).reversed());

        for (String segment : sortedSegments) {
            Set<String> annotations = index.get(segment);

            if (annotations != null) {

                for (String annotation : annotations) {
                    EntityAnnotation entityAnnotation = new EntityAnnotation();
                    entityAnnotation.setBegin(query.getText().indexOf(segment));
                    entityAnnotation.setEnd(entityAnnotation.getBegin() + segment.length());
                    entityAnnotation.setMention(segment);
                    entityAnnotation.setUrl(annotation);
                    String entityName = FilenameUtils.getBaseName(entityAnnotation.getUrl());
                    entityName = entityName.toLowerCase().replaceAll("_", " ");
                    entityAnnotation.setScore(
                            entityCommonness.get(
                                    entityName,
                                    entityAnnotation.getMention())
                    );

                    entityAnnotations.add(entityAnnotation);
                }
            }
        }

        List<EntityAnnotation> sortedAnnotations = new LinkedList<>(entityAnnotations);
        sortedAnnotations.sort(Comparator.comparingDouble(EntityAnnotation::getScore).reversed());

        return new LinkedHashSet<>(sortedAnnotations);
    }

    @Override
    public String getAnnotationTag() {
        return "webis-exer";
    }

    @Override
    public void close() {
        index.close();
        if(entityCommonness != null){
            entityCommonness.close();
        }
    }
}
