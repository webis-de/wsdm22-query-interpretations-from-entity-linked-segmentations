package de.webis.datastructures.persistent;

import de.webis.datastructures.Query;
import de.webis.utils.StreamSerializer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LuceneIndex {
    private IndexSearcher searcher;
    private IndexWriter writer;
    private final Analyzer analyzer;
    private Directory directory;
    private final int hitsPerPage;
    private boolean empty;

    public LuceneIndex(String dir) {
        analyzer = new StandardAnalyzer();
        hitsPerPage = 150;
        IndexReader reader = null;

        try {
            directory = FSDirectory.open(Paths.get(dir));
            reader = DirectoryReader.open(directory);
            searcher = new IndexSearcher(reader);
            empty = false;

        } catch (IndexNotFoundException e){
            empty = true;
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            try {
                writer = new IndexWriter(directory, iwc);
//                reader = DirectoryReader.open(directory);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void put(String key, Set<String> values) {
        Document document = new Document();
        document.add(new TextField("segment", key, Field.Store.YES));
        document.add(new StoredField("entities", StreamSerializer.serialize(values)));
        try {
            writer.addDocument(document);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Set<String>> get(Query query) {
        if(searcher == null){
            try {
                IndexReader reader = DirectoryReader.open(directory);
                searcher = new IndexSearcher(reader);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        QueryParser parser = new QueryParser("segment", analyzer);
        Map<String, Set<String>> searchResults = new LinkedHashMap<>();
        try {
            org.apache.lucene.search.Query luceneQuery = parser.parse(query.getText());
            TopDocs results = searcher.search(luceneQuery, hitsPerPage);
            ScoreDoc[] hits = results.scoreDocs;

            for (ScoreDoc scoreDoc : hits) {
                Document doc = searcher.doc(scoreDoc.doc);
                String segment = doc.get("segment");


                HashSet<String> entities = (HashSet<String>)
                        StreamSerializer.deserialize(doc.getBinaryValue("entities").bytes);
                searchResults.put(segment, entities);
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

        return mapToSegment(query, searchResults);
    }

    public boolean isEmpty(){
        return empty;
    }

    public void close(){
        if(writer != null){
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void flush(){
        if(writer != null){
            try {
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Map<String, Set<String>> mapToSegment(Query query, Map<String, Set<String>> luceneEntities) {
        Map<String, Set<String>> segmentEntityMap = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> entry : luceneEntities.entrySet()) {
            segmentEntityMap.put(query.getText(), entry.getValue());
        }

        return segmentEntityMap;
    }
}
