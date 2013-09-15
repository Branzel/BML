package net.minecraft.bootstrap;

import LZMA.LzmaInputStream;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.hopper.HopperService;

public final class Bootstrap extends JFrame
{
    private static final Font MONOSPACED = new Font("Monospaced", 0, 12);
    public static final String LAUNCHER_URL = "https://dl.dropboxusercontent.com/u/69130671/Minecraft/BML/launcher.jar";
    private final File workDir;
    private final Proxy proxy;
    private final File launcherJar;
    private final File packedLauncherJar;
    private final File packedLauncherJarNew;
    private final File packedModsZip;
    private final File packedModsZipNew;
    private final File packedConfigZip;
    private final File packedConfigZipNew;
    private final File packedVersionsZip;
    private final File packedVersionsZipNew;
    private final File packedResourcepacksZip;
    private final File packedResourcepacksZipNew;
    private final File packedLibrariesZip;
    private final File packedLibrariesZipNew;
    private final JTextArea textArea;
    private final JScrollPane scrollPane;
    private final PasswordAuthentication proxyAuth;
    private final String[] remainderArgs;
    private final StringBuilder outputBuffer = new StringBuilder();

    public Bootstrap(File workDir, Proxy proxy, PasswordAuthentication proxyAuth, String[] remainderArgs) {
        super("Minecraft Launcher");
        this.workDir = workDir;
        this.proxy = proxy;
        this.proxyAuth = proxyAuth;
        this.remainderArgs = remainderArgs;
        launcherJar = new File(workDir, "launcher.jar");
        packedLauncherJar = new File(workDir, "launcher.pack.lzma");
        packedLauncherJarNew = new File(workDir, "launcher.pack.lzma.new");
        packedConfigZip = new File(workDir, "config.zip");
        packedConfigZipNew = new File(workDir, "config.zip.new");
        packedVersionsZip = new File(workDir, "versions.zip");
        packedVersionsZipNew = new File(workDir, "versions.zip.new");
        packedResourcepacksZip = new File(workDir, "resourcepacks.zip");
        packedResourcepacksZipNew = new File(workDir, "resourcepacks.zip.new");
        packedLibrariesZip = new File(workDir, "libraries.zip");
        packedLibrariesZipNew = new File(workDir, "libraries.zip.new");
        packedModsZip = new File(workDir, "mods.zip");
        packedModsZipNew = new File(workDir, "mods.zip.new");

        setSize(854, 480);
        setDefaultCloseOperation(3);

        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.setFont(MONOSPACED);
        ((DefaultCaret)textArea.getCaret()).setUpdatePolicy(1);

        scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(22);

        add(scrollPane);
        setLocationRelativeTo(null);
        setVisible(true);

        println("Bootstrap (v5)");
        println(new StringBuilder().append("Current time is ").append(DateFormat.getDateTimeInstance(2, 2, Locale.US).format(new Date())).toString());
        println(new StringBuilder().append("System.getProperty('os.name') == '").append(System.getProperty("os.name")).append("'").toString());
        println(new StringBuilder().append("System.getProperty('os.version') == '").append(System.getProperty("os.version")).append("'").toString());
        println(new StringBuilder().append("System.getProperty('os.arch') == '").append(System.getProperty("os.arch")).append("'").toString());
        println(new StringBuilder().append("System.getProperty('java.version') == '").append(System.getProperty("java.version")).append("'").toString());
        println(new StringBuilder().append("System.getProperty('java.vendor') == '").append(System.getProperty("java.vendor")).append("'").toString());
        println(new StringBuilder().append("System.getProperty('sun.arch.data.model') == '").append(System.getProperty("sun.arch.data.model")).append("'").toString());
        println("");
    }

    public void execute(boolean force) {
        checkUpdate(force, packedLauncherJar, packedLauncherJarNew, "https://dl.dropboxusercontent.com/u/69130671/Minecraft/BML/launcher.pack.lzma");
        checkUpdate(force, packedModsZip, packedModsZipNew, "https://dl.dropboxusercontent.com/u/69130671/Minecraft/BML/mods.zip");
        checkUpdate(force, packedConfigZip, packedConfigZipNew, "https://dl.dropboxusercontent.com/u/69130671/Minecraft/BML/config.zip");
        checkUpdate(force, packedVersionsZip, packedVersionsZipNew, "https://dl.dropboxusercontent.com/u/69130671/Minecraft/BML/versions.zip");
        checkUpdate(force, packedLibrariesZip, packedLibrariesZipNew, "https://dl.dropboxusercontent.com/u/69130671/Minecraft/BML/libraries.zip");
        checkUpdate(force, packedResourcepacksZip, packedResourcepacksZipNew, "https://dl.dropboxusercontent.com/u/69130671/Minecraft/BML/resourcepacks.zip");

        startLauncher(launcherJar);
    }

    public void unpack() {
        File lzmaUnpacked = getUnpackedLzmaFile(packedLauncherJar);
        InputStream inputHandle = null;
        OutputStream outputHandle = null;

        println(new StringBuilder().append("Reversing LZMA on ").append(packedLauncherJar).append(" to ").append(lzmaUnpacked).toString());
        try
        {
          inputHandle = new LzmaInputStream(new FileInputStream(packedLauncherJar));
          outputHandle = new FileOutputStream(lzmaUnpacked);

          byte[] buffer = new byte[65536];

          int read = inputHandle.read(buffer);
          while (read >= 1) {
            outputHandle.write(buffer, 0, read);
            read = inputHandle.read(buffer);
          }
        } catch (Exception e) {
          throw new FatalBootstrapError(new StringBuilder().append("Unable to un-lzma: ").append(e).toString());
        } finally {
          closeSilently(inputHandle);
          closeSilently(outputHandle);
        }

        println(new StringBuilder().append("Unpacking ").append(lzmaUnpacked).append(" to ").append(launcherJar).toString());

        JarOutputStream jarOutputStream = null;
        try {
          jarOutputStream = new JarOutputStream(new FileOutputStream(launcherJar));
          Pack200.newUnpacker().unpack(lzmaUnpacked, jarOutputStream);
        } catch (Exception e) {
          throw new FatalBootstrapError(new StringBuilder().append("Unable to un-pack200: ").append(e).toString());
        } finally {
          closeSilently(jarOutputStream);
        }

        println(new StringBuilder().append("Cleaning up ").append(lzmaUnpacked).toString());

        lzmaUnpacked.delete();
    }
  
    public void unzip(File packedFile, File outputDir) {
        try {
            try (ZipFile zipFile = new ZipFile(packedFile)) {
                Enumeration<?> enu = zipFile.entries();
                while (enu.hasMoreElements()) {
                    ZipEntry zipEntry = (ZipEntry) enu.nextElement();

                    String name = zipEntry.getName();
                    println ("Extracting " + name);

                    File file = new File(outputDir, name);
                    if (name.endsWith("/")) {
                            file.mkdirs();
                            continue;
                    }

                    File parent = file.getParentFile();
                    if (parent != null) {
                            parent.mkdirs();
                    }
                    FileOutputStream fos;
                    try (InputStream is = zipFile.getInputStream(zipEntry)) {
                        fos = new FileOutputStream(file);
                        byte[] bytes = new byte[1024];
                        int length;
                        while ((length = is.read(bytes)) >= 0) {
                                fos.write(bytes, 0, length);
                        }
                    }
                    fos.close();
                }
            }
	} catch (IOException e) {
		println("Error while extracting " + packedFile.toString() + ": " + e.toString());
        }
    }

    public void checkUpdate(boolean force, File packedFile, File packedFileNew, String URL) {
        boolean update = false;
        File FileFolder = new File(workDir, packedFileNew.getName().substring(0, packedFileNew.getName().indexOf(".")));
        
        if (packedFileNew.isFile()) {
            println("Found cached update");
            renameNew(packedFile, packedFileNew);
            update = true;
        }
        
        Downloader.Controller controller = new Downloader.Controller();

        if ((force) || (!packedFile.exists())) {
            Downloader downloader = new Downloader(controller, this, proxy, null, packedFileNew, URL);
            downloader.run();

            if (controller.hasDownloadedLatch.getCount() != 0L) {
                throw new FatalBootstrapError("Unable to download while being forced");
            }

            renameNew(packedFile, packedFileNew);
            update = true;
        } else {
            String md5 = getMd5(packedFile);

            Thread thread = new Thread(new Downloader(controller, this, proxy, md5, packedFileNew, URL));
            thread.setName("Downloader for " + packedFile.getName());
            thread.start();
            try
            {
                println("Looking for update of " + packedFile.getName());
                boolean wasInTime = controller.foundUpdateLatch.await(3L, TimeUnit.SECONDS);

                if (controller.foundUpdate.get()) {
                    println("Found update in time, waiting to download");
                    controller.hasDownloadedLatch.await();
                    
                    renameNew(packedFile, packedFileNew);
                    update = true;
                } else if (!wasInTime) {
                    println("Didn't find an update in time.");
                }
            } catch (InterruptedException e) {
                throw new FatalBootstrapError(new StringBuilder().append("Got interrupted: ").append(e.toString()).toString());
            }
        }
        
        if (packedFile == packedLauncherJar)
        {
            unpack();
        } else if (update == true || !FileFolder.exists()) {
            deleteFile(FileFolder);
            unzip(packedFile, workDir);
        }
    }
    
    public void deleteFile(File file) {
        println("Deleting " + file.toString());
        try {
            if(file.isDirectory()){
                    //directory is empty, then delete it
                    if(file.list().length==0) {
                        file.delete();
                    } else {
                        //list all the directory contents
                        String files[] = file.list();

                        for (String temp : files) {
                            //construct the file structure
                            File fileDelete = new File(file, temp);

                            //recursive delete
                            deleteFile(fileDelete);
                        }

                        //check the directory again, if empty then delete it
                        if(file.list().length==0){
                            file.delete();
                        }
                    }
            } else {
                //if file, then delete it
                file.delete();
            }
        } catch (Exception e) {
            println("Error while deleting " + file.toString() + e.toString());
        }
    }
    
  public static void closeSilently(Closeable closeable) {
    if (closeable != null)
      try {
        closeable.close();
      }
      catch (IOException ignored) {
      }
  }

  private File getUnpackedLzmaFile(File packedLauncherJar) {
    String filePath = packedLauncherJar.getAbsolutePath();
    if (filePath.endsWith(".lzma")) {
      filePath = filePath.substring(0, filePath.length() - 5);
    }
    return new File(filePath);
  }

  public String getMd5(File file) {
    DigestInputStream stream = null;
    try {
      stream = new DigestInputStream(new FileInputStream(file), MessageDigest.getInstance("MD5"));
      byte[] buffer = new byte[65536];

      int read = stream.read(buffer);
      while (read >= 1)
        read = stream.read(buffer);
    }
    catch (NoSuchAlgorithmException | IOException ignored) {
      return null; } finally { closeSilently(stream);
    }

    if (stream != null)
    {
        return String.format("%1$032x", new Object[] { new BigInteger(1, stream.getMessageDigest().digest()) });
    } else
        return null;
  }

  public void println(String string) {
    print(new StringBuilder().append(string).append("\n").toString());
  }

  public void print(String string) {
    System.out.print(string);

    outputBuffer.append(string);

    Document document = textArea.getDocument();
    final JScrollBar scrollBar = scrollPane.getVerticalScrollBar();

    boolean shouldScroll = scrollBar.getValue() + scrollBar.getSize().getHeight() + MONOSPACED.getSize() * 2 > scrollBar.getMaximum();
    try
    {
      document.insertString(document.getLength(), string, null);
    }
    catch (BadLocationException ignored) {
    }
    if (shouldScroll)
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          scrollBar.setValue(2147483647);
        }
      });
  }

public void startLauncher(File launcherJar)
  {
    println("Starting launcher.");
    try
    {
      Class aClass = new URLClassLoader(new URL[] { launcherJar.toURI().toURL() }).loadClass("net.minecraft.launcher.Launcher");
      Constructor constructor = aClass.getConstructor(new Class[] { JFrame.class, File.class, Proxy.class, PasswordAuthentication.class, Integer.class });
      constructor.newInstance(new Object[] { this, this.workDir, this.proxy, this.proxyAuth, Integer.valueOf(2) });
    } catch (MalformedURLException | ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new FatalBootstrapError("Unable to start: " + e);
    }
  }

    public void renameNew(File packedFile, File packedFileNew) {
        if ((packedFile.exists()) && (!packedFile.isFile()) && 
                (!packedFile.delete())) {
            throw new FatalBootstrapError(new StringBuilder().append("while renaming, target path: ").append(packedFile.getAbsolutePath()).append(" is not a file and we failed to delete it").toString());
        }

        if (packedFileNew.isFile()) {
            println(new StringBuilder().append("Renaming ").append(packedFileNew.getAbsolutePath()).append(" to ").append(packedFile.getAbsolutePath()).toString());

            if (packedFileNew.renameTo(packedFile)) {
                println("Renamed successfully.");
            } else {
                if ((packedFile.exists()) && (!packedFile.canWrite())) {
                    throw new FatalBootstrapError(new StringBuilder().append("unable to rename: target").append(packedLauncherJar.getAbsolutePath()).append(" not writable").toString());
                }

                println("Unable to rename - could be on another filesystem, trying copy & delete.");

                if ((packedFileNew.exists()) && (packedFileNew.isFile()))
                    try {
                        copyFile(packedFileNew, packedFile);
                        if (packedFileNew.delete())
                            println("Copy & delete succeeded.");
                        else
                            println(new StringBuilder().append("Unable to remove ").append(packedLauncherJarNew.getAbsolutePath()).append(" after copy.").toString());
                    } catch (IOException e) {
                        throw new FatalBootstrapError(new StringBuilder().append("unable to copy:").append(e).toString());
                    }
                else
                    println("Nevermind... file vanished?");
            }
        }
    }

  public static void copyFile(File source, File target) throws IOException
  {
    if (!target.exists()) {
      target.createNewFile();
    }

    FileChannel sourceChannel = null;
    FileChannel targetChannel = null;
    try
    {
      sourceChannel = new FileInputStream(source).getChannel();
      targetChannel = new FileOutputStream(target).getChannel();
      targetChannel.transferFrom(sourceChannel, 0L, sourceChannel.size());
    } finally {
      if (sourceChannel != null) {
        sourceChannel.close();
      }

      if (targetChannel != null)
        targetChannel.close();  }  } 
  public static void main(String[] args) throws IOException { System.setProperty("java.net.preferIPv4Stack", "true");

    OptionParser optionParser = new OptionParser();
    optionParser.allowsUnrecognizedOptions();

    optionParser.accepts("help", "Show help").forHelp();
    optionParser.accepts("force", "Force updating");

    OptionSpec proxyHostOption = optionParser.accepts("proxyHost", "Optional").withRequiredArg();
    OptionSpec proxyPortOption = optionParser.accepts("proxyPort", "Optional").withRequiredArg().defaultsTo("8080", new String[0]).ofType(Integer.class);
    OptionSpec proxyUserOption = optionParser.accepts("proxyUser", "Optional").withRequiredArg();
    OptionSpec proxyPassOption = optionParser.accepts("proxyPass", "Optional").withRequiredArg();
    OptionSpec workingDirectoryOption = optionParser.accepts("workDir", "Optional").withRequiredArg().ofType(File.class).defaultsTo(Util.getWorkingDirectory(), new File[0]);
    OptionSpec nonOptions = optionParser.nonOptions();
    OptionSet optionSet;
    try { optionSet = optionParser.parse(args);
    } catch (OptionException e) {
      optionParser.printHelpOn(System.out);
      System.out.println("(to pass in arguments to minecraft directly use: '--' followed by your arguments");
      return;
    }

    if (optionSet.has("help")) {
      optionParser.printHelpOn(System.out);
      return;
    }

    String hostName = (String)optionSet.valueOf(proxyHostOption);
    Proxy proxy = Proxy.NO_PROXY;
    if (hostName != null) {
      try {
        proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(hostName, ((Integer)optionSet.valueOf(proxyPortOption)).intValue()));
      }
      catch (Exception ignored)
      {
      }
    }
    String proxyUser = (String)optionSet.valueOf(proxyUserOption);
    String proxyPass = (String)optionSet.valueOf(proxyPassOption);
    PasswordAuthentication passwordAuthentication = null;
    if ((!proxy.equals(Proxy.NO_PROXY)) && (stringHasValue(proxyUser)) && (stringHasValue(proxyPass))) {
      passwordAuthentication = new PasswordAuthentication(proxyUser, proxyPass.toCharArray());

      final PasswordAuthentication auth = passwordAuthentication;
      Authenticator.setDefault(new Authenticator()
      {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return auth;
        }
      });
    }

    File workingDirectory = (File)optionSet.valueOf(workingDirectoryOption);
    if ((workingDirectory.exists()) && (!workingDirectory.isDirectory()))
      throw new FatalBootstrapError(new StringBuilder().append("Invalid working directory: ").append(workingDirectory).toString());
    if ((!workingDirectory.exists()) && 
      (!workingDirectory.mkdirs())) {
      throw new FatalBootstrapError(new StringBuilder().append("Unable to create directory: ").append(workingDirectory).toString());
    }

    List strings = optionSet.valuesOf(nonOptions);
    String[] remainderArgs = (String[])strings.toArray(new String[strings.size()]);

    boolean force = optionSet.has("force");

    Bootstrap frame = new Bootstrap(workingDirectory, proxy, passwordAuthentication, remainderArgs);
    try
    {
      frame.execute(force);
    } catch (Throwable t) {
      ByteArrayOutputStream stracktrace = new ByteArrayOutputStream();
      t.printStackTrace(new PrintStream(stracktrace));

      StringBuilder report = new StringBuilder();
      report.append(stracktrace).append("\n\n-- Head --\nStacktrace:\n").append(stracktrace).append("\n\n").append(frame.outputBuffer);
      report.append("\tMinecraft.Bootstrap Version: 5");
      try
      {
        HopperService.submitReport(proxy, report.toString(), "Minecraft.Bootstrap", "5");
      }
      catch (Throwable ignored) {
      }
      frame.println(new StringBuilder().append("FATAL ERROR: ").append(stracktrace.toString()).toString());
      frame.println("\nPlease fix the error and restart.");
    } }

  public static boolean stringHasValue(String string)
  {
    return (string != null) && (!string.isEmpty());
  }
}

/* Location:           C:\Users\Branzel\Downloads\Minecraft.jar
 * Qualified Name:     net.minecraft.bootstrap.Bootstrap
 * JD-Core Version:    0.6.0
 */