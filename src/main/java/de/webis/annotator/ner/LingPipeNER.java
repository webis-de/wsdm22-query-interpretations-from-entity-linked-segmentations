package de.webis.annotator.ner;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
import com.aliasi.util.AbstractExternalizable;
import de.webis.annotator.EntityAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class LingPipeNER implements EntityAnnotator {
    private Chunker chunker;

    public LingPipeNER(){
        File modelFile = new File(getClass().getResource("/ne-en-news-muc6.AbstractCharLmRescoringChunker").getPath());
        try {
            chunker = (Chunker) AbstractExternalizable.readObject(modelFile);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<EntityAnnotation> annotate(Query query) {
        Set<EntityAnnotation> annotations = new HashSet<>();

        if(chunker != null){
            Chunking chunking = chunker.chunk(query.getText());

            for(Chunk chunk: chunking.chunkSet()){
                EntityAnnotation annotation = new EntityAnnotation();
                annotation.setBegin(chunk.start());
                annotation.setEnd(chunk.end());
                annotation.setMention(query.getText().substring(chunk.start(), chunk.end()));
                annotation.setScore(chunk.score());

                annotations.add(annotation);
            }
        }

        return annotations;
    }

    @Override
    public String getAnnotationTag() {
        return "lingpipe";
    }
}
