package de.webis.datastructures;

import java.util.ArrayList;
import java.util.List;

public class WebisCorpusQuery extends Query {

    private List<EntityAnnotation> explicitEntities;
    private List<EntityAnnotation> implicitEntities;

    private List<InterpretationAnnotation> interpretations;

    public WebisCorpusQuery() {
        explicitEntities = new ArrayList<>();
        implicitEntities = new ArrayList<>();

        interpretations = new ArrayList<>();
    }

    public List<EntityAnnotation> getExplicitEntities() {
        return explicitEntities;
    }

    public void setExplicitEntities(List<EntityAnnotation> explicitEntities) {
        this.explicitEntities = explicitEntities;
    }

    public List<EntityAnnotation> getImplicitEntities() {
        return implicitEntities;
    }

    public void setImplicitEntities(List<EntityAnnotation> implicitEntities) {
        this.implicitEntities = implicitEntities;
    }

    public List<InterpretationAnnotation> getInterpretations() {
        return interpretations;
    }

    public void setInterpretations(List<InterpretationAnnotation> interpretations) {
        this.interpretations = interpretations;
    }

    public void addExplicitEntity(EntityAnnotation annotation){
        explicitEntities.add(annotation);
    }

    public void addImplicitEntity(EntityAnnotation annotation){
        implicitEntities.add(annotation);
    }

    public void addInterpretation(InterpretationAnnotation annotation){
        interpretations.add(annotation);
    }

    @Override
    public void addAnnotation(EntityAnnotation annotation){
        throw new UnsupportedOperationException();
    }
}
