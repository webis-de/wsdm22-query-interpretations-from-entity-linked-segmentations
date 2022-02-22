package de.webis.tokenizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleFilterFactory;
import org.apache.lucene.analysis.standard.ClassicTokenizerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NGramTokenizer {
    private Analyzer analyzer;

    public NGramTokenizer() {
        Map<String, String> shingleFilterOptions = new HashMap<>();

        shingleFilterOptions.put("maxShingleSize", "8");

        try {
            analyzer = CustomAnalyzer.builder()
                    .withTokenizer(ClassicTokenizerFactory.class)
                    .addTokenFilter("standard")
                    .addTokenFilter(ShingleFilterFactory.class, shingleFilterOptions)
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public NGramTokenizer(int maxNGramLength, int minNGramLength) {
        Map<String, String> shingleFilterOptions = new HashMap<>();

        shingleFilterOptions.put("maxShingleSize", String.valueOf(Math.max(maxNGramLength, 2)));
        shingleFilterOptions.put("minShingleSize", String.valueOf(Math.max(minNGramLength, 2)));

        if (minNGramLength > 1) {
            shingleFilterOptions.put("outputUnigrams", String.valueOf(false));
        }

        try {
            analyzer = CustomAnalyzer.builder()
                    .withTokenizer(ClassicTokenizerFactory.class)
                    .addTokenFilter("standard")
                    .addTokenFilter(ShingleFilterFactory.class, shingleFilterOptions)
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TokenStream getSegments(String text){
        TokenStream tokenStream = analyzer.tokenStream(null, text);

        try {
            tokenStream.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tokenStream;
    }
}
