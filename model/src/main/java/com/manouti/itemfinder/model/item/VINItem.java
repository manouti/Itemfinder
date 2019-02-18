package com.manouti.itemfinder.model.item;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.zxing.client.result.VINParsedResult;

import java.util.Map;


@IgnoreExtraProperties
public class VINItem extends Item {
    private String worldManufacturerID;
    private String vehicleDescriptorSection;
    private String vehicleIdentifierSection;
    private String countryCode;
    private String vehicleAttributes;
    private int modelYear;
    private char plantCode;
    private String sequentialNumber;

    public VINItem() {
        mType = ItemType.VIN;
    }

    public VINItem(VINParsedResult result, long timestamp, double rating, int voteCount) {
        this(result.getVIN(), timestamp, result.getWorldManufacturerID(), result.getVehicleDescriptorSection(),
                result.getVehicleIdentifierSection(), result.getCountryCode(), result.getVehicleAttributes(),
                result.getModelYear(), result.getPlantCode(), result.getSequentialNumber(), rating, voteCount);
    }

    public VINItem(String vin,
                   long timestamp,
                           String worldManufacturerID,
                           String vehicleDescriptorSection,
                           String vehicleIdentifierSection,
                           String countryCode,
                           String vehicleAttributes,
                           int modelYear,
                           char plantCode,
                           String sequentialNumber,
                           double rating,
                           int voteCount) {
        super(vin, timestamp, ItemType.VIN, rating, voteCount);
        this.worldManufacturerID = worldManufacturerID;
        this.vehicleDescriptorSection = vehicleDescriptorSection;
        this.vehicleIdentifierSection = vehicleIdentifierSection;
        this.countryCode = countryCode;
        this.vehicleAttributes = vehicleAttributes;
        this.modelYear = modelYear;
        this.plantCode = plantCode;
        this.sequentialNumber = sequentialNumber;
    }

    public String getManufacturerId() {
        return worldManufacturerID;
    }

    public void setManufacturerId(String worldManufacturerID) {
        this.worldManufacturerID = worldManufacturerID;
    }

    public String getDescriptorSec() {
        return vehicleDescriptorSection;
    }

    public void setDescriptorSec(String vehicleDescriptorSection) {
        this.vehicleDescriptorSection = vehicleDescriptorSection;
    }

    public String getIdSection() {
        return vehicleIdentifierSection;
    }

    public void setIdSection(String vehicleIdentifierSection) {
        this.vehicleIdentifierSection = vehicleIdentifierSection;
    }

    public String getCountry() {
        return countryCode;
    }

    public void setCountry(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getAttr() {
        return vehicleAttributes;
    }

    public void setAttr(String vehicleAttributes) {
        this.vehicleAttributes = vehicleAttributes;
    }

    public int getYear() {
        return modelYear;
    }

    public void setYear(int modelYear) {
        this.modelYear = modelYear;
    }

    public char getPlantCode() {
        return plantCode;
    }

    public void setPlantCode(char plantCode) {
        this.plantCode = plantCode;
    }

    public String getSeqNum() {
        return sequentialNumber;
    }

    public void setSeqNum(String sequentialNumber) {
        this.sequentialNumber = sequentialNumber;
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

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> result = super.toMap();
        result.put("manufacturerId", worldManufacturerID);
        result.put("descriptorSec", vehicleDescriptorSection);
        result.put("idSection", vehicleIdentifierSection);
        result.put("country", countryCode);
        result.put("attr", vehicleAttributes);
        result.put("year", modelYear);
        result.put("plantCode", plantCode);
        result.put("seqNum", sequentialNumber);

        return result;
    }
}
