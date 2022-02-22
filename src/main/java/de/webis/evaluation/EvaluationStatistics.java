package de.webis.evaluation;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

public class EvaluationStatistics {
    private static final List<String> LABELS = Arrays.asList(
            "macro-precision",
            "macro-recall",
            "macro-f1",
            "macro-w-recall",
            "macro-w-f1",
            "micro-precision",
            "micro-recall",
            "micro-f1",
            "micro-w-recall",
            "micro-w-f1",
            "ep-precision",
            "ep-recall",
            "ep-w-recall",
            "ep-f1",
            "ep-w-f1",
            "runtime"
            );

    private Map<String, DoubleSummaryStatistics> statisticsMap;

    public EvaluationStatistics(){
        statisticsMap = new LinkedHashMap<>();

        for(String label: LABELS){
            statisticsMap.put(label, new DoubleSummaryStatistics());
        }
    }

    public EvaluationStatistics accept(String key, Double value) {
        if (statisticsMap.containsKey(key)) {
            statisticsMap.get(key).accept(value);
        } else {
            System.out.println("Unknown metric!");
        }
        return this;
    }

    public double get(String value) {
        if (!LABELS.contains(value)) {
            throw new RuntimeException("Label " + value + " is not existent");
        }

        return statisticsMap.get(value).getAverage();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        DecimalFormat decimalFormat = new DecimalFormat("#.####");
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
        builder.append("\n------------------------------");
        builder.append("\n#queries: ").append(statisticsMap.get("macro-precision").getCount());
        for (Map.Entry<String, DoubleSummaryStatistics> statisticsEntry : statisticsMap.entrySet()) {
            builder.append("\n")
                    .append(statisticsEntry.getKey())
                    .append(": ")
                    .append(decimalFormat.format(statisticsEntry.getValue().getAverage()));
        }

        builder.append("\n------------------------------\n");
        return builder.toString();
    }
}
