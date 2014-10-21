package ch.cyberduck.core.importer;

/*
 * Copyright (c) 2002-2014 David Kocher. All rights reserved.
 * http://cyberduck.io/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Bug fixes, suggestions and comments should be sent to:
 * feedback@cyberduck.io
 */

import ch.cyberduck.core.Host;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.LocalFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.ftp.FTPConnectMode;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * @version $Id$
 */
public class FilezillaBookmarkCollection extends XmlBookmarkCollection {
    private static final Logger log = Logger.getLogger(FilezillaBookmarkCollection.class);

    private static final long serialVersionUID = -4612895793983093594L;

    @Override
    public String getBundleIdentifier() {
        return "de.filezilla";
    }

    @Override
    public String getName() {
        return "FileZilla";
    }

    @Override
    public Local getFile() {
        return LocalFactory.get(Preferences.instance().getProperty("bookmark.import.filezilla.location"));
    }

    @Override
    protected void parse(Local file) throws AccessDeniedException {
        this.read(file);
    }

    @Override
    protected AbstractHandler getHandler() {
        return new ServerHandler();
    }

    /**
     * Parser for Filezilla Site Manager.
     */
    private class ServerHandler extends AbstractHandler {
        private Host current = null;

        @Override
        public void startElement(String name) {
            if(name.equals("Server")) {
                current = new Host(Preferences.instance().getProperty("connection.hostname.default"));
            }
        }

        @Override
        public void endElement(String name, String elementText) {
            if(name.equals("Host")) {
                current.setHostname(elementText);
            }
            else if(name.equals("Protocol")) {
                try {
                    switch(Integer.parseInt(elementText)) {
                        case 0:
                            current.setProtocol(ProtocolFactory.FTP);
                            break;
                        case 3:
                        case 4:
                            current.setProtocol(ProtocolFactory.FTP_TLS);
                            break;
                        case 1:
                            current.setProtocol(ProtocolFactory.SFTP);
                            break;
                    }
                }
                catch(NumberFormatException e) {
                    log.warn("Unknown protocol:" + e.getMessage());
                }
            }
            else if(name.equals("Port")) {
                try {
                    current.setPort(Integer.parseInt(elementText));
                }
                catch(NumberFormatException e) {
                    log.warn("Invalid Port:" + e.getMessage());
                }
            }
            else if(name.equals("User")) {
                current.getCredentials().setUsername(elementText);
            }
            else if(name.equals("Logontype")) {
                try {
                    switch(Integer.parseInt(elementText)) {
                        case 0:
                            current.getCredentials().setUsername(Preferences.instance().getProperty("connection.login.anon.name"));
                            break;
                    }
                }
                catch(NumberFormatException e) {
                    log.warn("Invalid Logontype:" + e.getMessage());
                }
            }
            else if(name.equals("Pass")) {
                current.getCredentials().setPassword(elementText);
            }
            else if(name.equals("Name")) {
                current.setNickname(elementText);
            }
            else if(name.equals("EncodingType")) {
                if("Auto".equals(elementText)) {
                    current.setEncoding(null);
                }
                else {
                    current.setEncoding(elementText);
                }
            }
            else if(name.equals("PasvMode")) {
                if("MODE_PASSIVE".equals(elementText)) {
                    current.setFTPConnectMode(FTPConnectMode.passive);
                }
                else if("MODE_ACTIVE".equals(elementText)) {
                    current.setFTPConnectMode(FTPConnectMode.active);
                }
            }
            else if(name.equals("Comments")) {
                current.setComment(elementText);
            }
            else if(name.equals("LocalDir")) {
                current.setDownloadFolder(LocalFactory.get(elementText));
            }
            else if(name.equals("RemoteDir")) {
                if(StringUtils.isNotBlank(elementText)) {
                    StringBuilder b = new StringBuilder();
                    int i = 0;
                    //Path is written using wxString::Format(_T("%d %s "), (int)iter->Length(), iter->c_str());
                    for(String component : elementText.substring(3).split("\\s")) {
                        if(i % 2 == 0) {
                            b.append(component);
                        }
                        else {
                            b.append(Path.DELIMITER);
                        }
                        i++;
                    }
                    if(StringUtils.isNotBlank(b.toString())) {
                        current.setDefaultPath(b.toString());
                    }
                }
            }
            else if(name.equals("TimezoneOffset")) {
                ;
            }
            else if(name.equals("Server")) {
                add(current);
            }
        }
    }
}