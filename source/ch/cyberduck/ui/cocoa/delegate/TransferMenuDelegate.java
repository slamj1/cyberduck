package ch.cyberduck.ui.cocoa.delegate;

/*
 *  Copyright (c) 2007 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import ch.cyberduck.core.LocalFactory;
import ch.cyberduck.core.local.RevealService;
import ch.cyberduck.core.local.RevealServiceFactory;
import ch.cyberduck.core.transfer.Transfer;
import ch.cyberduck.core.transfer.TransferItem;
import ch.cyberduck.ui.cocoa.application.NSImage;
import ch.cyberduck.ui.cocoa.application.NSMenu;
import ch.cyberduck.ui.cocoa.application.NSMenuItem;
import ch.cyberduck.ui.resources.IconCacheFactory;

import org.rococoa.Foundation;
import org.rococoa.Selector;
import org.rococoa.cocoa.foundation.NSInteger;

import java.util.ArrayList;

/**
 * @version $Id$
 */
public class TransferMenuDelegate extends AbstractMenuDelegate {

    private Transfer transfer;

    private RevealService reveal = RevealServiceFactory.get();

    public TransferMenuDelegate(final Transfer transfer) {
        this.transfer = transfer;
    }

    @Override
    public NSInteger numberOfItemsInMenu(NSMenu menu) {
        if(this.isPopulated()) {
            // If you return a negative value, the number of items is left unchanged
            // and menu:updateItem:atIndex:shouldCancel: is not called.
            return new NSInteger(-1);
        }
        return new NSInteger(transfer.getRoots().size());
    }

    @Override
    public boolean menuUpdateItemAtIndex(NSMenu menu, NSMenuItem item, NSInteger index, boolean cancel) {
        final TransferItem entry
                = new ArrayList<TransferItem>(transfer.getRoots()).get(index.intValue());
        item.setTitle(entry.remote.getName());
        if(entry.local != null) {
            item.setRepresentedObject(entry.local.getAbsolute());
            if(entry.local.exists()) {
                item.setEnabled(true);
                item.setTarget(this.id());
                item.setAction(this.getDefaultAction());
            }
            else {
                item.setEnabled(false);
                item.setTarget(null);
            }
        }
        else {
            item.setRepresentedObject(entry.remote.getAbsolute());
        }
        item.setImage(IconCacheFactory.<NSImage>get().fileIcon(entry.remote, 16));
        return super.menuUpdateItemAtIndex(menu, item, index, cancel);
    }

    public void reveal(final NSMenuItem sender) {
        reveal.reveal(LocalFactory.get(sender.representedObject()));
    }

    @Override
    protected Selector getDefaultAction() {
        return Foundation.selector("reveal:");
    }
}