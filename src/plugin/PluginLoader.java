package plugin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * used to load plugins from jars in the local working directory
 */
public class PluginLoader<T> {
    /**
     * @param clazz the class you are looking for subclasses of (please note that
     *              clazz must be, or a subclass of the parameterization <T> of
     *              PluginLoader
     * @return List of plugins implementing clazz
     */
    @SuppressWarnings("unchecked")
    public List<T> getPlugins(Class<?> clazz) {
        ArrayList<Class<?>> allClassesFound = findAllClassesInJarsInWorkingDirectory();

        List<T> plugins = new ArrayList<T>();

        for (Class<?> tempClass : allClassesFound) {
            if (clazz.isAssignableFrom(tempClass)) {
                try {
                    plugins.add((T) tempClass.newInstance());
                } catch (ClassCastException e) {
                    // odds are tempClass is not a subclass of T
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return plugins;
    }

    private ArrayList<Class<?>> findAllClassesInJarsInWorkingDirectory() {
        ArrayList<Class<?>> allClassesFound = new ArrayList<Class<?>>();

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:.\\plugins\\*.jar");

        File filePath = new File(".\\plugins");
        for (File file : filePath.listFiles()) {
            if (!matcher.matches(file.toPath()))
                continue;

            JarFile jar;
            try {
                jar = new JarFile(file);
            } catch (IOException e) {
                System.out.println(String.format("Error reading plugin %s", file));
                continue;
            }

            for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
                JarEntry entry = entries.nextElement();

                if (!entry.getName().endsWith(".class"))
                    continue;

                try {
                    URLClassLoader loader = new URLClassLoader(new URL[]{file.toURI().toURL()});
                    allClassesFound.add(loader.loadClass(entry.getName().replaceAll("/", "\\.")
                            .replace(".class", "")));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }

        return allClassesFound;
    }

}
