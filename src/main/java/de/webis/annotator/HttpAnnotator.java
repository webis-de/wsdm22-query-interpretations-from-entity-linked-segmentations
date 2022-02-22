package de.webis.annotator;

import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;
import de.webis.datastructures.persistent.PersistentStore;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class HttpAnnotator implements LoggedAnnotator{
    protected URIBuilder uriBuilder;

    protected List<NameValuePair> defaultParams;

    protected PersistentStore<String, String> logger;


    public HttpAnnotator(String scheme, String host, int port, String path){
        uriBuilder = new URIBuilder();

        uriBuilder
                .setScheme(scheme)
                .setHost(host)
                .setPort(port)
                .setPath(path);

        defaultParams = new ArrayList<>();
    }

    public HttpAnnotator(String scheme, String host, int port, String path, String logPath){
        uriBuilder = new URIBuilder();

        uriBuilder
                .setScheme(scheme)
                .setHost(host)
                .setPort(port)
                .setPath(path);

        defaultParams = new ArrayList<>();
        logger = new PersistentStore<>(logPath);
    }

    @Override
    public void close() {
        if (logger != null) {
            logger.close();
        }
    }

    protected abstract void extractAnnotations(Query query, String response, Set<EntityAnnotation> annotations)
            throws IOException;


    protected abstract void prepareRequest(Query query);
}
