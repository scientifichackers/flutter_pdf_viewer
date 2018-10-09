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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

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
        final Bundle opts = intent.getExtras();
        assert opts != null;

        final DefaultScrollHandle scrollHandle = new DefaultScrollHandle(this);
        final OnLoadCompleteListener onLoadCompleteListener = this;

        class PrimeThread extends Thread {
            public void run() {
                String method = opts.getString("method");
                String xorDecryptKey = opts.getString("xorDecryptKey");

                assert method != null;

                PDFView.Configurator configurator;
                if (xorDecryptKey == null) {
                    switch (method) {
                        case "fromFile":
                            configurator = pdfView.fromUri(
                                Uri.parse(opts.getString(method))
                            );
                            break;
                        case "fromBytes":
                            try {
                                configurator = pdfView.fromBytes(
                                    readBytesFromSocket(opts.getInt(method))
                                );
                            } catch (IOException e) {
                                e.printStackTrace();
                                return;
                            }
                            break;
                        case "fromAsset":
                            configurator = pdfView.fromAsset(opts.getString(method));
                            break;
                        default:
                            return;
                    }
                } else {
                    byte[] pdfBytes;
                    try {
                        switch (method) {
                            case "fromFile":
                                pdfBytes = xorEncryptDecrypt(
                                    readBytesFromFile(opts.getString(method)),
                                    xorDecryptKey
                                );

                                break;
                            case "fromBytes":
                                pdfBytes = xorEncryptDecrypt(
                                    readBytesFromSocket(opts.getInt(method)),
                                    xorDecryptKey
                                );

                                break;
                            case "fromAsset":
                                pdfBytes = xorEncryptDecrypt(
                                    readBytesFromAsset(opts.getString(method)),
                                    xorDecryptKey
                                );

                                break;
                            default:
                                return;
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    configurator = pdfView.fromBytes(pdfBytes);
                }

                configurator
                    .password(opts.getString("password"))
                    .scrollHandle(scrollHandle)
                    .nightMode(opts.getBoolean("nightMode"))
                    .swipeHorizontal(opts.getBoolean("swipeHorizontal"))
                    .onLoad(onLoadCompleteListener)
                    .load();
            }
        }
        new PrimeThread().start();
    }

    private byte[] readBytesFromSocket(int pdfBytesSize) throws IOException {
        System.out.println(pdfBytesSize);
        byte[] bytes = new byte[pdfBytesSize];

        Socket socket = new Socket("0.0.0.0", 4567);
        InputStream inputStream = socket.getInputStream();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

        int readTillNow = 0;
        while (readTillNow < pdfBytesSize)
            readTillNow += bufferedInputStream.read(
                bytes, readTillNow, pdfBytesSize - readTillNow
            );

        bufferedInputStream.close();
        inputStream.close();
        socket.close();

        return bytes;
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
