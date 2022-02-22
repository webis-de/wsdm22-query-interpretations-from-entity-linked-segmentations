package de.webis.annotator.interpretation;

import de.webis.annotator.InterpretationAnnotator;
import de.webis.annotator.LoggedAnnotator;
import de.webis.annotator.entitylinking.DandelionEntityExtractor;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.InterpretationAnnotation;
import de.webis.datastructures.Query;
import de.webis.parser.CorpusStreamReader;
import de.webis.parser.WebisJsonStreamReader;

import java.io.*;
import java.util.*;

public class HasibiGIF implements InterpretationAnnotator {

    @Override
    public Set<InterpretationAnnotation> annotate(Query query, Set<EntityAnnotation> annotations) {
        PrintWriter writer = null;
        ProcessBuilder processBuilder = new ProcessBuilder();

        try {
            writer = new PrintWriter(new FileWriter("data/annotations/interpretation/tmp.tsv"));
            writer.println("qid\tmention\tfreebase_id\tscore");
        } catch (IOException e) {
            e.printStackTrace();
        }


        if(annotations != null && writer != null){
            for(EntityAnnotation entityAnnotation: annotations){
                writer.println(
                        query.getId() + "\t" + entityAnnotation.getMention() + "\t" + entityAnnotation.getUrl() +
                                "\t" + entityAnnotation.getScore());
            }

            writer.flush();
            writer.close();
        }

//        processBuilder.command("third-party/EntityLinkingInQueries-ELQ/run.sh");
        processBuilder.command(
                "envs/hasibi_gif/bin/python",
                "third-party/EntityLinkingInQueries-ELQ/code/GIF.py",
                "-in" , "data/annotations/interpretation/tmp.tsv",
                "-th", "0");
        Process process = null;

        try {
             process = processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(process != null){
            try {
                int exitVal = process.waitFor();

                if(exitVal == 0){
                    return parseAnnotations(query, annotations);
                } else{
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                    String line;
                    while((line = errorReader.readLine()) != null){
                        System.err.println(line);
                    }

                    errorReader.close();
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }

        return new HashSet<>();
    }

    private Set<InterpretationAnnotation> parseAnnotations(Query query, Set<EntityAnnotation> entities) throws IOException {
        Set<InterpretationAnnotation> annotations = new HashSet<>();

        BufferedReader reader = new BufferedReader(new FileReader("data/annotations/interpretation/tmp-GIF-th0.0.txt"));
        String line;

        while((line = reader.readLine()) != null){
            String[] attribs = line.split("\t");

            InterpretationAnnotation annotation = new InterpretationAnnotation();
            annotation.setId(Integer.parseInt(attribs[1]));

            String queryText = query.getText();

            for(int i=2; i < attribs.length; i++){
                for(EntityAnnotation entityAnnotation: entities){
                    if(entityAnnotation.getUrl().equals(attribs[i])){
//                        queryText = queryText.substring(0, entityAnnotation.getBegin()) + entityAnnotation.getUrl() + queryText.substring(entityAnnotation.getEnd());
                        queryText = queryText.replace(entityAnnotation.getMention(), entityAnnotation.getUrl());
                        break;
                    }
                }
            }

            List<String> interpretation = new LinkedList<>(Arrays.asList(queryText.split(" ")));

            annotation.setInterpretation(interpretation);
            annotations.add(annotation);
        }

        reader.close();
        System.out.println(annotations);
        return annotations;
    }

    public static void main(String[] args) {
        CorpusStreamReader streamReader = new WebisJsonStreamReader();
        LoggedAnnotator entityAnnotator = new DandelionEntityExtractor();
        HasibiGIF hasibiGIF = new HasibiGIF();

        while(streamReader.hasNext()){
            Query query = streamReader.next();
            Set<EntityAnnotation> annotationFileHandle = entityAnnotator.annotate(query);

            hasibiGIF.annotate(query, annotationFileHandle);
        }

        entityAnnotator.close();
    }
}
