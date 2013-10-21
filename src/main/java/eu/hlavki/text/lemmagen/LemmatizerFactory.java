/*
 * Copyright (C) 2013 Michal Hlavac <hlavki@hlavki.eu>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hlavki.text.lemmagen;

import eu.hlavki.text.lemmagen.api.Lemmatizer;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.hlavki.text.lemmagen.impl.DefaultLemmatizer;
import java.io.InputStream;
import java.text.MessageFormat;

/**
 *
 * @author Michal Hlavac <hlavki@hlavki.eu>
 */
public final class LemmatizerFactory {

    private static final Logger log = LoggerFactory.getLogger(LemmatizerFactory.class);
    private static final String PREBUILD_PATTERN = "{0}.lem";

    private LemmatizerFactory() {
    }

    public static Lemmatizer getPrebuild(String name) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String resource = MessageFormat.format(PREBUILD_PATTERN, name);
        InputStream in = cl.getResourceAsStream(resource);
        Lemmatizer result = null;
        if (in != null) {
            result = read(in);
        } else {
            throw new IOException("Cannot not find resource " + resource);
        }
        return result;
    }

    public static void saveToFile(DefaultLemmatizer lemmatizer, File file) throws IOException {
        ObjectOutputStream oos = null;
        try {
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            GZIPOutputStream zos = new GZIPOutputStream(bos);
            oos = new ObjectOutputStream(zos);
            lemmatizer.writeObject(oos, false);
        } finally {
            try {
                if (oos != null) oos.close();
            } catch (IOException e) {
                log.warn("Can't close stream", e);
            }
        }
    }

    public static Lemmatizer read(InputStream in) throws IOException {
        Lemmatizer retVal = null;
        ObjectInputStream ois = null;
        try {
            GZIPInputStream zis = new GZIPInputStream(in);
            ois = new ObjectInputStream(zis);
            retVal = new DefaultLemmatizer(ois);
            ois.close();
        } finally {
            try {
                if (ois != null) ois.close();
            } catch (IOException e) {
                log.warn("Can't close stream", e);
            }
        }
        return retVal;

    }

    static Lemmatizer readFromFile(File file) throws IOException {
        return read(new FileInputStream(file));
    }

}
