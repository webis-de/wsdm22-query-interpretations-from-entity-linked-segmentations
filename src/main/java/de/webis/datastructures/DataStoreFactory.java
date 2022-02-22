package de.webis.datastructures;

import de.webis.datastructures.persistent.LuceneIndex;
import de.webis.datastructures.persistent.PersistentStore;
import de.webis.parser.WikipediaSQLStreamReader;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataStoreFactory {
    private static final String WIKI_PAGES_DUMP = "../wikipedia/enwiki-20200501-page.sql";
    private static final String WIKI_PAGE_PROPS_DUMP = "../wikipedia/enwiki-20200501-page_props.sql";
    private static final String WIKI_REDIRECTS_DUMP = "../wikipedia/enwiki-20200501-redirect.sql";
    private static final String WIKI_PAGELINKS_DUMP = "../wikipedia/enwiki-20200501-pagelinks.sql";

    public static PersistentStore<String, Set<String>> WIKI_ENTITY_INDEX;
    public static LuceneIndex LUCENE_ENTITY_INDEX;

    static{
        initWikiEntityIndex();
        initLuceneEntityIndex();
    }

    private static void initLuceneEntityIndex(){
        LUCENE_ENTITY_INDEX = new LuceneIndex("./data/persistent/lucene-entity-index");

        if(LUCENE_ENTITY_INDEX.isEmpty()){
            final int[] numKeys = {0};
            final long[] start = {System.currentTimeMillis()};

            WIKI_ENTITY_INDEX.forEachKey(key -> {
                if(System.currentTimeMillis() - start[0] > 1000L){
                    System.out.print("\rIndexed keys: " + numKeys[0]);
                    start[0] = System.currentTimeMillis();
                }

                Set<String> values = WIKI_ENTITY_INDEX.get(key);

                LUCENE_ENTITY_INDEX.put(key, values);
                numKeys[0]++;
            });

            LUCENE_ENTITY_INDEX.flush();
        }
    }

    private static void initWikiEntityIndex(){
        WIKI_ENTITY_INDEX = new PersistentStore<>("./data/persistent/wiki-entity-index");

        if(WIKI_ENTITY_INDEX.isEmpty()){
            Map<Long, String> redirectTitles = new HashMap<>();
            Map<String, String> redirectMappings = new HashMap<>();
            Map<Long, String> disambiguationTitles = new HashMap<>();

            WikipediaSQLStreamReader pagePropsReader = new WikipediaSQLStreamReader(WIKI_PAGE_PROPS_DUMP);

            long printTime = System.currentTimeMillis();
            System.out.println("Collect disambiguations...");
            while(pagePropsReader.hasNext()){
                List<List<String>> data = pagePropsReader.next();

                for(List<String> values: data){
                    String property = values.get(1);

                    if(!property.equals("disambiguation")){
                        continue;
                    }

                    if(System.currentTimeMillis() - printTime >= 1000L) {
                        System.out.print("\rFound disambiguations: " + disambiguationTitles.size());
                        printTime = System.currentTimeMillis();
                    }

                    long id = Long.parseLong(values.get(0));

                    disambiguationTitles.put(id, null);
                }
            }

            System.out.println("\rFound disambiguations: " + disambiguationTitles.size());
            pagePropsReader.close();

            printTime = System.currentTimeMillis();
            System.out.println("Collect redirects...");
            WikipediaSQLStreamReader redirectsReader = new WikipediaSQLStreamReader(WIKI_REDIRECTS_DUMP);

            while(redirectsReader.hasNext()){
                List<List<String>> data = redirectsReader.next();

                if(System.currentTimeMillis() - printTime >= 1000L) {
                    System.out.print("\rFound redirects: " + redirectTitles.size());
                    printTime = System.currentTimeMillis();
                }

                for(List<String> values: data){
                    long id = Long.parseLong(values.get(0));
                    String entityTitle = values.get(2);
                    String namespace = values.get(1);

                    if(namespace.equals("0")){
                        redirectTitles.put(id, entityTitle);
                    }

                }
            }

            System.out.println("\rFound redirects: " + redirectTitles.size());
            redirectsReader.close();

            WikipediaSQLStreamReader pagesReader = new WikipediaSQLStreamReader(WIKI_PAGES_DUMP);

            int pageCounter = 0;
            printTime = System.currentTimeMillis();
            System.out.println("\nIndex pages...");
            while(pagesReader.hasNext()){
                List<List<String>> data = pagesReader.next();

                for(List<String> values: data){
                    if(System.currentTimeMillis() - printTime > 1000L){
                        System.out.print("\rIndexed pages: " + pageCounter);
                        printTime = System.currentTimeMillis();
                    }
                    pageCounter++;

                    String title = values.get(WikiPage.TITLE_IDX);
                    String namespace = values.get(WikiPage.NAMESPACE_IDX);
                    boolean isRedirect = values.get(WikiPage.REDIRECT_IDX).equals("1");

                    long id = Long.parseLong(values.get(WikiPage.ID_IDX));

                    if(!namespace.equals("0")){
                        continue;
                    }

                    if(disambiguationTitles.containsKey(id)){
                        disambiguationTitles.put(id, title);
                        continue;
                    }

                    if(WikiPage.DISAMBIGUATION_PATTERN.matcher(title).matches()){
                        disambiguationTitles.put(id, title);
                        continue;
                    }

                    String entity;

                    if(redirectTitles.containsKey(id)){
                        entity = WikiPage.PREFIX + redirectTitles.get(id);
                        redirectMappings.put(title, entity);
                    }
                    else{
                        if(isRedirect){
                            continue;
                        }
                        entity = WikiPage.PREFIX + title;
                    }

                    for(String term: WikiPage.normalizeTitle(title)){
                        Set<String> entities = WIKI_ENTITY_INDEX.get(term);

                        if(entities != null){
                            entities.add(entity);

                            WIKI_ENTITY_INDEX.put(term, entities);
                        } else{
                            WIKI_ENTITY_INDEX.put(term, new HashSet<>(Collections.singletonList(entity)));
                        }
                    }
                }
            }

            System.out.print("\rIndexed pages: " + pageCounter);
            pagesReader.close();

            WikipediaSQLStreamReader pagelinksReader = new WikipediaSQLStreamReader(WIKI_PAGELINKS_DUMP);
            int disambiguationLinkCounter = 0;

            printTime = System.currentTimeMillis();

            while(pagelinksReader.hasNext()){
                List<List<String>> data = pagelinksReader.next();

                if(System.currentTimeMillis() - printTime >= 1000L){
                    System.out.print("\rAdded disambiguation links: "+ disambiguationLinkCounter);
                    printTime = System.currentTimeMillis();
                }

                for(List<String> values: data){
                    long id = Long.parseLong(values.get(0));

                    if(disambiguationTitles.containsKey(id)){
                        disambiguationLinkCounter += 1;
                        String entityTitle = values.get(2);

                        if(entityTitle.equals("Disambiguation")){
                            continue;
                        }

                        String entity;
                        if(redirectMappings.containsKey(entityTitle)){
                            entity = WikiPage.PREFIX + redirectMappings.get(entityTitle);
                        } else{
                            entity = WikiPage.PREFIX + entityTitle;
                        }

                        if(!WikiPage.DISAMBIGUATION_PATTERN.matcher(entityTitle).matches()){
//                            entity = WikiPage.PREFIX + entityTitle;
                            String title = disambiguationTitles.get(id);

                            if(title != null){
                                for(String term: WikiPage.normalizeTitle(title)){
                                    Set<String> entities = WIKI_ENTITY_INDEX.get(term);

                                    if(entities != null){
                                        entities.add(entity);

                                        WIKI_ENTITY_INDEX.put(term, entities);
                                    } else{
                                        WIKI_ENTITY_INDEX.put(term, new HashSet<>(Collections.singletonList(entity)));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            pagelinksReader.close();
        }
    }

    public static void loadAll(){
        try {
            Class.forName(DataStoreFactory.class.getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void closeAll(){
        WIKI_ENTITY_INDEX.close();
        LUCENE_ENTITY_INDEX.close();
    }

    public static void main(String[] args) {
        DataStoreFactory.loadAll();
        System.out.println(DataStoreFactory.WIKI_ENTITY_INDEX.size());
//        Set<String> entities = DataStoreFactory.WIKI_ENTITY_INDEX.get("barack");
//        System.out.println(entities);

        DataStoreFactory.closeAll();
    }
}

class WikiPage {
    public static final int ID_IDX = 0;
    public static final int NAMESPACE_IDX = 1;
    public static final int TITLE_IDX = 2;
    public static final int REDIRECT_IDX = 4;

    public static final String PREFIX = "https://en.wikipedia.org/wiki/";

    public static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_");
    public static final Pattern COMMA_PATTERN = Pattern.compile(", ");
    public static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\((.)+\\)");
    public static final Pattern ACCENT_PATTERN = Pattern.compile("\\p{M}");
    public static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^a-z0-9\\s]");
    public static final Pattern MULTIPLE_WHITESPACES_PATTERN = Pattern.compile("\\s\\s*");
    public static final Pattern DISAMBIGUATION_PATTERN = Pattern.compile(".*\\(disambiguation\\)$");

    public static Set<String> normalizeTitle(String title){
        Set<String> normalizedTitles = new LinkedHashSet<>();

        title = UNDERSCORE_PATTERN.matcher(title).replaceAll(" ");
        title = title.toLowerCase().trim();

        normalizedTitles.add(title);

        title = Normalizer.normalize(title, Normalizer.Form.NFD);
        title = ACCENT_PATTERN.matcher(title).replaceAll("");

        normalizedTitles.add(title);

        Matcher parenthesesMatcher = PARENTHESES_PATTERN.matcher(title);
        if(parenthesesMatcher.find()){
            title = parenthesesMatcher.replaceAll("").trim();
            title = MULTIPLE_WHITESPACES_PATTERN.matcher(title).replaceAll(" ");

            normalizedTitles.add(title);
        }

        Matcher commaMatcher = COMMA_PATTERN.matcher(title);
        if(commaMatcher.find()){
            normalizedTitles.add(COMMA_PATTERN.split(title)[0]);
        }

        Matcher specialCharMatcher = SPECIAL_CHAR_PATTERN.matcher(title);
        if(specialCharMatcher.find()){
            title = specialCharMatcher.replaceAll("").trim();
            normalizedTitles.add(title);
        }

        return normalizedTitles;
    }
}
