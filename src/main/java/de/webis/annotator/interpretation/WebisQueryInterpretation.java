package de.webis.annotator.interpretation;

import com.google.common.collect.Sets;
import de.webis.annotator.InterpretationAnnotator;
import de.webis.annotator.exer.WebisExplicitEntityRetriever;
import de.webis.annotator.exer.strategies.AllNGrams;
import de.webis.annotator.segmentation.WebisQuerySegmentation;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.InterpretationAnnotation;
import de.webis.datastructures.Query;
import de.webis.datastructures.persistent.LuceneIndex;
import de.webis.datastructures.persistent.PersistentStore;
import de.webis.metrics.EntityCommonness;
import de.webis.metrics.Metric;
import de.webis.query.segmentation.core.Segmentation;
import de.webis.query.segmentation.strategies.StrategyWtBaseline;
import de.webis.utils.StreamSerializer;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class WebisQueryInterpretation implements InterpretationAnnotator {
    private final WebisQuerySegmentation querySegmentation;
    private static ExecutorService executorService;
    private static LuceneIndex luceneIndex;

    private Metric commonness;
    private final PersistentStore<String, double[]> embeddingsStorage;

    private double alpha, beta, gamma;

    private final int numThreads = 8;

    private final static Pattern WIKI_URL_PATTERN = Pattern.compile("http(s)?://en.wikipedia.org/wiki/");

    public WebisQueryInterpretation() {
        luceneIndex = new LuceneIndex("./data/persistent/lucene-entity-index");

        embeddingsStorage = new PersistentStore<>("./data/persistent/embeddings/enwiki_500d_db");
        embeddingsStorage.setSerializer(StreamSerializer.class);
        commonness = EntityCommonness.getInstance();

        querySegmentation = new WebisQuerySegmentation(new StrategyWtBaseline(), 0.66);
//        querySegmentation = new WebisQuerySegmentation(new StrategyWikiBased(), 0.7);

        alpha = 1.0;
        beta = 1.0;
        gamma = 1.0;
    }

    @Override
    public Set<InterpretationAnnotation> annotate(Query query, Set<EntityAnnotation> entityAnnotations) {
        Map<Segmentation, Integer> segmentationScores = querySegmentation.getSegmentations(query);

        Map<String, Set<EntityAnnotation>> mentionEntityAnnoMap = new HashMap<>();
        Map<String, Set<String>> mentionEntityMap = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : luceneIndex.get(query).entrySet()) {
            for (String entity : entry.getValue()) {
                entityAnnotations.add(new EntityAnnotation(0, query.getText().length() - 1, query.getText(), entity));
            }
        }

        for (EntityAnnotation entityAnnotation : entityAnnotations) {
            mentionEntityAnnoMap.putIfAbsent(entityAnnotation.getMention(),
                    new TreeSet<>((a1, a2) -> a1.getScore() < a2.getScore() ? 1 : -1));


            String entity = entityAnnotation.getUrl();

            entity = WIKI_URL_PATTERN.matcher(entity).replaceAll("");
            entity = entity.replace("_", " ").toLowerCase();
            entityAnnotation.setScore(commonness.get(entity, entityAnnotation.getMention()));

            if (entityAnnotation.getScore() > 0) {
                mentionEntityAnnoMap.get(entityAnnotation.getMention()).add(entityAnnotation);
            }

        }

        for (Map.Entry<String, Set<EntityAnnotation>> entry : mentionEntityAnnoMap.entrySet()) {
            mentionEntityMap.putIfAbsent(entry.getKey(), new LinkedHashSet<>());
            int limit = 1;

            for (EntityAnnotation annotation : entry.getValue()) {
                if (mentionEntityMap.get(entry.getKey()).size() < limit) {
                    mentionEntityMap.get(entry.getKey()).add(annotation.getUrl());
                } else {
                    break;
                }
            }

            mentionEntityMap.get(entry.getKey()).add(entry.getKey());
        }

//        TreeSet<InterpretationAnnotation> interpretations = new TreeSet<>(
//                (a1, a2) -> a1.getScore() < a2.getScore() ? 1 : -1
//        );

        Set<InterpretationAnnotation> interpretations = new LinkedHashSet<>();

        for (Map.Entry<Segmentation, Integer> entry : segmentationScores.entrySet()) {
            Segmentation segmentation = entry.getKey();
            List<Set<String>> interpretationCandidates = new LinkedList<>();


            segmentation.getSegments().forEach(s -> interpretationCandidates.add(
                    mentionEntityMap.getOrDefault(s, new HashSet<>(Collections.singletonList(s)))));

            Queue<List<String>> combinations = new ConcurrentLinkedQueue<>(
                    Sets.cartesianProduct(interpretationCandidates));
            Set<InterpretationAnnotation> annotations = createAnnotations(combinations, segmentation);
            interpretations.addAll(annotations);
        }

        if (interpretations.isEmpty()) {
            for (Map.Entry<Segmentation, Integer> segmention : segmentationScores.entrySet()) {
                interpretations.add(new InterpretationAnnotation(segmention.getKey().getSegments()));
            }
        }
        System.out.println("Interpretations: " + interpretations.size());

        List<InterpretationAnnotation> sortedInterpretations = new LinkedList<>(interpretations);
        sortedInterpretations.sort(Comparator.comparingDouble(InterpretationAnnotation::getRelevance).reversed());
        Set<InterpretationAnnotation> results = new LinkedHashSet<>();
        for(InterpretationAnnotation annotation: sortedInterpretations){
            if(results.size() < 20){
                results.add(annotation);
            }
        }
        return results;

//        return interpretations;
    }

    public void shutdown() {
        executorService.shutdown();
    }

    private Set<InterpretationAnnotation> createAnnotations(Queue<List<String>> interpretations, Segmentation segmentation) {
        executorService = Executors.newFixedThreadPool(numThreads);
        Set<InterpretationAnnotation> annotations = new CopyOnWriteArraySet<>();
        LongSummaryStatistics commonnessTime = new LongSummaryStatistics();
        LongSummaryStatistics relatednessTime = new LongSummaryStatistics();
        LongSummaryStatistics contextTime = new LongSummaryStatistics();

        Runnable scoringRunnable = () -> {
            while (!interpretations.isEmpty()) {
                List<String> interpretation = interpretations.poll();

                if (interpretation == null) {
                    break;
                }

                InterpretationAnnotation annotation = new InterpretationAnnotation(interpretation);

                long start = System.currentTimeMillis();
                double avgCommonness = getAvgCommonness(annotation, segmentation);
                commonnessTime.accept(System.currentTimeMillis() - start);
                start = System.currentTimeMillis();
                double avgRelatedness = getAvgRelatedness(annotation);
                relatednessTime.accept(System.currentTimeMillis() - start);
                start = System.currentTimeMillis();
                double avgContextScore = getAvgContextScore(annotation);
                contextTime.accept(System.currentTimeMillis() - start);

                annotation.setRelevance(
                        alpha * avgCommonness + beta * avgRelatedness + gamma * avgContextScore);

                if (annotation.getRelevance() > 0.0) {
                    annotations.add(annotation);
                }
            }

        };

        for (int i = 0; i < numThreads; i++) {
            executorService.execute(scoringRunnable);
        }

        executorService.shutdown();

        try {
            executorService.awaitTermination(1L, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            executorService.shutdown();
            e.printStackTrace();
        }

        return annotations;
    }

    private double getAvgCommonness(InterpretationAnnotation interpretation, Segmentation segmentation) {
        List<String> containedEntities = interpretation.getContainedEntities();
        List<String> segments = segmentation.getSegments();
        DoubleSummaryStatistics commonnessStats = new DoubleSummaryStatistics();

        for (String entity : containedEntities) {
            int indexOfEntity = interpretation.getInterpretation().indexOf(entity);

            entity = WIKI_URL_PATTERN.matcher(entity).replaceAll("");
            entity = entity.replace("_", " ").toLowerCase();

            commonnessStats.accept(commonness.get(entity, segments.get(indexOfEntity)));
        }

        return commonnessStats.getAverage();
    }

    private double getAvgRelatedness(InterpretationAnnotation interpretation) {
        List<String> containedEntities = interpretation.getContainedEntities();
        DoubleSummaryStatistics relatednessStats = new DoubleSummaryStatistics();

        if(containedEntities.size() <= 1){
            return getAvgContextScore(interpretation);
        }

        for (int i = 0; i < containedEntities.size(); i++) {
            String firstEntity = containedEntities.get(i);
            firstEntity = WIKI_URL_PATTERN.matcher(firstEntity).replaceAll("ENTITY/");

            double[] firstEntityVector = embeddingsStorage.getOrDefault(firstEntity, new double[500]);

            for (int j = 0; j < containedEntities.size(); j++) {
                if (i != j) {
                    String secondEntity = containedEntities.get(j);
                    secondEntity = WIKI_URL_PATTERN.matcher(secondEntity).replaceAll("ENTITY/");

                    double[] secondEntityVector = embeddingsStorage.getOrDefault(secondEntity, new double[500]);

                    relatednessStats.accept(Metric.cosineSimilarity(firstEntityVector, secondEntityVector));
                }
            }
        }

        return relatednessStats.getAverage();
    }

    private double getAvgContextScore(InterpretationAnnotation interpretation) {
        List<String> entities = interpretation.getContainedEntities();
        DoubleSummaryStatistics contextScoreStats = new DoubleSummaryStatistics();

        if (entities.isEmpty()) {
            return 0.0;
        }

        Set<String> contextWords = interpretation.getContextWords();

        if (contextWords.isEmpty()) {
            return 0.0;
        }

        for (String entity : entities) {
            for (String contextWord : contextWords) {
                entity = WIKI_URL_PATTERN.matcher(entity).replaceAll("ENTITY/");

                double[] entityVector = embeddingsStorage.getOrDefault(entity, new double[500]);
                double[] contextVector = embeddingsStorage.getOrDefault(contextWord, new double[500]);

                contextScoreStats.accept(Metric.cosineSimilarity(entityVector, contextVector));
            }
        }

        return contextScoreStats.getAverage();
    }

    public void close() {
        commonness.close();
        embeddingsStorage.close();
    }

    public void setParameter(double alpha, double beta, double gamma) {
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
    }

    public static void main(String[] args) {
        WebisQueryInterpretation webisQueryInterpretation = new WebisQueryInterpretation();

        Query query = new Query("new york times square dance");
        Set<InterpretationAnnotation> interpretations = webisQueryInterpretation.annotate(query,
                new WebisExplicitEntityRetriever(new AllNGrams()).annotate(query));

        System.out.println(interpretations);
        webisQueryInterpretation.shutdown();
        webisQueryInterpretation.close();
    }
}
