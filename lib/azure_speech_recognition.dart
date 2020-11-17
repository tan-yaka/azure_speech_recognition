import 'dart:async';
import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

typedef void StringResultHandler(String text);

class AzureSpeechRecognition {
  static const MethodChannel _channel = const MethodChannel('azure_speech_recognition');

  static final AzureSpeechRecognition _azureSpeechRecognition = AzureSpeechRecognition._internal();

  factory AzureSpeechRecognition() => _azureSpeechRecognition;

  AzureSpeechRecognition._internal() {
    _channel.setMethodCallHandler(_platformCallHandler);
  }

  static String _subKey;
  static String _region;
  static String _lang;
  static String _endpoint;
  static String _languageUnderstandingSubscriptionKey;
  static String _languageUnderstandingServiceRegion;
  static String _languageUnderstandingAppId;
  static bool _useSubscription;

  /// default initializer for almost every type except for the intent recognizer.
  /// Default language -> English
  AzureSpeechRecognition.initializeWithSubscription(
      {@required String subscriptionKey, @required String region, @required String lang}
    ) {
    _subKey = subscriptionKey;
    _region = region;
    _lang = lang;
    _useSubscription = true;
  }

  AzureSpeechRecognition.initializeWithEndpoint(
      {@required String endpoint, @required String subscriptionKey, @required String lang}
    ) {
    _subKey = subscriptionKey;
    _endpoint = endpoint;
    _lang = lang;
    _useSubscription = false;
  }

  /// initializer for intent purpose
  /// Default language -> English
  AzureSpeechRecognition.initializeLanguageUnderstanding(
      {@required String subKey, @required String region, @required @required String appId, @required String lang}
    ) {
    _languageUnderstandingSubscriptionKey = subKey;
    _languageUnderstandingServiceRegion = region;
    _languageUnderstandingAppId = appId;
    _lang = lang;
  }

  StringResultHandler exceptionHandler;
  StringResultHandler recognitionResultHandler;
  StringResultHandler finalTranscriptionHandler;
  VoidCallback recognitionStartedHandler;
  VoidCallback startRecognitionHandler;
  VoidCallback stopRecognitionHandler;

  Future _platformCallHandler(MethodCall call) async {
    print(call.method);
    switch (call.method) {
      case "speech.onRecognitionStarted":
        recognitionStartedHandler();
        break;
      case "speech.onSpeech":
        recognitionResultHandler(call.arguments);
        break;
      case "speech.onFinalResponse":
        finalTranscriptionHandler(call.arguments);
        break;
      case "speech.onStartAvailable":
        setStartHandler(call.arguments);
        break;
      case "speech.onStopAvailable":
        setStopHandler(call.arguments);
        break;
      case "speech.onException":
        onExceptionHandler(call.arguments);
        break;
      default:
        print("Error: method called not found");
    }
  }

  /// called each time a result is obtained from the async call
  void setRecognitionResultHandler(StringResultHandler handler) =>
      recognitionResultHandler = handler;

  /// final transcription is passed here
  void setFinalTranscription(StringResultHandler handler) =>
      finalTranscriptionHandler = handler;

  /// called when an exception occur
  void onExceptionHandler(StringResultHandler handler) =>
      exceptionHandler = handler;

  /// called when the recognition is started
  void setRecognitionStartedHandler(VoidCallback handler) =>
      recognitionStartedHandler = handler;

  /// only for continuously
  void setStartHandler(VoidCallback handler) =>
      startRecognitionHandler = handler;

  /// only for continuously
  void setStopHandler(VoidCallback handler) => stopRecognitionHandler = handler;

  /// Simple voice Recognition, the result will be sent only at the end.
  /// Return the text obtained or the error caught

  static simpleVoiceRecognition() {
    if ((_subKey != null && _region != null)) {
      _channel.invokeMethod('simpleVoice', {'language': _lang, 'subscriptionKey': _subKey, 'region': _region});
    } else {
      throw "Error: SpeechRecognitionParameters not initialized correctly";
    }
  }

  /// Speech recognition that return text while still recognizing
  /// Return the text obtained or the error caught

  static micStream() {
    if (_useSubscription && _subKey != null && _region != null) {
      _channel.invokeMethod('micStreamFromSubscription', {'language': _lang, 'subscriptionKey': _subKey,
        'region': _region});
    } else if (!_useSubscription && _endpoint != null && _subKey != null) {
      _channel.invokeMethod('micStreamFromEndpoint', {'language': _lang, 'subscriptionKey': _subKey,
        'endpoint': _endpoint});
    } else {
      throw "Error: SpeechRecognitionParameters not initialized correctly";
    }
  }

  /// Speech recognition that doesnt stop recording text until you stopped it by calling again this function
  /// Return the text obtained or the error caught

  static continuousRecording() {
    if (_subKey != null && _region != null) {
      _channel.invokeMethod('continuousStream',
          {'language': _lang, 'subscriptionKey': _subKey, 'region': _region});
    } else {
      throw "Error: SpeechRecognitionParameters not initialized correctly";
    }
  }

  /// Intent recognition
  /// Return the intent obtained or the error caught

  static intentRecognizer() {
    if (_languageUnderstandingSubscriptionKey != null &&
        _languageUnderstandingServiceRegion != null &&
        _languageUnderstandingAppId != null) {
      _channel.invokeMethod('intentRecognizer', {
        'language': _lang,
        'subscriptionKey': _languageUnderstandingSubscriptionKey,
        'appId': _languageUnderstandingAppId,
        'region': _languageUnderstandingServiceRegion
      });
    } else {
      throw "Error: LanguageUnderstading not initialized correctly";
    }
  }

  /// Speech recognition with Keywords
  /// [kwsModelName] name of the file in the asset folder that contains the keywords
  /// Return the speech obtained or the error caught

  static speechRecognizerWithKeyword(String kwsModelName) {
   if(_subKey != null && _region != null){
    _channel.invokeMethod('keywordRecognizer',{'language': _lang, 'subscriptionKey': _subKey, 'region': _region,'kwsModel': kwsModelName});
   }else{
    throw "Error: SpeechRecognitionParameters not initialized correctly";
   }
  }

}
