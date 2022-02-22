package de.webis.annotator.ner;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.comprehend.AmazonComprehend;
import com.amazonaws.services.comprehend.AmazonComprehendClientBuilder;
import com.amazonaws.services.comprehend.model.DetectEntitiesRequest;
import com.amazonaws.services.comprehend.model.DetectEntitiesResult;
import com.amazonaws.services.comprehend.model.Entity;
import de.webis.annotator.EntityAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;

import java.util.HashSet;
import java.util.Set;

public class AmazonComprohendEntityDetector implements EntityAnnotator {
    private static AmazonComprehend comprehendClient;

    public AmazonComprohendEntityDetector(){
        System.setProperty("aws.accessKeyId", "AKIAIAHFIMIO7DJJKPDQ");
        System.setProperty("aws.secretKey", "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");
        AWSCredentialsProvider awsCredentials = DefaultAWSCredentialsProviderChain.getInstance();

        comprehendClient = AmazonComprehendClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion("eu-central-1")
                .build();

    }

    @Override
    public Set<EntityAnnotation> annotate(Query query) {
        Set<EntityAnnotation> annotations = new HashSet<>();

        DetectEntitiesRequest detectEntitiesRequest = new DetectEntitiesRequest().withText(query.getText()).withLanguageCode("en");
        DetectEntitiesResult detectEntitiesResult = comprehendClient.detectEntities(detectEntitiesRequest);

        for(Entity entity: detectEntitiesResult.getEntities()){
            EntityAnnotation annotation = new EntityAnnotation();
            annotation.setBegin(entity.getBeginOffset());
            annotation.setEnd(entity.getEndOffset());
            annotation.setMention(entity.getText());
            annotation.setScore(entity.getScore());

            annotations.add(annotation);
        }

        return annotations;
    }

    @Override
    public String getAnnotationTag() {
        return "amazon-comprehend";
    }
}
