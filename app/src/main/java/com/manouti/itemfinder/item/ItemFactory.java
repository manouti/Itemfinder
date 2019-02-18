package com.manouti.itemfinder.item;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.google.zxing.Result;
import com.google.zxing.client.android.result.ResultHandler;
import com.google.zxing.client.android.result.ResultHandlerFactory;
import com.google.zxing.client.result.ExpandedProductParsedResult;
import com.google.zxing.client.result.ISBNParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ProductParsedResult;
import com.google.zxing.client.result.VINParsedResult;
import com.manouti.itemfinder.model.item.ISBNItem;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.model.item.Product;
import com.manouti.itemfinder.model.item.VINItem;
import com.manouti.itemfinder.result.ParcelableBarcodeResult;


public class ItemFactory {

    //TODO handle unsupported item types (by throwing a custom exception)?
    public static Item fromBarcodeResult(@NonNull ParcelableBarcodeResult parcelableBarcodeResult, Activity activity) {
        Item item = null;
        Result rawResult = parcelableBarcodeResult.getRawResult();
        ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(activity, rawResult);

        String itemId = resultHandler.getDisplayContents();

        ParsedResult parsedResult = resultHandler.getResult();
        if(parsedResult instanceof ProductParsedResult || parsedResult instanceof ExpandedProductParsedResult) {
            item = new Product(itemId, rawResult.getTimestamp(), 0, 0, parsedResult);
        } else if(parsedResult instanceof ISBNParsedResult) {
            item = new ISBNItem((ISBNParsedResult) parsedResult, rawResult.getTimestamp(), 0, 0);
        } else if(parsedResult instanceof VINParsedResult) {
            item = new VINItem((VINParsedResult) parsedResult, rawResult.getTimestamp(), 0, 0);
        }
        return item;
    }

}
