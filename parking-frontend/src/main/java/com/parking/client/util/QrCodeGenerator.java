
package com.parking.client.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class QrCodeGenerator {

    public static Image generate(String text, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();

            BitMatrix matrix = writer.encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    size,
                    size
            );

            WritableImage image = new WritableImage(size, size);
            PixelWriter pixelWriter = image.getPixelWriter();

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    if (matrix.get(x, y)) {
                        pixelWriter.setArgb(x, y, 0xFF000000);
                    } else {
                        pixelWriter.setArgb(x, y, 0xFFFFFFFF);
                    }
                }
            }

            return image;

        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}