package de.webis.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.EntityAnnotator;
import de.webis.annotator.InterpretationAnnotator;
import de.webis.annotator.LoggedAnnotator;
import de.webis.annotator.exer.WebisExplicitEntityRetriever;
import de.webis.annotator.exer.strategies.AllNGrams;
import de.webis.annotator.exer.strategies.HeuristicSegmentation;
import de.webis.annotator.interpretation.WebisQueryInterpretation;
import de.webis.datastructures.*;
import de.webis.parser.CorpusStreamReader;
import de.webis.parser.CorpusType;
import de.webis.parser.WebisJsonStreamReader;
import de.webis.query.segmentation.strategies.StrategyWtBaseline;

import java.io.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

enum Task {EXER, IMER}

public class Evaluator {
    public static MatchType MATCHTYPE = MatchType.PM;

    public static void evaluateER(EntityAnnotator annotator, CorpusStreamReader reader, Task task) {
        EvaluationStatistics statistics = new EvaluationStatistics();

        Set<Annotation> overallRetrievedAnnotations = new LinkedHashSet<>();
        Set<Annotation> overallRelevantAnnotations = new LinkedHashSet<>();

        BufferedWriter annotationWriter = null;
        ObjectMapper mapper = new ObjectMapper();
        File outFile = new File("data/out/webis-qinc-22/evaluation/el/");
        if(!outFile.exists()){
            outFile.mkdirs();
        }

        try {
            annotationWriter = new BufferedWriter(
                    new FileWriter(
                            "data/out/webis-qinc-22/evaluation/el/"
                                    + annotator.getAnnotationTag() + ".jsonl"
                    )
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (reader.hasNext()) {
            Map<String, Object> annotationData = new LinkedHashMap<>();
            Query query = reader.next();

            annotationData.put("id", query.getId());
            annotationData.put("query", query.getText());
            annotationData.put("timestamp", Timestamp.from(Instant.now()).toString());
            annotationData.put("retrieved-entities", new LinkedList<EntityAnnotation>());
            annotationData.put("scores", new LinkedHashMap<String, Double>());
            long start = System.currentTimeMillis();

            Set<EntityAnnotation> retrievedAnnotations = annotator.annotate(query);
            double runtime = (double) (System.currentTimeMillis() - start);
            statistics.accept("runtime", runtime);
            annotationData.put("runtime", runtime);

            Set<Annotation> relevantAnnotations = getRelevantEntities(query, task);
            ((LinkedList<EntityAnnotation>)annotationData.get("retrieved-entities")).addAll(retrievedAnnotations);
            System.out.println(query.getText());
            if (!retrievedAnnotations.isEmpty()
                && !retrievedAnnotations.iterator().next().hasUrl()) {

                for (Annotation retrievedAnnotation : retrievedAnnotations) {
                    EntityAnnotation retrievedAnno = (EntityAnnotation) retrievedAnnotation;

                    for (Annotation relevantAnnotation : relevantAnnotations) {
                        EntityAnnotation relevantAnno = (EntityAnnotation) relevantAnnotation;

                        if (retrievedAnno.getMention().equals(relevantAnno.getMention())) {
                            retrievedAnno.setUrl(relevantAnno.getUrl());
                            break;
                        }
                    }

                }

                retrievedAnnotations = new LinkedHashSet<>(retrievedAnnotations);
            }

            overallRetrievedAnnotations.addAll(retrievedAnnotations);
            overallRelevantAnnotations.addAll(relevantAnnotations);

            double macroPrecision = getPrecision(retrievedAnnotations, relevantAnnotations);
            double macroRecall = getRecall(retrievedAnnotations, relevantAnnotations);
            double macroF1 = 2 * macroPrecision * macroRecall / (macroPrecision + macroRecall);

            double wRecall = getWRecall(retrievedAnnotations, relevantAnnotations);
            double wF1 = 2.0 * macroPrecision * wRecall / (macroPrecision + wRecall);

            if (Double.isNaN(macroF1)) {
                macroF1 = 0;
            }

            if (Double.isNaN(wF1)) {
                wF1 = 0;
            }

            ((Map<String, Double>)annotationData.get("scores")).put("precision", macroPrecision);
            ((Map<String, Double>)annotationData.get("scores")).put("recall", macroRecall);
            ((Map<String, Double>)annotationData.get("scores")).put("f1", macroF1);
            ((Map<String, Double>)annotationData.get("scores")).put("weighted-recall", wRecall);
            ((Map<String, Double>)annotationData.get("scores")).put("weighted-f1f1", wF1);

            try {
                annotationWriter.write(mapper.writeValueAsString(annotationData));
                annotationWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!relevantAnnotations.isEmpty()) {
                statistics.accept("ep-precision", macroPrecision);
                statistics.accept("ep-recall", macroRecall);
                statistics.accept("ep-f1", macroF1);
                statistics.accept("ep-w-recall", wRecall);
                statistics.accept("ep-w-f1", wF1);
            }

            System.out.println("macro-precision: " + macroPrecision);
            System.out.println("macro-recall: " + macroRecall);
            System.out.println("macro-f1: " + macroF1);

            statistics.accept("macro-precision", macroPrecision);
            statistics.accept("macro-recall", macroRecall);
            statistics.accept("macro-f1", macroF1);

            statistics.accept("macro-w-recall", wRecall);
            statistics.accept("macro-w-f1", wF1);

            System.out.println("--------------------");
        }

        if (annotator instanceof LoggedAnnotator) {
            ((LoggedAnnotator) annotator).close();
        }

        double microPrecision = getPrecision(overallRetrievedAnnotations, overallRelevantAnnotations);
        double microRecall = getRecall(overallRetrievedAnnotations, overallRelevantAnnotations);
        double microWRecall = getWRecall(overallRetrievedAnnotations, overallRelevantAnnotations);
        double microF1 = 2 * microPrecision * microRecall / (microPrecision + microRecall);
        double wF1 = 2.0 * microPrecision * microWRecall / (microPrecision + microWRecall);

        statistics.accept("micro-precision", microPrecision);
        statistics.accept("micro-recall", microRecall);
        statistics.accept("micro-w-recall", microWRecall);
        statistics.accept("micro-w-f1", wF1);
        statistics.accept("micro-f1", microF1);

        try {
            annotationWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(statistics);
    }

    public static EvaluationStatistics evaluateInterpretation(InterpretationAnnotator interpretationAnnotator,
                                                              EntityAnnotator entityAnnotator,
                                                              CorpusStreamReader corpusStreamReader,
                                                              MatchType type) {

        MATCHTYPE = type;
        EvaluationStatistics statistics = new EvaluationStatistics();

        Map<String, Set<String>> concepts = null;

        try {
            concepts = loadConcepts();
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedWriter annotationWriter = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            annotationWriter = new BufferedWriter(
                    new FileWriter(
                            "data/corpora/corpus-webis-query-interpretation/final/evaluation/interpretation/"
                                    + entityAnnotator.getAnnotationTag() + "-"
                                    + interpretationAnnotator.getClass().getSimpleName().toLowerCase() + ".jsonl"
                    )
            );
        } catch (IOException e) {
            e.printStackTrace();
        }


        while (corpusStreamReader.hasNext()) {
            Query query = corpusStreamReader.next();

            Map<String, Object> annotationData = new LinkedHashMap<>();
            annotationData.put("id", query.getId());
            annotationData.put("query", query.getText());
            annotationData.put("timestamp", Timestamp.from(Instant.now()).toString());

            System.out.println("Annotate \"" + query.getText() + "\"...");

            long start = System.currentTimeMillis();
            Set<EntityAnnotation> entityAnnotations = entityAnnotator.annotate(query);
            Set<Annotation> retrievedAnnotations =
                    new LinkedHashSet<>(interpretationAnnotator.annotate(query, entityAnnotations));
            statistics.accept("runtime", (double) (System.currentTimeMillis() - start));
            annotationData.put("runtime", System.currentTimeMillis() - start);


            if (concepts != null)
                for (Annotation annotation : retrievedAnnotations) {
                    if (annotation instanceof InterpretationAnnotation) {
                        Set<String> conceptLinks = new HashSet<>(concepts.keySet());
                        List<String> interpretation = new LinkedList<>(((InterpretationAnnotation) annotation).getInterpretation());

                        conceptLinks.retainAll(interpretation);

                        if (!conceptLinks.isEmpty()) {
                            for (int i = 0; i < interpretation.size(); i++) {
                                if (conceptLinks.contains(interpretation.get(i))) {
                                    for (String conceptTerm : concepts.get(interpretation.get(i))) {
                                        if (query.getText().contains(conceptTerm)) {
                                            interpretation.set(i, conceptTerm);
                                            ((InterpretationAnnotation) annotation).setInterpretation(interpretation);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            retrievedAnnotations = new LinkedHashSet<>(retrievedAnnotations);
            annotationData.put("interpretations", retrievedAnnotations);
            annotationData.put("scores", new HashMap<>());

            Set<Annotation> desiredAnnotations = null;
            if (query instanceof WebisCorpusQuery) {
                desiredAnnotations =
                        new LinkedHashSet<>(((WebisCorpusQuery) query).getInterpretations());
            }

            double macroPrecision = getPrecision(retrievedAnnotations, desiredAnnotations);
            double macroRecall = getRecall(retrievedAnnotations, desiredAnnotations);
            double macroWRecall = getWRecall(retrievedAnnotations, desiredAnnotations);
            double macroF1 = 2 * macroPrecision * macroRecall / (macroPrecision + macroRecall);

            if (Double.isNaN(macroF1)) {
                macroF1 = 0;
            }

            ((Map<String, Double>)annotationData.get("scores")).put("recall", macroRecall);
            ((Map<String, Double>)annotationData.get("scores")).put("weighted-recall", macroWRecall);
            ((Map<String, Double>)annotationData.get("scores")).put("precision", macroPrecision);
            ((Map<String, Double>)annotationData.get("scores")).put("f1", macroF1);

            statistics.accept("macro-precision", macroPrecision);
            statistics.accept("macro-recall", macroRecall);
            statistics.accept("macro-w-recall", macroWRecall);
            statistics.accept("macro-f1", macroF1);

            System.out.println("macro-precision: " + macroPrecision);
            System.out.println("macro-recall: " + macroRecall);
            System.out.println("macro-f1: " + macroF1);
            System.out.println("--------------------");

            try {
                annotationWriter.write(mapper.writeValueAsString(annotationData));
                annotationWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (entityAnnotator instanceof LoggedAnnotator) {
            ((LoggedAnnotator) entityAnnotator).close();
        }

        try {
            assert annotationWriter != null;
            annotationWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(entityAnnotator.getClass().getName() + "|" + interpretationAnnotator.getClass().getName() + "|" + type.name());
        System.out.println(statistics);

        return statistics;
    }

    private static double getPrecision(Set<? extends Annotation> retrieved, Set<? extends Annotation> relevant) {
        if (retrieved.size() == 0 && relevant.size() == 0) {
            return 1.0;
        } else if (retrieved.size() == 0) {
            return 0.0;
        } else {
            Set<Annotation> intersection = new HashSet<>(relevant);
            intersection.retainAll(retrieved);

            return (double) (intersection.size()) / (double) (retrieved.size());
        }
    }

    private static double getRecall(Set<? extends Annotation> retrieved, Set<? extends Annotation> relevant) {
        if (retrieved.size() == 0 && relevant.size() == 0) {
            return 1.0;
        } else if (relevant.size() == 0) {
            return 0.0;
        } else {
            Set<Annotation> intersection = new HashSet<>(relevant);
            intersection.retainAll(retrieved);

            return (double) (intersection.size()) / (double) (relevant.size());
        }
    }

    private static double getWRecall(Set<? extends Annotation> retrieved, Set<? extends Annotation> relevant) {
        if (retrieved.size() == 0 && relevant.size() == 0) {
            return 1.0;
        } else if (relevant.size() == 0) {
            return 0.0;
        } else {
            int relevantDiff = (int) relevant.stream().collect(
                    Collectors.summarizingDouble(Annotation::getScore))
                    .getSum();

            Set<Annotation> intersection = new HashSet<>(relevant);
            intersection.retainAll(retrieved);

            int foundDiff = (int) intersection.stream().collect(
                    Collectors.summarizingDouble(Annotation::getScore))
                    .getSum();

            return (double) (foundDiff) / (double) (relevantDiff);
        }
    }

    private static Set<Annotation> getRelevantEntities(Query query, Task task) {
        if (query instanceof WebisCorpusQuery) {
            if (task == Task.EXER) {
                return new HashSet<>(((WebisCorpusQuery) query).getExplicitEntities());
            } else if (task == Task.IMER) {
                return new HashSet<>(((WebisCorpusQuery) query).getImplicitEntities());
            }
        }

        return new HashSet<>(query.getAnnotations());
    }

    private static Map<String, Set<String>> loadConcepts() throws IOException {
        Map<String, Set<String>> concepts = new HashMap<>();

        BufferedReader conceptReader = new BufferedReader(
                new FileReader("./data/annotations/webis-not-found-interpretations-concepts.csv")
        );

        String line;

        while ((line = conceptReader.readLine()) != null) {
            String[] attribs = line.split(",");
            String url = attribs[1].trim();

            concepts.putIfAbsent(url, new HashSet<>());
            concepts.get(url).add(attribs[0].trim());
        }

        conceptReader.close();
        return concepts;
    }

    public static void main(String[] args) {
        /* EXPLICIT ENTITY RECOGNITION EVALUATION **/

//        Evaluator.evaluateER(new BaselineAnnotator(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);

        Evaluator.evaluateER(new WebisExplicitEntityRetriever(new AllNGrams()),
                new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);

//        Evaluator.evaluateER(new AmbiverseNLU(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new BabelfyEntityLinker(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new DandelionEntityExtractor(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new DexterEntityLinking(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new FalconEntityLinker(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new NordlysEntityLinker(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new NordlysEntityRetriever(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new RadboudEntityLinker(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new SmaphEntityLinker(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new TagMeEntityAnnotator(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new TextRazorEntityLinker(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new YahooFastEntityLinker(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);

//        Evaluator.evaluateER(new AmazonComprohendEntityDetector(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new DeepPavlovNER(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new FlairNLPTagger(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new MITIEEntityRecognizer(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new LingPipeNER(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new OpenNLPEntityLinker(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);
//        Evaluator.evaluateER(new StanfordNERTagger(), new WebisJsonStreamReader(CorpusType.TEST), Task.EXER);

        /* INTERPRETATION EVALUATION **/

//        Evaluator.evaluateInterpretation(
//                new WebisQueryInterpretation(),
//                new WebisExplicitEntityRetriever(
//                        new HeuristicSegmentation(
//                                new StrategyWtBaseline(),
//                                0.85)),
//                new WebisJsonStreamReader(CorpusType.TEST),
//                MatchType.PM
//        );

//        Evaluator.evaluateInterpretation(new HasibiGIF(), new AmbiverseNLU(), new WebisJsonStreamReader(CorpusType.TEST), MatchType.CM);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new BabelfyEntityLinker(), new WebisJsonStreamReader(CorpusType.TEST), MatchType.CM);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new DandelionEntityExtractor(), new WebisJsonStreamReader(CorpusType.TEST), MatchType.CM);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new DexterEntityLinking(), new WebisJsonStreamReader(CorpusType.TEST), MatchType.CM);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new NordlysEntityLinker(), new WebisJsonStreamReader(CorpusType.TEST), MatchType.CM);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new SmaphEntityLinker(), new WebisJsonStreamReader(CorpusType.TEST), MatchType.CM);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new TagMeEntityAnnotator(), new WebisJsonStreamReader(CorpusType.TEST), MatchType.CM);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new TextRazorEntityLinker(), new WebisJsonStreamReader(CorpusType.TEST), MatchType.CM);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new YahooFastEntityLinker(), new WebisJsonStreamReader(CorpusType.TEST), MatchType.CM);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new RadboudEntityLinker(), new WebisJsonStreamReader(CorpusType.TEST), MatchType.CM);




    }
}
