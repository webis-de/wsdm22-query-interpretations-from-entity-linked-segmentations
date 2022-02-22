package de.webis.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WikiPageIDResolver {
    private URIBuilder uriBuilder;
    private List<NameValuePair> defaultParams;

    private ObjectMapper jsonMapper;

    public WikiPageIDResolver(){
        uriBuilder = new URIBuilder();

        uriBuilder
                .setScheme("https")
                .setHost("en.wikipedia.org")
                .setPath("/w/api.php");

        defaultParams = new ArrayList<>();
        defaultParams.add(new BasicNameValuePair("action", "query"));
        defaultParams.add(new BasicNameValuePair("prop", "info"));
        defaultParams.add(new BasicNameValuePair("inprop", "url"));
        defaultParams.add(new BasicNameValuePair("format", "json"));

        jsonMapper = new ObjectMapper();
    }

    public String resolvePageID(String pageID){
        uriBuilder.addParameters(defaultParams);
        uriBuilder.addParameter("pageids", pageID);

        URL url = null;

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

                    JsonNode node = jsonMapper.readValue(content.toString(), JsonNode.class);
                    node = node.get("query").get("pages").get(pageID);

                    if(node.has("missing")){
                        return null;
                    }

                    if(node.has("canonicalurl")){
                        return node.get("canonicalurl").asText();
                    }
                    else{
                        System.out.println(content.toString());
                        return node.get("fullurl").asText();
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        uriBuilder.clearParameters();
        return null;
    }

    public static void main(String[] args) {
        WikiPageIDResolver wikiPageIDResolver = new WikiPageIDResolver();
        System.out.println(wikiPageIDResolver.resolvePageID("31389"));
    }
}
