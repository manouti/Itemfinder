package com.manouti.itemfinder.model.item;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.zxing.client.result.ExpandedProductParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ProductParsedResult;


@IgnoreExtraProperties
public class Product extends Item {
    private String rawText;
    private String productID;
    private String sscc;
    private String lotNumber;
    private String productionDate;
    private String packagingDate;
    private String bestBeforeDate;
    private String expirationDate;
    private String weight;
    private String weightType;
    private String weightIncrement;
    private String price;
    private String priceIncrement;
    private String priceCurrency;

    public Product() {
        mType = ItemType.PRODUCT;
    }

    public Product(String itemId, long timestamp, double rating, int voteCount, ParsedResult result) {
        super(itemId, timestamp, ItemType.PRODUCT, rating, voteCount);
        if(result instanceof ProductParsedResult) {
            this.rawText = ((ProductParsedResult) result).getProductID();
            this.productID = ((ProductParsedResult) result).getProductID();
        } else if(result instanceof ExpandedProductParsedResult) {
            this.rawText = ((ExpandedProductParsedResult) result).getRawText();
            this.productID = ((ExpandedProductParsedResult) result).getProductID();
            this.sscc = ((ExpandedProductParsedResult) result).getSscc();
            this.lotNumber = ((ExpandedProductParsedResult) result).getLotNumber();
            this.productionDate = ((ExpandedProductParsedResult) result).getProductionDate();
            this.packagingDate = ((ExpandedProductParsedResult) result).getPackagingDate();
            this.bestBeforeDate = ((ExpandedProductParsedResult) result).getBestBeforeDate();
            this.expirationDate = ((ExpandedProductParsedResult) result).getExpirationDate();
            this.weight = ((ExpandedProductParsedResult) result).getWeight();
            this.weightType = ((ExpandedProductParsedResult) result).getWeightType();
            this.weightIncrement = ((ExpandedProductParsedResult) result).getWeightIncrement();
            this.price = ((ExpandedProductParsedResult) result).getPrice();
            this.priceIncrement = ((ExpandedProductParsedResult) result).getPriceIncrement();
            this.priceCurrency = ((ExpandedProductParsedResult) result).getPriceCurrency();
        } else {
            throw new IllegalArgumentException(result.getClass().toString());
        }
    }

    public Product(String itemId,
                   long timestamp,
                   String rawText,
                   String productID,
                   String sscc,
                   String lotNumber,
                   String productionDate,
                   String packagingDate,
                   String bestBeforeDate,
                   String expirationDate,
                   String weight,
                   String weightType,
                   String weightIncrement,
                   String price,
                   String priceIncrement,
                   String priceCurrency,
                   double rating,
                   int voteCount) {
        super(itemId, timestamp, ItemType.PRODUCT, rating, voteCount);
        this.rawText = rawText;
        this.productID = productID;
        this.sscc = sscc;
        this.lotNumber = lotNumber;
        this.productionDate = productionDate;
        this.packagingDate = packagingDate;
        this.bestBeforeDate = bestBeforeDate;
        this.expirationDate = expirationDate;
        this.weight = weight;
        this.weightType = weightType;
        this.weightIncrement = weightIncrement;
        this.price = price;
        this.priceIncrement = priceIncrement;
        this.priceCurrency = priceCurrency;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getProductID() {
        return productID;
    }

    public void setProductID(String productID) {
        this.productID = productID;
    }

    public String getSscc() {
        return sscc;
    }

    public void setSscc(String sscc) {
        this.sscc = sscc;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public String getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(String productionDate) {
        this.productionDate = productionDate;
    }

    public String getPackagingDate() {
        return packagingDate;
    }

    public void setPackagingDate(String packagingDate) {
        this.packagingDate = packagingDate;
    }

    public String getBestBeforeDate() {
        return bestBeforeDate;
    }

    public void setBestBeforeDate(String bestBeforeDate) {
        this.bestBeforeDate = bestBeforeDate;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getWeightType() {
        return weightType;
    }

    public void setWeightType(String weightType) {
        this.weightType = weightType;
    }

    public String getWeightIncrement() {
        return weightIncrement;
    }

    public void setWeightIncrement(String weightIncrement) {
        this.weightIncrement = weightIncrement;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getPriceIncrement() {
        return priceIncrement;
    }

    public void setPriceIncrement(String priceIncrement) {
        this.priceIncrement = priceIncrement;
    }

    public String getPriceCurrency() {
        return priceCurrency;
    }

    public void setPriceCurrency(String priceCurrency) {
        this.priceCurrency = priceCurrency;
    }

    public String getType() {
        // Convert enum to string
        if (mType == null) {
            return null;
        } else {
            return mType.toString();
        }
    }

    public void setType(String type) {
        // Get enum from string
        if (type == null) {
            this.mType = null;
        } else {
            this.mType = ItemType.valueOf(type);
        }
    }
}
