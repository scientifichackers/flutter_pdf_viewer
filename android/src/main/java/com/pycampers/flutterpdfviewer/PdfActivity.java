package com.pycampers.flutterpdfviewer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PdfActivity extends Activity implements OnLoadCompleteListener {
    FrameLayout progressOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pdf_viewer_layout);
        final PDFView pdfView = findViewById(R.id.pdfView);
        progressOverlay = findViewById(R.id.progress_overlay);
        progressOverlay.bringToFront();

        System.out.println("Pdf Activity created");

        Intent intent = getIntent();
        final Bundle intentOptions = intent.getExtras();
        assert intentOptions != null;

        final DefaultScrollHandle scrollHandle = new DefaultScrollHandle(this);
        final OnLoadCompleteListener onLoadCompleteListener = this;

        class PrimeThread extends Thread {
            public void run() {
                String method = intentOptions.getString("method");
                String xorDecryptKey = intentOptions.getString("xorDecryptKey");

                assert method != null;

                PDFView.Configurator configurator;
                if (xorDecryptKey == null) {
                    switch (method) {
                        case "fromFile":
                            configurator = pdfView.fromUri(
                                Uri.parse(intentOptions.getString(method))
                            );
                            break;
                        case "fromBytes":
                            configurator = pdfView.fromBytes(intentOptions.getByteArray(method));
                            break;
                        case "fromAsset":
                            configurator = pdfView.fromAsset(intentOptions.getString(method));
                            break;
                        default:
                            return;
                    }
                } else {
                    byte[] pdfBytes;

                    switch (method) {
                        case "fromFile":
                            try {
                                pdfBytes = xorEncryptDecrypt(
                                    readBytesFromFile(intentOptions.getString(method)),
                                    xorDecryptKey
                                );
                            } catch (IOException e) {
                                e.printStackTrace();
                                return;
                            }

                            break;
                        case "fromBytes":
                            byte[] bytes = intentOptions.getByteArray(method);

                            if (bytes != null) pdfBytes = xorEncryptDecrypt(bytes, xorDecryptKey);
                            else return;

                            break;
                        case "fromAsset":
                            try {
                                pdfBytes = xorEncryptDecrypt(
                                    readBytesFromAsset(intentOptions.getString(method)),
                                    xorDecryptKey
                                );
                            } catch (IOException e) {
                                e.printStackTrace();
                                return;
                            }

                            break;
                        default:
                            return;
                    }
                    configurator = pdfView.fromBytes(pdfBytes);
                }

                configurator
                    .password(intentOptions.getString("password"))
                    .scrollHandle(scrollHandle)
                    .nightMode(intentOptions.getBoolean("nightMode"))
                    .onLoad(onLoadCompleteListener)
                    .load();
            }
        }
        new PrimeThread().start();
    }

    private byte[] readBytesFromAsset(String fileName) throws IOException {
        InputStream inputStream = getApplicationContext().getAssets().open(fileName);

        byte[] bytes = new byte[inputStream.available()];
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        dataInputStream.readFully(bytes);

        dataInputStream.close();
        inputStream.close();

        return bytes;
    }

    private byte[] readBytesFromFile(String fileName) throws IOException {
        File file = new File(fileName);
        byte[] bytes = new byte[(int) file.length()];

        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file));
        dataInputStream.readFully(bytes);
        dataInputStream.close();

        return bytes;
    }

    private byte[] xorEncryptDecrypt(byte[] bytes, String key) {
        int secretKeyLength = key.length();

        System.out.println("decrypting...");
        for (int index = 0; index < bytes.length; index++)
            bytes[index] ^= key.charAt(index % secretKeyLength);
        System.out.println("done!");

        return bytes;
    }

    @Override
    public void loadComplete(int nbPages) {
        System.out.println("loaded!");
        progressOverlay.setVisibility(View.GONE);
    }
}
