import UIKit
import Flutter

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate {
    override func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        GeneratedPluginRegistrant.register(with: self)
        return super.application(application, didFinishLaunchingWithOptions: launchOptions)
    }
}

  private func simpleVoice(call: FlutterMethodCall, result: FlutterResult) {
    //printCallParams(call: call)

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
