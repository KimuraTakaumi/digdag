package io.digdag.cli.client;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.Parameter;
import io.digdag.cli.Context;
import io.digdag.cli.SystemExitException;
import io.digdag.client.DigdagClient;
import io.digdag.client.api.RestProject;
import io.digdag.core.Version;

import static io.digdag.cli.SystemExitException.systemExit;

public class Upload
    extends ClientCommand
{
    @Parameter(names = {"-r", "--revision"})
    String revision = null;

    public Upload(Context ctx)
    {
        super(ctx);
    }

    @Override
    public void mainWithClientException()
        throws Exception
    {
        if (args.size() != 2) {
            throw usage(null, ctx);
        }
        if (revision == null) {
            throw usage("-r, --revision option is required", ctx);
        }
        upload(args.get(0), args.get(1));
    }

    public SystemExitException usage(String error, Context ctx)
    {
        err.println("Usage: digdag upload <path.tar.gz> <project>");
        err.println("  Options:");
        err.println("    -r, --revision REVISION          revision name");
        //err.println("        --time-revision              use current time as the revision name");
        showCommonOptions();
        return systemExit(error);
    }

    private void upload(String path, String projName)
        throws IOException, SystemExitException
    {
        DigdagClient client = buildClient();
        RestProject proj = client.putProjectRevision(projName, revision, new File(path));
        showUploadedProject(proj);
    }

    void showUploadedProject(RestProject proj)
    {
        ln("Uploaded:");
        ln("  id: %d", proj.getId());
        ln("  name: %s", proj.getName());
        ln("  revision: %s", proj.getRevision());
        ln("  archive type: %s", proj.getArchiveType());
        ln("  project created at: %s", proj.getCreatedAt());
        ln("  revision updated at: %s", proj.getUpdatedAt());
        ln("");
        ln("Use `digdag workflows` to show all workflows.");
    }
}
