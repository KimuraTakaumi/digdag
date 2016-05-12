package acceptance;

import com.google.common.io.Resources;
import io.digdag.cli.Context;
import io.digdag.cli.Main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class TestUtils
{
    static CommandStatus main(String... args)
    {
        return main(Context.defaultContext(), args);
    }

    static CommandStatus main(Context ctx, String... args)
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        final int code;
        try (
                final PrintStream outp = new PrintStream(out);
                final PrintStream errp = new PrintStream(err);
        ) {
            Context outputCapturingContext = ctx.toBuilder()
                    .out(outp)
                    .err(errp)
                    .build();
            code = new Main(outputCapturingContext).cli(args);
        }
        return CommandStatus.of(code, out.toByteArray(), err.toByteArray());
    }

    static void copyResource(String resource, Path dest)
            throws IOException
    {
        try (InputStream input = Resources.getResource(resource).openStream()) {
            Files.copy(input, dest, REPLACE_EXISTING);
        }
    }

    static void fakeHome(String home, Action a)
            throws Exception
    {
        String orig = System.setProperty("user.home", home);
        try {
            a.run();
        }
        finally {
            System.setProperty("user.home", orig);
        }
    }
}
