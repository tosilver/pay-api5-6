package co.b4pay.api.common.kit;

import java.io.File;

/**
 * FileKit.
 */
public class FileKit {
    public static void delete(final File file) {
        if (file != null && file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                final File files[] = file.listFiles();
                final int length = files == null ? 0 : files.length;
                for (int i = 0; i < length; i++) {
                    delete(files[i]);
                }
            }
            file.delete();
        }
    }
}
