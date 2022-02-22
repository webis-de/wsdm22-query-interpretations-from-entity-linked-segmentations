package de.webis.annotator;

import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public abstract class HttpGetAnnotator extends HttpAnnotator {


    public HttpGetAnnotator(String scheme, String host, int port, String path){
        super(scheme, host, port, path);
    }

    public HttpGetAnnotator(String scheme, String host, int port, String path, String logPath){
        super(scheme, host, port, path, logPath);
    }

    @Override
    public Set<EntityAnnotation> annotate(Query query) {
        Set<EntityAnnotation> annotations = new HashSet<>();

        if (logger != null)
        if (logger.contains(query.getText())){
            try {
                extractAnnotations(query, logger.get(query.getText()), annotations);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return annotations;
        }

        URL url = null;

        prepareRequest(query);
        uriBuilder.addParameters(defaultParams);
        try {
            url = uriBuilder.build().toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            e.printStackTrace();
        }

        if (url != null){
            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setUseCaches(false);
                connection.setDoOutput(true);

                int status = connection.getResponseCode();

                if(status == 200) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }

                    in.close();

                    extractAnnotations(query, content.toString(), annotations);

                    if (logger != null)
                        logger.put(query.getText(), content.toString());

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        uriBuilder.clearParameters();

        return annotations;
    }




}
