package de.webis.annotator;

import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

public abstract class HttpPostAnnotator extends HttpAnnotator {
    protected EntityBuilder entityBuilder;

    public HttpPostAnnotator(String scheme, String host, int port, String path) {
        super(scheme, host, port, path);
    }

    public HttpPostAnnotator(String scheme, String host, int port, String path, String logPath) {
        super(scheme, host, port, path, logPath);
    }

    @Override
    public Set<EntityAnnotation> annotate(Query query) {
        Set<EntityAnnotation> annotations = new HashSet<>();

        HttpClient client = HttpClients.createDefault();
        uriBuilder.addParameters(defaultParams);

        HttpPost request = null;
        try {
            request = new HttpPost(uriBuilder.build());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        if (request != null) {
            request.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
            request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());

            entityBuilder = EntityBuilder.create();
            entityBuilder.setContentType(ContentType.APPLICATION_JSON);
            prepareRequest(query);

            request.setEntity(entityBuilder.build());

            HttpResponse response;
            try {
                response = client.execute(request);

                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
                    StringBuilder responseString = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        responseString.append(line).append("\n");
                    }

                    reader.close();
                    extractAnnotations(query, responseString.toString(), annotations);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        uriBuilder.clearParameters();

        return annotations;
    }
}
