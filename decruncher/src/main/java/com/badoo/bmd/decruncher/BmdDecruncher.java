package com.badoo.bmd.decruncher;

import com.badoo.bmd.BmdReader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * Application for converting BMD files to HPROF.
 * <p/>
 * Created by Erik Andre on 02/11/14.
 */
public class BmdDecruncher {

    /**
     * Convert a BMD file to HPROF, recovering as much of the original HPROF data as possible.
     * @param in input stream for the BMD data
     * @param out output stream to write HPROF data to
     * @param additionalFiles additional string resource files used to recover hashed strings from the BMD file
     */
    public static void decrunch(@Nonnull InputStream in, @Nonnull OutputStream out, @Nonnull Collection<String> additionalFiles) throws IOException {
        Set<String> strings = new HashSet<String>();
        for (String file : additionalFiles) {
            if (file.endsWith(".dex") || file.endsWith(".apk")) {
                strings.addAll(ApkStringReader.readStrings(new File(file)));
            }
            else if (file.endsWith(".jar")) {
                strings.addAll(JarStringReader.readStrings(new File(file)));
            }
            else {
                throw new IllegalArgumentException("Invalid string input file: " + file);
            }
        }
        // Make sure that we are using buffered streams to increase performance
        if (!(out instanceof BufferedOutputStream)) {
            out = new BufferedOutputStream(out);
        }
        if (!(in instanceof BufferedInputStream)) {
            in = new BufferedInputStream(in);
        }
        try {
            DecrunchProcessor processor = new DecrunchProcessor(out, strings);
            BmdReader reader = new BmdReader(in, processor);
            while (reader.hasNext()) {
                reader.next();
            }
            // Write all the heap records collected by the processor
            processor.writeHeapRecords();
            out.flush();
        }
        finally {
            out.close();
        }
    }

    public static void main(String[] args) {
        try {
            String inFile;
            String outFile;
            List<String> additionalFiles = new ArrayList<String>();
            if (args != null && args.length >= 2) {
                List<String> argList = new LinkedList<String>(Arrays.asList(args));
                inFile = argList.remove(0); // args[0]
                outFile = argList.remove(0); // args[1]
                additionalFiles.addAll(argList);
            }
            else {
                System.out.println("Usage:");
                System.out.println("java -jar decruncher.jar input.bmd output.hprof [string file1] [string file2] ...");
                System.out.println("String input files can be dex, apk or jar");
                System.out.println("Converts BMD to HPROF. String input files are used to restore hashed strings");
                System.exit(1);
                return;
            }
            decrunch(new FileInputStream(inFile), new FileOutputStream(outFile), additionalFiles);
            System.exit(0);
        }
        catch (IOException e) {
            System.err.println("Failed to convert HPROF file, " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
}
