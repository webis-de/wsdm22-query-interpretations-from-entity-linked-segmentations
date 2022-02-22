package de.webis.annotator;

import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.InterpretationAnnotation;
import de.webis.datastructures.Query;

import java.util.Set;

public interface InterpretationAnnotator {
    Set<InterpretationAnnotation> annotate(Query query, Set<EntityAnnotation> annotations);
}
