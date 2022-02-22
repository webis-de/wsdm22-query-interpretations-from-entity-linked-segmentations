package de.webis.datastructures;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class EntityAnnotationFileHandle {
    private final String filePath;

    private BufferedWriter annotationOut;

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private double runtime;

    public EntityAnnotationFileHandle(String query, String annotationTag) {
        this.filePath = "./data/annotations/"+annotationTag+"/"+query.replaceAll("[\\s]+","-")+".ldjson";

        File file = new File(filePath);

        boolean dirExists = true;
        if(!file.getParentFile().exists()){
            dirExists = file.getParentFile().mkdirs();
        }

        try {
            if(!dirExists){
                throw new IOException("Can't create dir \"" + file.getParentFile() + "\"");
            }

            annotationOut = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }

        runtime = -1;
    }

    public void writeAnnotation(EntityAnnotation annotation) throws IOException {
        if(annotationOut != null){
            annotationOut.write(jsonMapper.writeValueAsString(annotation)+"\n");
        }
    }

    public List<EntityAnnotation> loadAnnotations() throws IOException {
        List<EntityAnnotation> annotations = new ArrayList<>();


        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;

        while((line = reader.readLine()) != null){
            annotations.add(jsonMapper.readValue(line, EntityAnnotation.class));
        }

        reader.close();

        return annotations;
    }

    public void prepareAnnotations(int maxAnnotations){
        List<EntityAnnotation> annotations = null;
        try {
            annotations = loadAnnotations();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert annotations != null;
        annotations = sortAnnotations(annotations);

        Set<String> urls = new HashSet<>();

        try {
            annotationOut = new BufferedWriter(new FileWriter(filePath));
            int numAnnotations = 0;
            for(EntityAnnotation annotation: annotations){
                if(!urls.contains(annotation.getUrl())){
                    writeAnnotation(annotation);
                    numAnnotations++;

//                    if(annotation.getScore() < 0.75){
//                        break;
//                    }

                    urls.add(annotation.getUrl());
                }

                if(numAnnotations == maxAnnotations){
                    break;
                }
            }

            annotationOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void prepareAnnotations(){
        prepareAnnotations(-1);
    }

    public void flush(){
        try {
            annotationOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<EntityAnnotation> sortAnnotations(List<EntityAnnotation> annotations){
        return annotations.stream()
                .sorted(Comparator.comparingDouble(EntityAnnotation::getScore).reversed())
                .collect(Collectors.toList());
    }

    public double getRuntime() {
        return runtime;
    }

    public void setRuntime(double runtime) {
        this.runtime = runtime;
    }
}
