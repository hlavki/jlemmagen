/*
 * Copyright 2013 Michal Hlavac <hlavki@hlavki.eu>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    public static Lemmatizer getPrebuilt(String name) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String resource = MessageFormat.format(PREBUILD_PATTERN, name);
        InputStream in = cl.getResourceAsStream(resource);
        Lemmatizer result = null;
        if (in != null) {
            result = read(in);
        } else {
            throw new IOException("Cannot found resource " + resource);
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
