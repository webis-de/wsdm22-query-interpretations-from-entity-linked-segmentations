package de.webis.annotator;

import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;

import java.util.HashSet;
import java.util.Set;

public class BaselineAnnotator implements EntityAnnotator {
    @Override
    public Set<EntityAnnotation> annotate(Query query) {
        return new HashSet<>();
    }

    @Override
    public String getAnnotationTag() {
        return "baseline";
    }
}
