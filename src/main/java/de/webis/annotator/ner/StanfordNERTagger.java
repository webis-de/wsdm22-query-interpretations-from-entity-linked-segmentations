package de.webis.annotator.ner;

import de.webis.annotator.EntityAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.Triple;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StanfordNERTagger implements EntityAnnotator {
    private static AbstractSequenceClassifier<CoreLabel> classifier;

    public StanfordNERTagger() {
        try {
            classifier = CRFClassifier.getClassifier(
                    getClass()
                            .getClassLoader()
                            .getResource("classifiers/english.all.3class.distsim.crf.ser.gz")
                            .getFile()
            );
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<EntityAnnotation> annotate(Query query) {
        Set<EntityAnnotation> entityAnnotations = new HashSet<>();
        List<Triple<String, Integer, Integer>> annotations = classifier.classifyToCharacterOffsets(query.getText());

        for (Triple<String, Integer, Integer> annotation: annotations){
            EntityAnnotation entityAnnotation = new EntityAnnotation();
            entityAnnotation.setBegin(annotation.second);
            entityAnnotation.setEnd(annotation.third);
            entityAnnotation.setMention(query.getText().substring(entityAnnotation.getBegin(), entityAnnotation.getEnd()));

            entityAnnotations.add(entityAnnotation);
        }

        return entityAnnotations;
    }

    @Override
    public String getAnnotationTag() {
        return "stanford-ner";
    }

    public static void main(String[] args) {
        StanfordNERTagger tagger = new StanfordNERTagger();
        tagger.annotate(new Query("new york times square dance"));
    }
}
