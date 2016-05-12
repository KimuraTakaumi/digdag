package io.digdag.cli;

import java.io.PrintStream;
import java.util.Properties;
import java.io.IOException;
import java.nio.file.Paths;
import javax.servlet.ServletException;

import com.beust.jcommander.Parameter;
import io.digdag.core.Version;
import io.digdag.server.ServerBootstrap;
import static io.digdag.cli.SystemExitException.systemExit;
import static io.digdag.server.ServerConfig.DEFAULT_PORT;
import static io.digdag.server.ServerConfig.DEFAULT_BIND;

public class Server
    extends Command
{
    @Parameter(names = {"-n", "--port"})
    Integer port = null;

    @Parameter(names = {"-b", "--bind"})
    String bind = null;

    @Parameter(names = {"-m", "--memory"})
    boolean memoryDatabase = false;

    @Parameter(names = {"-o", "--database"})
    String database = null;

    @Parameter(names = {"-O", "--task-log"})
    String taskLogPath = null;

    @Parameter(names = {"-A", "--access-log"})
    String accessLogPath = null;

    protected final Version localVersion;

    public Server(Version localVersion, Environment environment)
    {
        super(environment);
        this.localVersion = localVersion;
    }

    @Override
    public void main()
            throws Exception
    {
        JvmUtil.validateJavaRuntime(err);

        if (args.size() != 0) {
            throw usage(null, environment);
        }

        if (database == null && memoryDatabase == false && configPath == null) {
            throw usage("--database, --memory, or --config option is required", environment);
        }
        if (database != null && memoryDatabase == true) {
            throw usage("Setting both --database and --memory is invalid", environment);
        }

        server();
    }

    @Override
    public SystemExitException usage(String error, Environment environment)
    {
        err.println("Usage: digdag server [options...]");
        err.println("  Options:");
        err.println("    -n, --port PORT                  port number to listen for web interface and api clients (default: " + DEFAULT_PORT + ")");
        err.println("    -b, --bind ADDRESS               IP address to listen HTTP clients (default: " + DEFAULT_BIND + ")");
        err.println("    -m, --memory                     uses memory database");
        err.println("    -o, --database DIR               store status to this database");
        err.println("    -O, --task-log DIR               store task logs to this database");
        err.println("    -A, --access-log DIR             store access logs files to this path");
        err.println("    -c, --config PATH.properties     server configuration property path");
        Main.showCommonOptions(err, environment);
        return systemExit(error);
    }

    private void server()
            throws ServletException, IOException
    {
        ServerBootstrap.startServer(localVersion, buildServerProperties(), ServerBootstrap.class);
    }

    protected Properties buildServerProperties()
        throws IOException
    {
        // parameters for ServerBootstrap
        Properties props = loadSystemProperties();

        // overwrite by command-line parameters
        if (database != null) {
            props.setProperty("database.type", "h2");
            props.setProperty("database.path", Paths.get(database).toAbsolutePath().toString());
        }
        else if (memoryDatabase) {
            props.setProperty("database.type", "memory");
        }

        if (port != null) {
            props.setProperty("server.port", Integer.toString(port));
        }

        if (bind != null) {
            props.setProperty("server.bind", bind);
        }

        if (taskLogPath != null) {
            props.setProperty("log-server.type", "local");
            props.setProperty("log-server.local.path", taskLogPath);
        }

        if (accessLogPath != null) {
            props.setProperty("server.access-log.path", accessLogPath);
        }

        return props;
    }
}
