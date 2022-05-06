package de.webis.cli.converters;

import de.webis.datastructures.Query;
import picocli.CommandLine;

/**
 * @author marcel.gohsen@uni-weimar.de
 */
public class QueryConverter implements CommandLine.ITypeConverter<Query> {
    @Override
    public Query convert(String value) throws Exception {
        return new Query(value);
    }
}
