package com.manouti.itemfinder.item.detail;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.manouti.itemfinder.R;
import com.manouti.itemfinder.model.item.ISBNItem;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.model.item.Product;
import com.manouti.itemfinder.model.item.VINItem;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;


public class ItemDetailArrayAdapter extends ArrayAdapter<ItemDetailArrayAdapter.ItemDetail> {
    private Context mContext;

    public ItemDetailArrayAdapter(Context context, Item item) {
        super(context, R.layout.new_item_detail);
        this.mContext = context;
        if(item instanceof Product) {
            addProductDetails((Product) item);
        } else if(item instanceof ISBNItem) {
            addBookDetails((ISBNItem) item);
        } if(item instanceof VINItem) {
            addVINDetails((VINItem) item);
        }
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        View layout;
        if (view instanceof LinearLayout) {
            layout = view;
        } else {
            LayoutInflater factory = LayoutInflater.from(mContext);
            layout = factory.inflate(R.layout.new_item_detail, parent, false);
        }

        ItemDetail detail = getItem(position);
        ((TextView) layout.findViewById(R.id.detail_description)).setText(detail.description);
        ((TextView) layout.findViewById(R.id.detail_text_view)).setText(detail.value);

        return layout;
    }

    private void addProductDetails(Product product) {
        addDetail("SSCC", product.getSscc());
        addDetail("Lot number", product.getLotNumber());
        addDetail("Production date", product.getProductionDate());
        addDetail("Packaging date", product.getPackagingDate());
        addDetail("Best before date", product.getBestBeforeDate());
        addDetail("Expiration date", product.getExpirationDate());
        addDetail("Weight", product.getWeight());
        addDetail("Weight type", product.getWeightType());
        addDetail("Weight increment", product.getWeightIncrement());
        addDetail("Price", product.getPrice());
        addDetail("Price increment", product.getPriceIncrement());
        addDetail("Price currency", product.getPriceCurrency());
    }

    private void addBookDetails(ISBNItem isbnItem) {
        addDetail("Author(s)", isbnItem.getAuthors() != null ? Arrays.toString(isbnItem.getAuthors().toArray()) : "");
        addDetail("Publisher", isbnItem.getPublisher());
        addDetail("Language", isbnItem.getLanguage());
        addDetail("Country", isbnItem.getCountry());
        addDetail("List price", isbnItem.getListPrice());
    }

    private void addVINDetails(VINItem vinItem) {
        addDetail("World manufacturer ID", vinItem.getManufacturerId());
        addDetail("Vehicle descriptor section", vinItem.getDescriptorSec());
        addDetail("Vehicle identifier section", vinItem.getIdSection());
        addDetail("Country code", vinItem.getCountry());
        addDetail("Vehicle attributes", vinItem.getAttr());
        addDetail("Model year", String.valueOf(vinItem.getYear()));
        addDetail("Plant code", String.valueOf(vinItem.getPlantCode()));
        addDetail("Sequential number", vinItem.getSeqNum());
    }

    private void addDetail(String description, String value) {
        if(StringUtils.isNotBlank(value)) {
            add(new ItemDetail(description, value));
        } else {
            add(new ItemDetail(description, ""));
        }
    }

    protected class ItemDetail {
        String description;
        String value;

        public ItemDetail(String description, String value) {
            this.description = description;
            this.value = value;
        }
    }
}

