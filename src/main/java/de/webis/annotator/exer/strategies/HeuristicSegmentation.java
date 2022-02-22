package de.webis.annotator.exer.strategies;

import de.webis.datastructures.Query;
import de.webis.query.segmentation.application.QuerySegmentation;
import de.webis.query.segmentation.core.Segmentation;
import de.webis.query.segmentation.strategies.SegmentationStrategy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HeuristicSegmentation implements ExerStrategy {
    private final QuerySegmentation querySegmentation;
    private final double scoreThreshold;

    public HeuristicSegmentation(SegmentationStrategy segmentationStrategy, double scoreThreshold) {
        querySegmentation = new QuerySegmentation(segmentationStrategy);
        this.scoreThreshold = scoreThreshold;
    }

    @Override
    public Set<String> apply(Query query) {
        Set<String> segments = new HashSet<>();

        Object[] segmentations = querySegmentation.performSegmentationWithFilteration(
                new de.webis.query.segmentation.core.Query(query.getText()), scoreThreshold);

        Map<Segmentation, Integer> segmentationScores = querySegmentation.performSegmentationWithHeuristic(segmentations);

        for (Segmentation segmentation : segmentationScores.keySet()) {
            segments.addAll(segmentation.getSegments());
        }

        return segments;
    }
}
