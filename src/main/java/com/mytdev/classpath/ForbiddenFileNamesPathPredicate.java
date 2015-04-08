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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;

/**
 *
 * @author Yann D'Isanto
 */
@AllArgsConstructor
final class ForbiddenFileNamesPathPredicate implements Predicate<Path> {

    private final List<String> forbiddenFileNames;

    @Override
    public boolean test(Path path) {
        return forbiddenFileNames.contains(path.getFileName().toString()) == false;
    }

    private static final ForbiddenFileNamesPathPredicate NO_CLASSPATH_JARS = new ForbiddenFileNamesPathPredicate(
            Stream.of(System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator)))
            .filter(s -> s.endsWith(".jar"))
            .map(Paths::get)
            .map(Path::getFileName)
            .map(Path::toString)
            .collect(toList()));

    public static Predicate<Path> noClassPathJars() {
        return NO_CLASSPATH_JARS;
    }
}
