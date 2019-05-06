package co.b4pay.api.common.kit;

import java.io.File;

/**
 * new File("..\path\abc.txt") 中的三个方法获取路径的方法 1： getPath() 获取相对路径，例如 ..\path\abc.txt 2： getAbslutlyPath() 获取绝对路径，但可能包含
 * ".." 或 "." 字符，例如 D:\otherPath\..\path\abc.txt 3： getCanonicalPath() 获取绝对路径，但不包含 ".." 或 "." 字符，例如 D:\path\abc.txt
 */
public class PathKit {

    private static String webRootPath;
    private static String rootClassPath;

    @SuppressWarnings("rawtypes")
    public static String getPath(final Class clazz) {
        final String path = clazz.getResource("").getPath();
        return new File(path).getAbsolutePath();
    }

    public static String getPath(final Object object) {
        final String path = object.getClass().getResource("").getPath();
        return new File(path).getAbsolutePath();
    }

    public synchronized static String getRootClassPath() {
        if (rootClassPath == null) {
            try {
                final String path = PathKit.class.getClassLoader().getResource("").toURI().getPath();
                rootClassPath = new File(path).getAbsolutePath();
            } catch (final Exception e) {
                final String path = PathKit.class.getClassLoader().getResource("").getPath();
                rootClassPath = new File(path).getAbsolutePath();
            }
        }
        return rootClassPath;
    }

    public static String getPackagePath(final Object object) {
        final Package p = object.getClass().getPackage();
        return p != null ? p.getName().replaceAll("\\.", "/") : "";
    }

    public static File getFileFromJar(final String file) {
        throw new RuntimeException("Not finish. Do not use this method.");
    }

    public static String getWebRootPath() {
        synchronized (rootClassPath) {
            if (webRootPath == null) {
                webRootPath = detectWebRootPath();
            }
            return webRootPath;
        }
    }

    public static void setWebRootPath(String webRootPath) {
        if (webRootPath == null) {
            return;
        }

        if (webRootPath.endsWith(File.separator)) {
            webRootPath = webRootPath.substring(0, webRootPath.length() - 1);
        }
        PathKit.webRootPath = webRootPath;
    }

    private static String detectWebRootPath() {
        try {
            final String path = PathKit.class.getResource("/").toURI().getPath();
            return new File(path).getParentFile().getParentFile().getCanonicalPath();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * private static String detectWebRootPath() { try { String path = PathKit.class.getResource("/").getFile(); return
     * new File(path).getParentFile().getParentFile().getCanonicalPath(); } catch (IOException e) { throw new
     * RuntimeException(e); } }
     */
}
