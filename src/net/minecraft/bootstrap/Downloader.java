package net.minecraft.bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.BindException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

/**
 *
 * @author Branzel
 */
public class Downloader implements Runnable
{
    private static final int MAX_RETRIES = 10;
    private final Proxy proxy;
    private final String currentMd5;
    private final File targetFile;
    private final Controller controller;
    private final URL url;
    private final URL urlhash;
    private String serverMd5;
    private Bootstrap bootstrap;
    
    public Downloader(Controller controller, Bootstrap bootstrap, Proxy proxy, String currentMd5, File targetFile, String FileURL)
    {
        this.controller = controller;
        this.bootstrap = bootstrap;
        this.proxy = proxy;
        this.currentMd5 = currentMd5;
        this.serverMd5 = "-";
        this.targetFile = targetFile;
        try {
            this.url = new URL(FileURL);
            this.urlhash = new URL(FileURL + ".hash");
        } catch (MalformedURLException ex) {
            throw new FatalBootstrapError("Can't reach download server");
        }
    }
    
    @Override
    public void run()
    {
        int retries = 0;
        while (true) {
            retries++; if (retries > MAX_RETRIES) break;
            
            Scanner s = null;
            // Test code, manual hash
            try {
                if ("-".equals(serverMd5))
                {
                    s = new Scanner(urlhash.openStream());
                    // read from your scanner            
                    serverMd5 = s.nextLine();
                }
            }
            catch(Exception e) {
                log("Remote file not found.");
                return;
            } finally {
                if (s != null) s.close();
            }
            
            if (serverMd5.equalsIgnoreCase(currentMd5))
            {
                controller.foundUpdate.set(false);
                controller.foundUpdateLatch.countDown();
                log("No update found.");
                return;
            } else {
                controller.foundUpdate.set(true);
                controller.foundUpdateLatch.countDown();
            }
            try {
                HttpsURLConnection connection = getConnection(url);

                connection.setUseCaches(false);
                connection.setDefaultUseCaches(false);
                connection.setRequestProperty("Cache-Control", "no-store,max-age=0,no-cache");
                connection.setRequestProperty("Expires", "0");
                connection.setRequestProperty("Pragma", "no-cache");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(10000);

                log(new StringBuilder().append("Downloading: ").append(url.toString()).append(retries > 1 ? String.format(" (try %d/%d)", new Object[] { Integer.valueOf(retries), Integer.valueOf(10) }) : "").toString());
                long start = System.nanoTime();
                connection.connect();
                long elapsed = System.nanoTime() - start;
                log(new StringBuilder().append("Got reply in: ").append(elapsed / 1000000L).append("ms").toString());

                InputStream inputStream = connection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(targetFile);

                MessageDigest digest = MessageDigest.getInstance("MD5");

                long startDownload = System.nanoTime();
                long bytesRead = 0L;
                byte[] buffer = new byte[65536];
                try {
                  int read = inputStream.read(buffer);
                  while (read >= 1) {
                    bytesRead += read;
                    digest.update(buffer, 0, read);
                    outputStream.write(buffer, 0, read);
                    read = inputStream.read(buffer);
                  }
                } finally {
                  inputStream.close();
                  outputStream.close();
                }
                long elapsedDownload = System.nanoTime() - startDownload;

                float elapsedSeconds = (float)(1L + elapsedDownload) / 1.0E+009F;
                float kbRead = (float)bytesRead / 1024.0F;
                log(String.format("Downloaded %.1fkb in %ds at %.1fkb/s", new Object[] { Float.valueOf(kbRead), Integer.valueOf((int)elapsedSeconds), Float.valueOf(kbRead / elapsedSeconds) }));

                String md5sum = String.format("%1$032x", new Object[] { new BigInteger(1, digest.digest()) });
                if ((!serverMd5.contains("-")) && (!serverMd5.equalsIgnoreCase(md5sum))) {
                  log("After downloading, the MD5 hash didn't match. Retrying");
                }
                else { 
                  controller.hasDownloadedLatch.countDown();
                  return;
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                log(new StringBuilder().append("Exception: ").append(e.toString()).toString());
                suggestHelp(e);
            }
        }

        log("Unable to download remote file. Check your internet connection/proxy settings.");
    }
    
    public void suggestHelp(Throwable t) {
        if ((t instanceof BindException))
            log("Recognized exception: the likely cause is a broken ipv4/6 stack. Check your TCP/IP settings.");
        else if ((t instanceof SSLHandshakeException))
            log("Recognized exception: the likely cause is a set of broken/missing root-certificates. Check your java install and perhaps reinstall it.");
    }
    
    public void log(String str)
    {
        bootstrap.println(str);
    }

    public HttpsURLConnection getConnection(URL url) throws IOException {
        return (HttpsURLConnection)url.openConnection(proxy);
    }

    public static class Controller
    {
        public final CountDownLatch foundUpdateLatch = new CountDownLatch(1);
        public final AtomicBoolean foundUpdate = new AtomicBoolean(false);
        public final CountDownLatch hasDownloadedLatch = new CountDownLatch(1);
    }
}