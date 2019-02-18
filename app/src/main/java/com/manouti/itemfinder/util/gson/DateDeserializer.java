package com.manouti.itemfinder.util.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

public final class DateDeserializer implements JsonDeserializer<Date> {

    public static Date convertISO8601ToDate(String date) throws ParseException {
        return DateUtils.parseDate(date, Locale.getDefault(),
                DateFormatUtils.ISO_DATETIME_FORMAT.getPattern(),
                DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern());
    }

    private Date deserializeToDate(JsonElement json) {
        try {
            return convertISO8601ToDate(json.getAsString());
        } catch (ParseException e) {
            throw new JsonSyntaxException(json.getAsString(), e);
        }
    }

    @Override
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return deserializeToDate(json);
    }

}