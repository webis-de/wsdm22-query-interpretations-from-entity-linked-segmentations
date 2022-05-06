package de.webis.cli;

import de.webis.annotator.EntityAnnotator;
import de.webis.annotator.interpretation.WebisQueryInterpretation;
import de.webis.cli.converters.QueryConverter;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.InterpretationAnnotation;
import de.webis.datastructures.Query;
import picocli.CommandLine;

import java.util.HashSet;
import java.util.Set;

/**
 * @author marcel.gohsen@uni-weimar.de
 */
@CommandLine.Command(name = "interpret")
public class QueryInterpretationCommand implements Runnable{
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @CommandLine.Option(names = {"-a", "--annotator"}, defaultValue = "de.webis.annotator.exer.WebisExplicitEntityRetriever", required = true)
    private Class<EntityAnnotator> entityAnnotatorClass;

    @CommandLine.Option(names = {"-q", "--query"}, required = true, converter = QueryConverter.class)
    private Query query;

    private final WebisQueryInterpretation queryInterpretation;

    public QueryInterpretationCommand() {
        queryInterpretation = new WebisQueryInterpretation();
    }

    @Override
    public void run() {
        EntityAnnotator entityAnnotator;

        try {
            entityAnnotator = entityAnnotatorClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Can't instantiate annotator: " + e.getMessage(), e);
        }

        Set<EntityAnnotation> entityAnnotations = entityAnnotator.annotate(query);
        Set<InterpretationAnnotation> interpretationAnnotations = queryInterpretation.annotate(query, entityAnnotations);

        for(InterpretationAnnotation annotation: interpretationAnnotations){
            System.out.println(annotation);
        }
    }
}
