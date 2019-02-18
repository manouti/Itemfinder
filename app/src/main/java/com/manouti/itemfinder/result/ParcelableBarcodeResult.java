package com.manouti.itemfinder.result;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;

import java.util.HashMap;
import java.util.Map;


public class ParcelableBarcodeResult implements Parcelable {
    private Bitmap barcode;
    private Result rawResult;

    public ParcelableBarcodeResult() {
    }

    public ParcelableBarcodeResult(Result rawResult) {
        this.rawResult = rawResult;
    }

    private ParcelableBarcodeResult(Parcel in) {
        this.barcode = in.readParcelable(null);

        String text = in.readString();
        byte[] rawBytes = null;
        int rawByteCount = in.readInt();
        if(rawByteCount >= 0) {
            rawBytes = new byte[rawByteCount];
            in.readByteArray(rawBytes);
        }

        ResultPoint[] resultPoints = null;
        final int resultPointCount = in.readInt();
        if(resultPointCount >= 0) {
            resultPoints = new ResultPoint[resultPointCount];
            for(int i = 0; i < resultPointCount; i++) {
                float[] point = new float[2];
                in.readFloatArray(point);
                resultPoints[i] = new ResultPoint(point[0], point[1]);
            }
        }

        BarcodeFormat format = BarcodeFormat.valueOf(in.readString());

        Map<ResultMetadataType, Object> resultMetadata = new HashMap<>();
        final int metadataMapSize = in.readInt();
        if(metadataMapSize >= 0) {
            for (int i = 0; i < metadataMapSize; i++) {
                String key = in.readString();
                Object value = in.readValue(null);
                resultMetadata.put(ResultMetadataType.valueOf(key), value);
            }
        }

        long timestamp = in.readLong();

        this.rawResult = new Result(text, rawBytes, resultPoints, format, timestamp);
        this.rawResult.putAllMetadata(resultMetadata);
    }

    public Bitmap getBarcode() {
        return barcode;
    }

    public void setBarcode(Bitmap barcode) {
        this.barcode = barcode;
    }

    public Result getRawResult() {
        return rawResult;
    }

    public void setRawResult(Result rawResult) {
        this.rawResult = rawResult;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(barcode, 0);
        dest.writeString(rawResult.getText());
        byte[] rawBytes = rawResult.getRawBytes();
        if(rawBytes == null) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(rawBytes.length);
            dest.writeByteArray(rawBytes);
        }

        ResultPoint[] resultPoints = rawResult.getResultPoints();
        if(resultPoints == null) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(resultPoints.length);
            for(ResultPoint resultPoint : resultPoints) {
                dest.writeFloatArray(new float[]{resultPoint.getX(), resultPoint.getY()});
            }
        }
        dest.writeString(rawResult.getBarcodeFormat().toString());

        Map<ResultMetadataType, Object> resultMetadata = rawResult.getResultMetadata();
        if(resultMetadata == null) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(resultMetadata.size());
            for (Map.Entry<ResultMetadataType, Object> entry : resultMetadata.entrySet()) {
                dest.writeString(entry.getKey().toString());
                Object value = entry.getValue();
                dest.writeValue(value);
            }
        }

        dest.writeLong(rawResult.getTimestamp());
    }

    public static final Parcelable.Creator<ParcelableBarcodeResult> CREATOR = new Parcelable.Creator<ParcelableBarcodeResult>() {
        public ParcelableBarcodeResult createFromParcel(Parcel in) {
            return new ParcelableBarcodeResult(in);
        }
        public ParcelableBarcodeResult[] newArray(int size) {
            return new ParcelableBarcodeResult[size];
        }
    };
}
