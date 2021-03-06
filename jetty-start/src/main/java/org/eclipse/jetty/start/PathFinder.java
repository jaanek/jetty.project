//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.start;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathFinder extends SimpleFileVisitor<Path>
{
    private boolean includeDirsInResults = false;
    private Map<String, Path> hits = new HashMap<>();
    private Path basePath = null;
    private PathMatcher dirMatcher = PathMatchers.getNonHidden();
    private PathMatcher fileMatcher = PathMatchers.getNonHidden();

    private void addHit(Path path)
    {
        String relPath = basePath.relativize(path).toString();
        StartLog.debug("addHit(" + path + ") = [" + relPath + "," + path + "]");
        hits.put(relPath,path);
    }

    public PathMatcher getDirMatcher()
    {
        return dirMatcher;
    }

    public PathMatcher getFileMatcher()
    {
        return fileMatcher;
    }

    public List<File> getHitList()
    {
        List<File> ret = new ArrayList<>();
        for (Path path : hits.values())
        {
            ret.add(path.toFile());
        }
        return ret;
    }

    public Collection<Path> getHits()
    {
        return hits.values();
    }

    public boolean isIncludeDirsInResults()
    {
        return includeDirsInResults;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
    {
        if (dirMatcher.matches(dir))
        {
            StartLog.debug("Following dir: " + dir);
            if (includeDirsInResults && fileMatcher.matches(dir))
            {
                addHit(dir);
            }
            return FileVisitResult.CONTINUE;
        }
        else
        {
            StartLog.debug("Skipping dir: " + dir);
            return FileVisitResult.SKIP_SUBTREE;
        }
    }

    /**
     * Set the active basePath, used for resolving relative paths.
     * <p>
     * When a hit arrives for a subsequent find that has the same relative path as a prior hit, the new hit overrides the prior path as the active hit.
     * 
     * @param basePath
     *            the basePath to tag all hits with
     */
    public void setBase(Path basePath)
    {
        this.basePath = basePath;
    }

    public void setDirMatcher(PathMatcher dirMatcher)
    {
        this.dirMatcher = dirMatcher;
    }

    public void setFileMatcher(PathMatcher fileMatcher)
    {
        this.fileMatcher = fileMatcher;
    }

    public void setFileMatcher(String pattern)
    {
        this.fileMatcher = PathMatchers.getMatcher(pattern);
    }

    public void setIncludeDirsInResults(boolean includeDirsInResults)
    {
        this.includeDirsInResults = includeDirsInResults;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
    {
        if (fileMatcher.matches(file))
        {
            StartLog.debug("Found file: " + file);
            addHit(file);
        }
        else
        {
            StartLog.debug("Ignoring file: " + file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
    {
        if (exc instanceof FileSystemLoopException)
        {
            StartLog.warn("skipping detected filesystem loop: " + file);
            return FileVisitResult.SKIP_SUBTREE;
        }
        else
        {
            StartLog.warn(exc);
            return super.visitFileFailed(file,exc);
        }
    }
}