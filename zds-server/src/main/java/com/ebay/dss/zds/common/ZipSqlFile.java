package com.ebay.dss.zds.common;

import com.ebay.dss.zds.model.SqlFile;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipSqlFile {
    public static byte[] getSqlFileZipStream(List<SqlFile> fileInfoList) throws IOException {
        byte[] fileStream = null;
        File zipFile = new java.io.File("/tmp/" + System.currentTimeMillis() + ".zip");

        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));

        for (SqlFile fileInfo : fileInfoList) {
            String name = fileInfo.getName();
            File textFile = new File("/tmp/" + System.currentTimeMillis() + name);
            FileWriter fileWriter = new FileWriter(textFile);

            String content = fileInfo.getContent();
            fileWriter.write(content);
            fileWriter.close();
            zipOut.putNextEntry(new ZipEntry(name));
            FileInputStream input = new FileInputStream(textFile);
            int temp = 0;
            while ((temp = input.read()) != -1) {
                zipOut.write(temp);
            }
            zipOut.closeEntry();
            input.close();
            textFile.delete();
        }
        zipOut.close();
        fileStream = FileUtils.readFileToByteArray(zipFile);
        zipFile.delete();
        return fileStream;
    }

    public static byte[] getFileZipBytes(String fileName, String content) throws IOException {
        byte[] fileStream = null;
        File zipFile = new java.io.File("/tmp/" + System.currentTimeMillis() + "_zeta.zip");
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));

        zipOut.putNextEntry(new ZipEntry(fileName));
        zipOut.write(content.getBytes());
        zipOut.flush();
        zipOut.closeEntry();
        zipOut.close();

        fileStream = FileUtils.readFileToByteArray(zipFile);

        zipFile.delete();

        return fileStream;
    }

    public static byte[] getFileZipBytes(String content) throws IOException {
        String fileName = System.currentTimeMillis() + "_zeta";
        return getFileZipBytes(fileName, content);
    }

    public static byte[] toFileBytes(byte[] content) throws IOException {
        File path = new File("/tmp/" + System.currentTimeMillis() + "_zeta.zip");
        FileOutputStream fs = new FileOutputStream(path);
        fs.write(content);
        fs.flush();
        return FileUtils.readFileToByteArray(path);
    }

    public static List<String> readLine(byte[] content, int number) throws Exception {
        String tmpPath = "/tmp/" + System.currentTimeMillis() + "_zeta.zip";
        File path = new File(tmpPath);
        FileOutputStream fs = new FileOutputStream(path);
        fs.write(content);
        fs.flush();
        fs.close();

        ZipFile zf = new ZipFile(path);
        InputStream in = new BufferedInputStream(new FileInputStream(path));
        ZipInputStream zin = new ZipInputStream(in);
        ZipEntry ze;
        List<String> result = new ArrayList<>();
        boolean stop = false;
        try {
            while ((ze = zin.getNextEntry()) != null) {
                if (stop) break;
                if (ze.isDirectory()) {
                } else {
                    System.out.println("file - " + ze.getName() + " : "
                            + ze.getSize() + " bytes");
                    try {
                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(zf.getInputStream(ze)));
                        String line;
                        while ((line = br.readLine()) != null) {
                            result.add(line);
                            if (result.size() == number) {
                                stop = true;
                                break;
                            }
                        }
                        br.close();
                    } catch (Exception ex) {
                        System.err.println("failed: file - " + ze.getName() + " : "
                                + ze.getSize() + " bytes, skip it");
                    }
                }
            }
        } finally {
            zin.closeEntry();
            path.delete();
        }
        return result;
    }

    public static String toCSV(List<String> labels, List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(labels.stream().collect(Collectors.joining(",")));
        sb.append("\n");
        String content = rows.stream()
                .map(line -> line.stream().map(Object::toString).collect(Collectors.joining(",")))
                .collect(Collectors.joining("\n"));
        sb.append(content);
        return sb.toString();
    }

}
