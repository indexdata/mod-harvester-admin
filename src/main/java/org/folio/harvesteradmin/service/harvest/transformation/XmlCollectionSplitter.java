package org.folio.harvesteradmin.service.harvest.transformation;

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
import java.util.List;

public class XmlCollectionSplitter extends DefaultHandler implements RecordProvider {
    String record=null;
    RecordReceiver target;
    String xmlCollectionOfRecords;

    public XmlCollectionSplitter(String recordsSource, RecordReceiver target) {
        this.target = target;
        this.xmlCollectionOfRecords = recordsSource;
    }

    public void produceRecords() {
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

    public static List<String> splitToListOfRecords(String xmlCollectionOfRecords) {
        RecordReceivingArrayList recordReceivingArrayList = new RecordReceivingArrayList();
        new XmlCollectionSplitter(xmlCollectionOfRecords, recordReceivingArrayList).produceRecords();
        return recordReceivingArrayList.getListOfRecords();
    }


}
