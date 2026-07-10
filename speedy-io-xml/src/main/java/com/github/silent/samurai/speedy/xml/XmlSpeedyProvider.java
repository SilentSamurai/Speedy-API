package com.github.silent.samurai.speedy.xml;

import com.github.silent.samurai.speedy.interfaces.request.ISpeedyIoProvider;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyRequestReader;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponseWriter;
import com.github.silent.samurai.speedy.xml.request.XmlStructureReader;
import com.github.silent.samurai.speedy.xml.response.XmlResponseWriter;

public class XmlSpeedyProvider implements ISpeedyIoProvider {

    @Override
    public String getContentType() {
        return "application/xml";
    }

    @Override
    public SpeedyResponseWriter createWriter() {
        return new XmlResponseWriter();
    }

    @Override
    public SpeedyRequestReader createReader() {
        return XmlStructureReader::over;
    }
}
