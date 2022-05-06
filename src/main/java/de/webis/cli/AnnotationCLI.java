package de.webis.cli;

import picocli.CommandLine;

/**
 * @author marcel.gohsen@uni-weimar.de
 */
@CommandLine.Command(
        name = "annotate",
        subcommands = {EntityLinkingCommand.class},
        synopsisSubcommandLabel = "<COMMAND>"
)
public class AnnotationCLI implements Runnable{
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        throw new CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand!");
    }

    public static void main(String[] args) {
        int status = new CommandLine(new AnnotationCLI()).execute(args);
        System.exit(status);
    }
}
