package com.veeva.csvstandard.files;
import com.veeva.csvstandard.dao.Product;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;


@Slf4j
@NoArgsConstructor
public class FileReaderUtil {

    private FileReader fileReader;
    private BufferedReader br;



    public FileReaderUtil(String fileName) throws FileNotFoundException {
        fileReader = new FileReader(fileName);
        br = new BufferedReader(fileReader);
    }

    public void openFile(String fileName) throws FileNotFoundException {
        this.fileReader = new FileReader(fileName);
        BufferedReader br = new BufferedReader(fileReader);
    }

    public void closeFile() throws IOException {
        br.close();
        fileReader.close();
    }

    // THE METHOD READ X LINES FROM FILE AND RETURN THE LAST LINE WHICH WILL BE NULL IN CASE OF FINISHING READING
    public Integer readChunkOfLines(List<Product> listOfLines, Integer maxLineRead, Integer compareIndex) throws IOException {
        String line = null;
        Integer readLinesCounter=0;
        for (;readLinesCounter < maxLineRead; readLinesCounter++) {
            line = br.readLine();
            if (line == null) {
                log.info("Finish to read from source file");
                return readLinesCounter;
            } else if (line.trim().equals("")) {
                log.info("Read the an empty line");
            } else {
                listOfLines.add(new Product(line, compareIndex));
            }
        }
        log.info("Finish to read chunk");
        return readLinesCounter;
    }



    public String readLine() throws IOException {
        String line=br.readLine();
        return line;
    }



}
