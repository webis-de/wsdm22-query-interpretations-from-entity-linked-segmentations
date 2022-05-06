package de.webis.cli;

import de.webis.annotator.EntityAnnotator;
import de.webis.annotator.LoggedAnnotator;
import de.webis.annotator.exer.WebisExplicitEntityRetriever;
import de.webis.cli.converters.QueryConverter;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.Query;
import picocli.CommandLine;

import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author marcel.gohsen@uni-weimar.de
 */
@CommandLine.Command(name = "entitylink", mixinStandardHelpOptions = true)
public class EntityLinkingCommand implements Runnable {
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @CommandLine.Option(names = {"-a", "--annotator"}, defaultValue = "de.webis.annotator.exer.WebisExplicitEntityRetriever", required = true)
    private Class<EntityAnnotator> entityAnnotatorClass;

    @CommandLine.Option(names = {"-q", "--query"}, required = true, converter = QueryConverter.class)
    private Query query;

    @Override
    public void run() {
        EntityAnnotator entityAnnotator;

        try {
            entityAnnotator = entityAnnotatorClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Can't instantiate annotator: " + e.getMessage(), e);
        }

        Set<EntityAnnotation> annotationSet = entityAnnotator.annotate(query);

        int rank = 1;
        System.out.printf("%4s | %4s | %4s | %30s | %100s | %5s%n", "RANK", "BEG.", "END", "MENTION", "ENTITY", "SCORE");
        for(EntityAnnotation annotation: annotationSet){
            System.out.printf("%4d | %s%n", rank, annotation.toString());
            rank++;
        }

        if (entityAnnotator instanceof LoggedAnnotator){
            ((LoggedAnnotator) entityAnnotator).close();
        }
    }
}
