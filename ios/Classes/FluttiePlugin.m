#import "FluttiePlugin.h"

@implementation FluttiePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"fluttie"
            binaryMessenger:[registrar messenger]];
  FluttiePlugin* instance = [[FluttiePlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"isAvailable" isEqualToString:call.method]) {
    result([@NO]);
  } else {
    result(FlutterMethodNotImplemented);
  }
}

@end
