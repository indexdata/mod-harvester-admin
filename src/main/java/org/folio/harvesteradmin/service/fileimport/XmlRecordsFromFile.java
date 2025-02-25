package org.folio.harvesteradmin.service.fileimport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    public static final Logger logger = LogManager.getLogger("XmlRecordsFromFile");

    public XmlRecordsFromFile(String recordsSource) {
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
            logger.error("SaxParsing, produceRecords, error: " + pce.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {

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
    public void characters(char[] ch, int start, int length)  {
        String text = new String(ch, start, length);
        record += EncodeXmlText.encodeXmlText(text);
    }

    @Override
    public void endElement(String uri, String localName, String qName)  {
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
    public Void call() {
        provideRecords();
        return null;
    }
}
