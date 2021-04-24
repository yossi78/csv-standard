package com.veeva.csvstandard.files;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.veeva.csvstandard.dao.Product;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;



@Slf4j
@Data
@Service
@NoArgsConstructor
public class ExternalMergeSortService {

    private String fileName;
    private int compareIndex;
    private int maxLineRead;


    public ExternalMergeSortService(String fileName, int compareIndex, int maxLineRead) {
        this.fileName = fileName;
        this.compareIndex = compareIndex;
        this.maxLineRead =maxLineRead;
    }


    /**
     * This method will be responsible for reading file, and creating some sorted temporary files from that.
     * After that this will merge all this file in out put files
     */
    public void externalMerge() throws IOException {
        AtomicInteger fileIndex = new AtomicInteger(-1);
        List<Product> listOfLines = new ArrayList<Product>();
        FileReaderUtil fileReaderUtil =new FileReaderUtil(fileName);
        String columnsLine=fileReaderUtil.readLine();
        while (true) {
            Integer counterForLine =fileReaderUtil.readChunkOfLines(listOfLines,maxLineRead,compareIndex);
            if(counterForLine==0){
                log.info("The file has been read completely");
                break;
            }
            sortAndWriteChunkFile(fileIndex,listOfLines);
        }
        fileReaderUtil.closeFile();
        mergeFiles(fileIndex.get(),columnsLine);
    }




    // THE MOTHOD SORT THE X LINES AND THEN CREATE A NEW FILE OF CHUNK AND SAVE THOSE SORTED LINES
    private void sortAndWriteChunkFile(AtomicInteger fileIndex,List<Product> listOfLines) throws IOException {
        fileIndex.addAndGet(1);
        FileWriter fw = new FileWriter(generateFile(fileIndex.get()));
        BufferedWriter bw = new BufferedWriter(fw);
        Collections.sort(listOfLines, new Product(compareIndex));
        for (int i = 0; i < listOfLines.size(); i++) {
            bw.append(listOfLines.get(i).line + "\n");
        }
        log.info("Finish to write a chunk file");
        bw.flush();
        bw.close();
        listOfLines.clear();
    }


    private String generateFileName(int index) {
        return this.fileName + "_" + "chunk" + "_"
                + index;
    }

    //  GENERATE TEMP FILES
    private File generateFile(int index) {
        File file = new File(generateFileName(index));
        file.deleteOnExit();
        return file;
    }


    //  THE METHOD READ LINES FROM ALL CHUNK TEMP FILES AND SORT THEM AND CREATE FINAL CSV FILE
    private void mergeFiles(int numOfFiles, String columnsLine) throws IOException {
        ArrayList<FileReader> listOfFileReader = new ArrayList<FileReader>();
        ArrayList<BufferedReader> listOfBufferedReader = new ArrayList<BufferedReader>();
        for (int index = 0; index <= numOfFiles; index++) {
            String fileName = generateFileName(index);
            listOfFileReader.add(new FileReader(fileName));
            listOfBufferedReader.add(new BufferedReader(listOfFileReader.get(index)));
        }
        sortFilesAndWriteOutput(listOfBufferedReader,columnsLine);
        for (int index = 0; index < listOfBufferedReader.size(); index++) {
            listOfBufferedReader.get(index).close();
            listOfFileReader.get(index).close();
        }
    }



    //  THE METHOD FETCH EVERY X FILES AND READ THEIR FIRST LINE THEN SORT AND WRITE INTO FINAL RESULT CSV FILE
    private void sortFilesAndWriteOutput(List<BufferedReader> listOfBufferedReader, String columnsLine) throws IOException {
        List<Product> listOfLinesfromAllFiles =readFirstLineFromEachFile(listOfBufferedReader);
        FileWriter fw = new FileWriter(this.fileName + "_sorted.csv");
        BufferedWriter bw = new BufferedWriter(fw);
        bw.append(columnsLine + "\n");
        while (true) {
            if (listOfLinesfromAllFiles.size() == 0) {
                break;
            }
            Collections.sort(listOfLinesfromAllFiles, new Product(compareIndex));
            int indexForFileName=writeFirstAndSmallestLine(bw,listOfLinesfromAllFiles);
            Boolean isReadLineSucceed = readNextLineComingAfterCurrentOne(listOfBufferedReader,listOfLinesfromAllFiles,indexForFileName);
            if(isReadLineSucceed){
                continue;
            }
            readLineAttemptFromOneOfTheFilesExceptIndexOne(listOfBufferedReader,listOfLinesfromAllFiles,indexForFileName);
        }
        closeBufferWriters(bw,fw);
    }


    private Boolean readNextLineComingAfterCurrentOne(List<BufferedReader> listOfBufferedReader
            , List<Product> listOfLinesfromAllFiles, int indexForFileName) throws IOException {
        String line = listOfBufferedReader.get(indexForFileName).readLine();
        if (line != null) {
            listOfLinesfromAllFiles.add(new Product(line,compareIndex,indexForFileName));
            return true;
        }
        return false;
    }


    private List<Product> readFirstLineFromEachFile(List<BufferedReader> listOfBufferedReader) throws IOException {
        List<Product> listOfLinesfromAllFiles = new ArrayList<Product>();
        // READ FIRST LINE FROM EACH TEMP CHUNK FILE
        for (int index = 0; index < listOfBufferedReader.size(); index++) {
            String line = listOfBufferedReader.get(index).readLine();
            if (line != null) {
                listOfLinesfromAllFiles.add(new Product(line, compareIndex,index));
            }
        }
        return  listOfLinesfromAllFiles;
    }



    private int writeFirstAndSmallestLine( BufferedWriter bw,List<Product> listOfLinesfromAllFiles) throws IOException {
        Product product = listOfLinesfromAllFiles.get(0);
        bw.append(product.line + "\n");
        int indexForFileName = product.indexForFileName;
        listOfLinesfromAllFiles.remove(0);
        return indexForFileName;
    }


    private void readLineAttemptFromOneOfTheFilesExceptIndexOne(List<BufferedReader> listOfBufferedReader, List<Product> listOfLinesfromAllFiles, int indexForFileName) throws IOException {
        for (int index = 0; index < listOfBufferedReader.size(); index++) {
            if (index == indexForFileName) {
                continue;
            } else {
                String line = listOfBufferedReader.get(index).readLine();
                if (line != null) {
                    listOfLinesfromAllFiles.add(new Product(line,compareIndex, index));
                    return ;
                }
            }
        }
    }


    private void closeBufferWriters(BufferedWriter bw,FileWriter fw) throws IOException {
        bw.flush();
        bw.close();
        fw.close();
    }


}