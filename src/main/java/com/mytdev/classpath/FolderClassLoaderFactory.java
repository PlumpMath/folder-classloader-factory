/*
 * The MIT License
 *
 * Copyright 2015 Yann D'Isanto.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mytdev.classpath;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.AllArgsConstructor;
import static com.mytdev.classpath.ForbiddenFileNamesPathPredicate.noClassPathJars;

/**
 *
 * @author Yann D'Isanto
 */
@AllArgsConstructor
public final class FolderClassLoaderFactory implements Supplier<Optional<ClassLoader>> {

    private static final Logger LOGGER = Logger.getLogger(FolderClassLoaderFactory.class.getName());

    private final Path folder;

    @Override
    public Optional<ClassLoader> get() {
        try {
            return Optional.of(createClassLoader());
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return Optional.empty();
        }
    }

    public ClassLoader createClassLoader() throws IOException {
        return createClassLoader(Thread.currentThread().getContextClassLoader());
    }
    
    public ClassLoader createClassLoader(ClassLoader parent) throws IOException {
        Path zippedJarsTempFolder = createZippedJarsTempFolder();
        try (DirectoryStream<Path> directory = Files.newDirectoryStream(folder)) {
            URL[] urls = StreamSupport.stream(directory.spliterator(), false)
                    .flatMap(new Unzipper(zippedJarsTempFolder))
                    .filter(noClassPathJars())
                    .map(Path::toUri)
                    .map(this::quietUriToUrl)
                    .toArray(URL[]::new);
            return new URLClassLoader(urls, parent);
        }
    }

    private URL quietUriToUrl(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Path createZippedJarsTempFolder() throws IOException {
        Path path = folder.resolve("temp");
        if (Files.exists(path)) {
            deleteFolderRecursive(path);
        }
        Files.createDirectories(path);
        return path;
    }

    private static void deleteFolderRecursive(Path folder) throws IOException {
        Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

        });
    }

    @AllArgsConstructor
    static class Unzipper implements Function<Path, Stream<Path>> {

        private final Path destFolder;

        @Override
        public Stream<Path> apply(Path path) {
            String filename = path.getFileName().toString();
            if (filename.endsWith(".jar")) {
                return Stream.of(path);
            } else if (filename.endsWith(".zip")) {
                List<Path> paths = new ArrayList<>();
                try (FileSystem zipFileSystem = FileSystems.newFileSystem(path, null)) {
                    final Path root = zipFileSystem.getPath("/");
                    //walk the zip file tree and copy files to the destination
                    Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file,
                                BasicFileAttributes attrs) throws IOException {
                            final Path destFile = Paths.get(destFolder.toString(),
                                    file.toString());
                            if (Files.notExists(destFile)) {
                                Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                                paths.add(destFile);
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult preVisitDirectory(Path dir,
                                BasicFileAttributes attrs) throws IOException {
                            final Path dirToCreate = Paths.get(destFolder.toString(),
                                    dir.toString());
                            if (Files.notExists(dirToCreate)) {
                                Files.createDirectory(dirToCreate);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    return paths.stream();
                } catch (IOException ex) {
                    throw new RuntimeException("error deflating zip file " + path, ex);
                }
            } else {
                return Stream.empty();
            }
        }
    }

}
