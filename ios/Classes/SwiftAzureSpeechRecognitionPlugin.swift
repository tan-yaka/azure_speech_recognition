import Flutter

public class SwiftAzureSpeechRecognitionPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "azure_speech_recognition", binaryMessenger: registrar.messenger())
    let instance = SwiftAzureSpeechRecognitionPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
    print("Plugin registered")
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    print("call.method = " + call.method)
    switch call.method {
    case "simpleVoice":
      simpleVoice(call: call, result: result)
      break
    case "micStreamFromSubscription":
        micStreamFromSubscription(call: call, result: result)
      break
    case "micStreamFromEndpoint":
        micStreamFromEndpoint(call: call, result: result)
      break
    default:
      result(FlutterMethodNotImplemented)
    }
  }

  private func simpleVoice(call: FlutterMethodCall, result: FlutterResult) {
    printCallParams(call: call)

    let _args = call.arguments as? Dictionary<String, Any>
    let _sub = _args?["subscriptionKey"] as? String;
    let _region = _args?["region"] as? String;
    let _lang = _args?["language"] as? String;

    var speechConfig: SPXSpeechConfiguration?
    do {
        //try speechConfig = SPXSpeechConfiguration(endpoint: _endpoint, subscription: _sub)
        try speechConfig = SPXSpeechConfiguration(subscription: _sub!, region: _region!)
    } catch {
        print("error \(error) happened")
        speechConfig = nil
    }
    speechConfig?.speechRecognitionLanguage = _lang

    let audioConfig = SPXAudioConfiguration()

    let reco = try! SPXSpeechRecognizer(speechConfiguration: speechConfig!, audioConfiguration: audioConfig)

    reco.addRecognizingEventHandler() {reco, evt in
        print("intermediate recognition result: \(evt.result.text ?? "(no result)")")
    }

    let result = try! reco.recognizeOnce()
    print(result.text!)
 }

  private func micStreamFromSubscription(call: FlutterMethodCall, result: FlutterResult) {
    printCallParams(call: call)
  }

  private func micStreamFromEndpoint(call: FlutterMethodCall, result: FlutterResult) {
    printCallParams(call: call)
    //result("Hello from Uranus")
  }

  private func printCallParams(call: FlutterMethodCall) {
    print("method: " + call.method)
    print("args:")
    let args = call.arguments as? Dictionary<String, Any>
    args?.forEach{print($0)}
  }
}
