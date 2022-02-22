package de.webis.annotator.entitylinking;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.HttpGetAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.Set;

public abstract class NordlysToolkit extends HttpGetAnnotator {
    protected static final ObjectMapper jsonMapper = new ObjectMapper();

    public NordlysToolkit(final String path){
        super("http", "localhost", 8004, path);
        jsonMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        jsonMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        jsonMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    }

    @Override
    protected void extractAnnotations(Query query, String response, Set<EntityAnnotation> annotations) throws IOException {
        this.parseAnnotations(response, annotations);
    }

    @Override
    protected void prepareRequest(Query query) {
        this.defaultParams.clear();
        this.defaultParams.add(new BasicNameValuePair("q", query.getText()));
    }


    protected abstract void parseAnnotations(String jsonString, Set<EntityAnnotation> annotations);

    public static void main(String[] args) {
        NordlysToolkit linker = new NordlysEntityRetriever();

        Set<EntityAnnotation> annotations = linker.annotate(new Query("directed bela glen glenda bride monster plan 9 outer space"));
        System.out.println(annotations);
        linker.close();
    }
}
