package de.webis.annotator;

import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;

import java.util.Set;

public interface EntityAnnotator {
    Set<EntityAnnotation> annotate(Query query);
    String getAnnotationTag();
}
