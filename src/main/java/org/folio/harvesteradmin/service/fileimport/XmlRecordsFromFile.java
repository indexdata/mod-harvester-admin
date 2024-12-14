package org.folio.harvesteradmin.service.fileimport;

import org.folio.reservoir.util.EncodeXmlText;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

public class XmlRecordsFromFile extends DefaultHandler implements RecordProvider, Callable<Void> {
    String record=null;
    RecordReceiver target;
    String xmlCollectionOfRecords;


    public XmlRecordsFromFile(String recordsSource) {
        System.out.println("XmlRecordsFromFile constructor, thread " + Thread.currentThread().getName());
        this.xmlCollectionOfRecords = recordsSource;
    }

    public XmlRecordsFromFile setTarget (RecordReceiver target) {
        this.target = target;
        return this;
    }

    public void provideRecords() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            InputStream inputStream = new ByteArrayInputStream(xmlCollectionOfRecords.getBytes(StandardCharsets.UTF_8));
            factory.newSAXParser().parse(inputStream, this);
        } catch (ParserConfigurationException | SAXException pce) {
            System.out.println("SaxParsing, produceRecords, error: " + pce.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        if (qName.equalsIgnoreCase("record")) {
            record="";
        }
        if (record!=null) {
            record += "<" + qName;
            for (int index = 0; index < attributes.getLength(); index++) {
                record += " " + attributes.getQName(index) + "=\"" + attributes.getValue(index) + "\"";
            }
            record += ">";
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String text = new String(ch, start, length);
        record += EncodeXmlText.encodeXmlText(text);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (record != null) {
            record += "</" + qName + ">";
            if (qName.equals("record")) {
                target.put(record);
                record = "";
            }
        }
    }

    @Override
    public void endDocument() {
        target.endOfDocument();
    }

    @Override
    public Void call() throws Exception {
        provideRecords();
        return null;
    }
}
