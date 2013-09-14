package net.minecraft.bootstrap;

import java.io.File;

public class Util
{
  public static final String APPLICATION_NAME = "minecraft";

  public static OS getPlatform()
  {
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("win")) return OS.WINDOWS;
    if (osName.contains("mac")) return OS.MACOS;
    if (osName.contains("linux")) return OS.LINUX;
    if (osName.contains("unix")) return OS.LINUX;
    return OS.UNKNOWN;
  }

  public static File getWorkingDirectory() {
    String userHome = System.getProperty("user.home", ".");
    File workingDirectory;
    OS CurOS = getPlatform();
    if (CurOS == OS.WINDOWS) {
        String applicationData = System.getenv("APPDATA");
        String folder = applicationData != null ? applicationData : userHome;

        workingDirectory = new File(folder, "BML/");
    }
    else if (CurOS == OS.MACOS) {
        workingDirectory = new File(userHome, "Library/Application Support/BML");
    }
    else if (CurOS == OS.LINUX) {
        workingDirectory = new File(userHome, "BML/");
    }
    else {
        workingDirectory = new File(userHome, "BML/");
    }
  /*  switch (getPlatform().ordinal()) {
    case 1:
    case 2:
      workingDirectory = new File(userHome, "BML/");
      break;
    case 3:
      String applicationData = System.getenv("APPDATA");
      String folder = applicationData != null ? applicationData : userHome;

      workingDirectory = new File(folder, "BML/");
      break;
    case 4:
      workingDirectory = new File(userHome, "Library/Application Support/BML");
      break;
    default:
      workingDirectory = new File(userHome, "BML/");
    }*/

    return workingDirectory;
  }

  public static enum OS
  {
    WINDOWS, MACOS, SOLARIS, LINUX, UNKNOWN;
  }
}

/* Location:           C:\Users\Branzel\Downloads\Minecraft.jar
 * Qualified Name:     net.minecraft.bootstrap.Util
 * JD-Core Version:    0.6.0
 */