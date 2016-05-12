package io.digdag.cli;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.nio.file.Paths;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;

import io.digdag.core.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.DynamicParameter;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import io.digdag.guice.rs.GuiceRsServerControl;
import io.digdag.client.config.Config;
import io.digdag.client.config.ConfigFactory;
import io.digdag.server.ServerBootstrap;
import io.digdag.server.ServerConfig;
import io.digdag.core.DigdagEmbed;
import io.digdag.core.config.YamlConfigLoader;
import io.digdag.core.config.ConfigLoaderManager;
import io.digdag.core.agent.WorkspaceManager;
import io.digdag.core.agent.NoopWorkspaceManager;
import static io.digdag.client.DigdagClient.objectMapper;
import static io.digdag.cli.Arguments.loadParams;
import static io.digdag.cli.SystemExitException.systemExit;

public class Sched
    extends Server
{
    private static final Logger logger = LoggerFactory.getLogger(Sched.class);

    private static final String SYSTEM_CONFIG_DAGFILE_KEY = "server.autoLoadLocalDagfile";

    private static final String SYSTEM_CONFIG_OVERWRITE_PARAMS = "server.overwriteParams";

    @Parameter(names = {"-f", "--file"})
    String dagfilePath = Run.DEFAULT_DAGFILE;

    @DynamicParameter(names = {"-p", "--param"})
    Map<String, String> params = new HashMap<>();

    @Parameter(names = {"-P", "--params-file"})
    String paramsFile = null;

    // TODO no-schedule mode

    public Sched(Context ctx)
    {
        super(ctx);
    }

    @Override
    public void main()
            throws Exception
    {
        JvmUtil.validateJavaRuntime(err);

        if (args.size() != 0) {
            throw usage(null, ctx);
        }

        sched();
    }

    @Override
    public SystemExitException usage(String error, Context ctx)
    {
        err.println("Usage: digdag sched [options...]");
        err.println("  Options:");
        err.println("    -f, --file PATH                  use this file to load tasks (default: digdag.yml)");
        err.println("    -n, --port PORT                  port number to listen for web interface and api clients (default: 65432)");
        err.println("    -b, --bind ADDRESS               IP address to listen HTTP clients (default: 127.0.0.1)");
        err.println("    -o, --database DIR               store status to this database");
        err.println("    -O, --task-log DIR               store task logs to this database");
        err.println("    -p, --param KEY=VALUE            overwrites a parameter (use multiple times to set many parameters)");
        err.println("    -P, --params-file PATH.yml       reads parameters from a YAML file");
        err.println("    -c, --config PATH.properties     server configuration property path");
        Main.showCommonOptions(err, ctx);
        return systemExit(error);
    }

    private void sched()
            throws ServletException, IOException
    {
        // use memory database by default
        if (database == null) {
            memoryDatabase = true;
        }

        Properties props = buildServerProperties();

        // read parameters
        ConfigFactory cf = new ConfigFactory(objectMapper());
        Config overwriteParams = loadParams(
                cf, new ConfigLoaderManager(cf, new YamlConfigLoader()),
                props, paramsFile, params);

        props.setProperty(SYSTEM_CONFIG_DAGFILE_KEY, dagfilePath);
        props.setProperty(SYSTEM_CONFIG_OVERWRITE_PARAMS, overwriteParams.toString());

        ServerBootstrap.startServer(localVersion, props, SchedulerServerBootStrap.class);
    }

    public static class SchedulerServerBootStrap
            extends ServerBootstrap
    {
        @Inject
        public SchedulerServerBootStrap(GuiceRsServerControl control)
        {
            super(control);
        }

        @Override
        public Injector initialize(ServletContext context)
        {
            Injector injector = super.initialize(context);

            Config systemConfig = injector.getInstance(Config.class);

            ConfigFactory cf = injector.getInstance(ConfigFactory.class);
            RevisionAutoReloader autoReloader = injector.getInstance(RevisionAutoReloader.class);
            try {
                autoReloader.watch(
                        Paths.get(systemConfig.get(SYSTEM_CONFIG_DAGFILE_KEY, String.class)),
                        cf.fromJsonString(systemConfig.get(SYSTEM_CONFIG_OVERWRITE_PARAMS, String.class)));
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            return injector;
        }

        @Override
        protected DigdagEmbed.Bootstrap bootstrap(DigdagEmbed.Bootstrap bootstrap, ServerConfig serverConfig, Version version)
        {
            return super.bootstrap(bootstrap, serverConfig, version)
                .addModules((binder) -> {
                    binder.bind(RevisionAutoReloader.class).in(Scopes.SINGLETON);
                })
                .overrideModulesWith((binder) -> {
                    // overwrite server that uses LocalWorkspaceManager
                    binder.bind(WorkspaceManager.class).to(NoopWorkspaceManager.class).in(Scopes.SINGLETON);
                });
        }
    }
}
