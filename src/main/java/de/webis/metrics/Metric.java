package de.webis.metrics;

import de.webis.datastructures.persistent.PersistentStore;

import java.io.IOException;

public abstract class Metric {
    protected PersistentStore<String, Double> storage;

    protected Metric(String storageDir){
        storage = new PersistentStore<>(storageDir);

        if(storage.isEmpty()){
            try {
                calculate();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected abstract void calculate() throws IOException;

    public double get(String entity, String mention){
        Double value = storage.get(mention + "->" + entity);

        if(value == null){
            return 0.0;
        }

        return value;
    }

    public void close(){
        if(storage != null){
            storage.close();
        }
    }

    public static double cosineSimilarity(double[] docVector1, double[] docVector2) {
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;
        double cosineSimilarity = 0.0;

        if (docVector1.length != docVector2.length)
            throw new RuntimeException();

        for (int i = 0; i < docVector1.length; i++) {
            dotProduct += docVector1[i] * docVector2[i];
            magnitude1 += Math.pow(docVector1[i], 2);
            magnitude2 += Math.pow(docVector2[i], 2);
        }

        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);

        if (magnitude1 != 0.0 && magnitude2 != 0.0) {
            cosineSimilarity = dotProduct / (magnitude1 * magnitude2);
        } else {
            return 0.0;
        }
        return cosineSimilarity;
    }
}
