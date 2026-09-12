package com.dsa.jobportal.web;

import com.dsa.jobportal.service.JobRepository;
import java.awt.Desktop;
import java.io.File;
import java.net.BindException;
import java.net.URI;

public class WebMain {
    public static void main(String[] args) {
        boolean explicitPort = args.length > 0;
        int requestedPort = 8080;
        if (explicitPort) {
            try { requestedPort = Integer.parseInt(args[0]); }
            catch (NumberFormatException ignored) { requestedPort = 8080; }
        }

        try {
            String dataPath = locate("data/jobs.csv", "../data/jobs.csv", "../../data/jobs.csv");
            String webPath = locateDirectory("web", "../web", "../../web");
            JobRepository repository = new JobRepository();
            repository.load(dataPath);

            PortalHttpServer server = null;
            int port = requestedPort;
            int lastPort = explicitPort ? requestedPort : requestedPort + 9;
            while (port <= lastPort) {
                try {
                    server = new PortalHttpServer(port, repository, new File(webPath));
                    break;
                } catch (BindException busy) {
                    if (explicitPort) throw busy;
                    port++;
                }
            }
            if (server == null) throw new BindException("No free local port found from 8080 to 8089");
            server.start();

            String url = "http://localhost:" + port;
            System.out.println();
            System.out.println("============================================================");
            System.out.println(" NOVAHIRE INTELLIGENT JOB PORTAL | WEB EDITION V4");
            System.out.println("============================================================");
            System.out.println(" Jobs loaded : " + repository.all().size());
            System.out.println(" Web UI      : " + url);
            System.out.println(" Health API  : " + url + "/api/health");
            System.out.println(" Stop server : Ctrl+C");
            System.out.println("============================================================");

            openBrowser(url);
        } catch (Exception e) {
            System.err.println("Unable to start web edition: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void openBrowser(String url) {
        try {
            if (java.awt.GraphicsEnvironment.isHeadless()) return;
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Thread opener = new Thread(() -> {
                    try {
                        Thread.sleep(550);
                        Desktop.getDesktop().browse(URI.create(url));
                    } catch (Throwable ignored) { }
                }, "browser-opener");
                opener.setDaemon(true);
                opener.start();
            }
        } catch (Throwable ignored) { }
    }

    private static String locate(String... candidates) {
        for (int i = 0; i < candidates.length; i++) {
            File f = new File(candidates[i]);
            if (f.isFile()) return f.getPath();
        }
        return candidates[0];
    }

    private static String locateDirectory(String... candidates) {
        for (int i = 0; i < candidates.length; i++) {
            File f = new File(candidates[i]);
            if (f.isDirectory()) return f.getPath();
        }
        return candidates[0];
    }
}
