package de.webis.annotator.ner;

import de.webis.annotator.EntityAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;
import edu.mit.ll.mitie.*;

import java.util.HashSet;
import java.util.Set;

public class MITIEEntityRecognizer implements EntityAnnotator {
    private final NamedEntityExtractor namedEntityExtractor;

    public MITIEEntityRecognizer(){
        System.loadLibrary("javamitie");
        namedEntityExtractor = new NamedEntityExtractor(getClass().getResource("/ner_model.dat").getPath());
    }

    @Override
    public Set<EntityAnnotation> annotate(Query query) {
        Set<EntityAnnotation> annotations = new HashSet<>();

        StringVector tokens = global.tokenize(query.getText());
        EntityMentionVector entities = namedEntityExtractor.extractEntities(tokens);

        StringBuilder mentionBuilder = new StringBuilder();
        for(int i = 0; i < entities.size(); i++){
            EntityAnnotation entityAnnotation = new EntityAnnotation();
            EntityMention mention = entities.get(i);

            for(int j = mention.getStart(); j < mention.getEnd(); j++){
                mentionBuilder.append(tokens.get(j)).append(" ");
            }

            entityAnnotation.setMention(mentionBuilder.toString().trim());

            entityAnnotation.setBegin(query.getText().indexOf(entityAnnotation.getMention()));
            entityAnnotation.setEnd(entityAnnotation.getBegin() + entityAnnotation.getMention().length());

            entityAnnotation.setScore(mention.getScore());

            annotations.add(entityAnnotation);

            mentionBuilder.delete(0, mentionBuilder.length());
        }

        return annotations;
    }

    @Override
    public String getAnnotationTag() {
        return "mit-ie";
    }

    public static void main(String[] args) {
        EntityAnnotator annotator = new MITIEEntityRecognizer();

        annotator.annotate(new Query("Barack Obama"));
    }
}
