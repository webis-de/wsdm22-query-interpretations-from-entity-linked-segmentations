package de.webis.annotator.entitylinking;

import com.textrazor.AnalysisException;
import com.textrazor.TextRazor;
import com.textrazor.annotations.Entity;
import com.textrazor.annotations.Response;
import de.webis.annotator.LoggedAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;
import de.webis.datastructures.persistent.PersistentStore;

import java.io.IOException;
import java.util.*;

public class TextRazorEntityLinker implements LoggedAnnotator {

    private static final Iterator<String> API_KEYS = Arrays.asList(
            "356dda81a43d8b63d711d54ff6d7753994afb0bf24e6c62275429f1e",
            "275a3ee412729eb248231d52a86c9812b7233ae80838fde75dca79fe",
            "9fd721ecf623bee5d9df201ec93046e8a18c53a84e51c8a8a7218a0e").iterator();

    private TextRazor client;
    private static PersistentStore<String, Set<EntityAnnotation>> LOGGER;

    public TextRazorEntityLinker(){
        client = new TextRazor(API_KEYS.next());
        client.addExtractor("words");
        client.addExtractor("entities");
        client.setLanguageOverride("eng");

        LOGGER = new PersistentStore<>("./data/persistent/logging/textrazor");
    }

    @Override
    public Set<EntityAnnotation> annotate(Query query) {
        Set<EntityAnnotation> annotations = new HashSet<>();

        if(LOGGER.contains(query.getText())){
            annotations = LOGGER.get(query.getText());

            if(annotations == null){
                annotations = new HashSet<>();
            }

            return annotations;
        }

        Response response;

        try {
            response = client.analyze(query.getText()).getResponse();
        } catch (IOException | AnalysisException e) {
            if(API_KEYS.hasNext()){
                client = new TextRazor(API_KEYS.next());
                client.addExtractor("words");
                client.addExtractor("entities");
                client.setLanguageOverride("eng");
                return annotate(query);
            } else{
                e.printStackTrace();
                response = null;
            }
        }

        if(response == null){
            return annotations;
        }

        List<Entity> entities = response.getEntities();

        if(entities != null) {
            for (Entity entity : entities) {
                EntityAnnotation annotation = new EntityAnnotation();
                annotation.setBegin(entity.getStartingPos());
                annotation.setEnd(entity.getEndingPos());
                annotation.setMention(entity.getMatchedText());
                annotation.setScore(entity.getRelevanceScore());

                String wikiLink = entity.getWikiLink();

                if (!wikiLink.isEmpty()) {
                    annotation.setUrl(entity.getWikiLink());

                    annotations.add(annotation);
                }
            }

            LOGGER.put(query.getText(), annotations);

            if(entities.isEmpty()){
                LOGGER.put(query.getText(), null);
            }
        }
        else{
            LOGGER.put(query.getText(), null);
        }

        return annotations;
    }

    @Override
    public String getAnnotationTag() {
        return "textrazor";
    }

    @Override
    public void close(){
        LOGGER.close();
    }

    public static void main(String[] args) {
        TextRazorEntityLinker annotator = new TextRazorEntityLinker();

        annotator.annotate(new Query("new york times square dance"));
        annotator.close();
    }
}
