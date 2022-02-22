package de.webis.annotator.exer.strategies;

import de.webis.datastructures.Query;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class All1Grams implements ExerStrategy {
    @Override
    public Set<String> apply(Query query) {
        return new HashSet<>(Arrays.asList(
                query.getText()
                        .trim()
                        .split("[\\s]+")));
    }
}
