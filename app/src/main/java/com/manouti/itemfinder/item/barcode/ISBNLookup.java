package com.manouti.itemfinder.item.barcode;

import android.os.AsyncTask;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.books.Books;
import com.google.api.services.books.BooksRequestInitializer;
import com.google.api.services.books.model.Volume;
import com.google.api.services.books.model.Volumes;
import com.manouti.itemfinder.model.item.ISBNItem;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


class ISBNLookup implements ItemBarcodeLookup {

    private static final String API_KEY = "your_api_key";
    private static final NetHttpTransport theTransport = new NetHttpTransport();
    private static final JsonFactory theJsonFactory = JacksonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "manouti-Itemfinder/1.0";

    @Override
    public void lookupItem(String barcode, BarcodeLookupCompletionListener completionListener) {
        new ISBNLookupAsyncTask(completionListener).execute(barcode);
    }

    private class ISBNLookupAsyncTask extends AsyncTask<String, Void, ISBNItem> {

        private BarcodeLookupCompletionListener completionListener;

        public ISBNLookupAsyncTask(BarcodeLookupCompletionListener completionListener) {
            this.completionListener = completionListener;
        }

        @Override
        protected ISBNItem doInBackground(String... params) {
            String barcode = params[0];
            final Books books = new Books.Builder(theTransport, theJsonFactory, null)
                    .setApplicationName(APPLICATION_NAME)
                    .setGoogleClientRequestInitializer(new BooksRequestInitializer(API_KEY))
                    .build();
            try {
                Books.Volumes.List volumesList = books.volumes().list("isbn:" + barcode);
                volumesList.setMaxResults(1L);
                volumesList.setFields("totalItems, items(volumeInfo, saleInfo)");
                Volumes volumes = volumesList.execute();
                if (volumes.getTotalItems() == 0 || volumes.getItems() == null) {
                    return null;
                }

                Volume volume = volumes.getItems().get(0);
                return createISBNItem(volume);
            } catch (IOException e) {
                completionListener.onLookupError(e);
                this.cancel(true);
            }
            return null;
        }

        @Override
        protected void onPostExecute(ISBNItem isbnItem) {
            super.onPostExecute(isbnItem);
            completionListener.onLookupSuccess(isbnItem);
        }

        private ISBNItem createISBNItem(Volume volume) {
            ISBNItem item = new ISBNItem();

            Volume.VolumeInfo volumeInfo = volume.getVolumeInfo();
            item.setS(volumeInfo.getTitle());
            item.setDesc(volumeInfo.getDescription());
            Double averageRating = volumeInfo.getAverageRating();
            if(averageRating != null) {
                item.setRating(averageRating);
            }
            Integer ratingsCount = volumeInfo.getRatingsCount();
            if(ratingsCount != null) {
                item.setVoteCount(ratingsCount);
            }

            fillItemCategories(item, volumeInfo);

            item.setAuthors(volumeInfo.getAuthors());
            item.setPublisher(volumeInfo.getPublisher());
            item.setLanguage(volumeInfo.getLanguage());

            Volume.SaleInfo saleInfo = volume.getSaleInfo();
            item.setCountry(saleInfo.getCountry());

            Volume.SaleInfo.ListPrice listPrice = saleInfo.getListPrice();
            if(listPrice != null) {
                Double amount = listPrice.getAmount();
                if(amount != null) {
                    item.setListPrice(listPrice.getCurrencyCode() + " " + amount);
                }
            }

            return item;
        }

        private void fillItemCategories(ISBNItem item, Volume.VolumeInfo volumeInfo) {
            List<String> categories = new LinkedList<>();
            categories.add("Book");

            String mainCategory = volumeInfo.getMainCategory();
            if(StringUtils.isNotBlank(mainCategory)) {
                categories.add(mainCategory);
            }

            List<String> volumeCategories = volumeInfo.getCategories();
            if(volumeCategories != null) {
                for(String category : volumeCategories) {
                    if(StringUtils.isNotBlank(category) && !StringUtils.equals(category, mainCategory)) {
                        categories.add(category);
                    }
                }
            }
            item.setCategories(categories);
        }

    }

}
