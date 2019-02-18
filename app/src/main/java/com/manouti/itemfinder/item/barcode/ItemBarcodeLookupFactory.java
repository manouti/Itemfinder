package com.manouti.itemfinder.item.barcode;

import com.google.zxing.client.result.ParsedResultType;


public class ItemBarcodeLookupFactory {

    public static ItemBarcodeLookup getItemBarcodeLookup(ParsedResultType resultType) {
        switch (resultType) {
            case ISBN:
                return new ISBNLookup();
            case PRODUCT:
                return new ProductBarcodeLookup();
            case VIN:
                return new VINBarcodeLookup();
            default:
                return null;
        }
    }

}
