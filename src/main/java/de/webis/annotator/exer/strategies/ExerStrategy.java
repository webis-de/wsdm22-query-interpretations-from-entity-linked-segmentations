package de.webis.annotator.exer.strategies;

import de.webis.datastructures.Query;

import java.util.Set;

public interface ExerStrategy {
    Set<String> apply(Query query);
}
