package de.webis.parser;

import de.webis.datastructures.Query;

public abstract class CorpusStreamReader {
    public abstract Query next();
    public abstract boolean hasNext();
}
