package com.pycampers.flutterpdfviewer;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class FlutterPdfViewerPlugin implements MethodCallHandler {
  private final Registrar registrar;

  private FlutterPdfViewerPlugin(Registrar registrar) {
    this.registrar = registrar;
  }

  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(
        registrar.messenger(), "flutter_pdf_viewer"
    );
    channel.setMethodCallHandler(new FlutterPdfViewerPlugin(registrar));
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    Intent intent = new Intent(this.registrar.context(), PdfActivity.class);

    intent.putExtra("password", (String) call.argument("password"));
    intent.putExtra("nightMode", (Boolean) call.argument("nightMode"));
    intent.putExtra("xorDecryptKey", (String) call.argument("xorDecryptKey"));
    intent.putExtra("method", call.method);

    switch (call.method) {
      case "fromFile":
        intent.putExtra(call.method, (String) call.argument("filePath"));
        break;
      case "fromBytes":
        intent.putExtra(call.method, (byte[]) call.argument("pdfBytes"));
        break;
      case "fromAsset":
        intent.putExtra(call.method, this.registrar.lookupKeyForAsset((String) call.argument("assetPath")));
        break;
      default: {
        result.notImplemented();
        return;
      }
    }

    this.registrar.activity().startActivity(intent);
    result.success(true);
  }


}
