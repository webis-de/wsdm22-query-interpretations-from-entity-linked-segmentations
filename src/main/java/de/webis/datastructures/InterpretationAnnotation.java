package de.webis.datastructures;

import de.webis.evaluation.Evaluator;

import java.util.*;
import java.util.regex.Pattern;

public class InterpretationAnnotation implements Annotation {
    private int id;
    private List<String> interpretation;
    private double relevance;

    private final List<String> containedEntities;
    private final Set<String> contextWords;

    private static final Pattern WIKI_URL_PATTERN = Pattern.compile("^http(s)?://en.wikipedia.org/wiki/(.)*");

    public InterpretationAnnotation() {
        containedEntities = new ArrayList<>();
        contextWords = new HashSet<>();
    }

    public InterpretationAnnotation(List<String> interpretation) {
        containedEntities = new ArrayList<>();
        contextWords = new HashSet<>();
        setInterpretation(interpretation);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<String> getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(List<String> interpretation) {
        this.interpretation = interpretation;

        contextWords.clear();
        containedEntities.clear();
        for (String part : interpretation) {
            if (WIKI_URL_PATTERN.matcher(part).matches()) {
                containedEntities.add(part);
            } else {
                contextWords.addAll(Arrays.asList(part.split("\\s")));
            }
        }
    }

    public double getRelevance() {
        return relevance;
    }

    public void setRelevance(double relevance) {
        this.relevance = relevance;
    }

    public List<String> getContainedEntities() {
        return containedEntities;
    }

    public Set<String> getContextWords() {
        return contextWords;
    }

    @Override
    public double getScore() {
        return getRelevance();
    }

    @Override
    public int hashCode() {
        if (Evaluator.MATCHTYPE == MatchType.PM) {
            return String.join("", interpretation)
                    .replaceAll("http(s)?://en.wikipedia.org/wiki/", "")
                    .replaceAll("[\\s]+", "").hashCode();
        } else {
            return String.join("|", interpretation)
                    .replaceAll("http(s)?://en.wikipedia.org/wiki/", "")
                    .trim().hashCode();
        }

    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof InterpretationAnnotation)){
            return false;
        }

        return this.hashCode() == obj.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%-200s | %2.4f", String.join(" | ", interpretation), relevance);
    }
}
