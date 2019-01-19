#import "FlutterPdfViewerPlugin.h"
#import <flutter_pdf_viewer/flutter_pdf_viewer-Swift.h>

@implementation FlutterPdfViewerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterPdfViewerPlugin registerWithRegistrar:registrar];
}
@end
