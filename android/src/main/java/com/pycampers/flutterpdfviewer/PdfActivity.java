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
import java.util.Objects;

public class PdfActivity extends Activity implements OnLoadCompleteListener {
  FrameLayout progressOverlay;
  PDFView pdfView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.pdf_viewer_layout);
    this.pdfView = findViewById(R.id.pdfView);
    this.progressOverlay = findViewById(R.id.progress_overlay);
    this.progressOverlay.bringToFront();

    Intent intent = getIntent();
    Bundle intentOptions = intent.getExtras();

    assert intentOptions != null;
    String method = intentOptions.getString("method");
    String xorDecryptKey = intentOptions.getString("xorDecryptKey");

    PDFView.Configurator config;

    assert method != null;
    if (xorDecryptKey == null) {
      switch (method) {
        case "fromFile":
          config = this.pdfView.fromUri(Uri.parse(intentOptions.getString(method)));
          break;
        case "fromBytes":
          config = this.pdfView.fromBytes(intentOptions.getByteArray(method));
          break;
        case "fromAsset":
          config = this.pdfView.fromAsset(intentOptions.getString(method));
          break;
        default:
          return;
      }
    } else {
      byte[] pdfBytes;

      switch (method) {
        case "fromFile":
          try {
            pdfBytes = xorEncryptDecrypt(readBytesFromFile(intentOptions.getString(method)), xorDecryptKey);
          } catch (IOException e) {
            e.printStackTrace();
            return;
          }
          break;
        case "fromBytes":
          pdfBytes = xorEncryptDecrypt(Objects.requireNonNull(intentOptions.getByteArray(method)), xorDecryptKey);
          break;
        case "fromAsset":
          try {
            pdfBytes = xorEncryptDecrypt(readBytesFromAsset(intentOptions.getString(method)), xorDecryptKey);
          } catch (IOException e) {
            e.printStackTrace();
            return;
          }
          break;
        default: {
          return;
        }
      }
      config = this.pdfView.fromBytes(pdfBytes);
    }

    config
        .password(intentOptions.getString("password"))
        .scrollHandle(new DefaultScrollHandle(this))
        .nightMode(intentOptions.getBoolean("nightMode"))
        .onLoad(this)
        .load();
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

    for (int index = 0; index < bytes.length; index++)
      bytes[index] ^= key.charAt(index % secretKeyLength);

    return bytes;
  }

  @Override
  public void loadComplete(int nbPages) {
    System.out.println("loaded!");
    progressOverlay.setVisibility(View.GONE);
  }
}
