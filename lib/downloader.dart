import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:crypto/crypto.dart';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart'; // for the utf8.encode method

// Holds a Future to a temporary cache directory
final cacheDirFuture =
    (() async => (await (await getTemporaryDirectory()).createTemp()).path)();

Future<bool> fileIsInvalid(File file) async =>
    (await file.stat()).type == FileSystemEntityType.notFound;

final httpClient = new HttpClient();

Future<String> downloadFile(String url, {bool cache: false}) async {
  String filepath = '${await cacheDirFuture}/${sha1.convert(utf8.encode(url))}';
  File file = new File(filepath);

  if (!cache || await fileIsInvalid(file))
    await file.writeAsBytes(await http.readBytes(url));
  
  return filepath;
}
