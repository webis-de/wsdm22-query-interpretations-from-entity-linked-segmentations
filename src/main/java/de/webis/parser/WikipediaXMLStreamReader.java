package de.webis.parser;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

public class WikipediaXMLStreamReader {

    private final List<File> wikiDumpFiles;
    private XMLStreamReader eventReader;

    private StringBuilder currentContent;


    public WikipediaXMLStreamReader(){
        wikiDumpFiles = new LinkedList<>();


        collectFiles(new File("/media/storage1/corpora/corpus-wikipedia/pages-articles-20190701"));
    }

    private void collectFiles(File dir){
        if(dir.isDirectory()){
            for(File file: dir.listFiles()){
                if(file.getName().matches("^enwiki-20190701-pages-articles(.)*")){
                    wikiDumpFiles.add(file);
                }
            }
        }
    }

    private void nextFile(){
        if(!wikiDumpFiles.isEmpty()){
            try {
                System.out.println("Process file "+ wikiDumpFiles.get(0).toString());
                eventReader = XMLInputFactory.newInstance().createXMLStreamReader(new FileReader(wikiDumpFiles.get(0)));
            } catch (XMLStreamException | FileNotFoundException e) {
                e.printStackTrace();
            }

            wikiDumpFiles.remove(0);

        }else{
            eventReader = null;
        }
    }

    public String nextRawContent(){
        if(eventReader == null){
            nextFile();
        }

        try {
            while (eventReader.hasNext()){
                eventReader.next();

                if(eventReader.isStartElement()){
                    if(eventReader.getLocalName().equals("text")){
                        return eventReader.getElementText();
                    }
                }

                if(eventReader.getEventType() == XMLStreamConstants.END_DOCUMENT){
                    nextFile();

                    if(eventReader == null){
                        return null;
                    }
                }
            }
        }
        catch (XMLStreamException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean hasNext(){
        if(eventReader != null){
            try {
                return !wikiDumpFiles.isEmpty() || eventReader.hasNext();
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }
        }
        else{
            return !wikiDumpFiles.isEmpty();
        }

        return false;
    }

    public static void main(String[] args) {
        WikipediaXMLStreamReader reader = new WikipediaXMLStreamReader();

        while(reader.hasNext()){
            System.out.println(reader.nextRawContent());
            System.out.println("***************************************");
        }
    }
}
