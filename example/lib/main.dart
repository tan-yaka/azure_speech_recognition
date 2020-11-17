import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:azure_speech_recognition/azure_speech_recognition.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _centerText = 'Unknown';
  AzureSpeechRecognition _speechAzure;
  String _subKey = "e56a6d51f227478db9ef0aaffaec4c94";
  String _region = "uksouth";
  String _lang = "en-GB";
  String _endpoint = "wss://uksouth.stt.speech.microsoft"
      ".com/speech/recognition/conversation/cognitiveservices/v1?cid=4ca8fb3e-37f2-4451-987b-0dec158ae8dd";
  bool isRecording = false;

Future<void> activateSpeechRecognizer() async {
  _speechAzure = AzureSpeechRecognition();
  // MANDATORY INITIALIZATION
  AzureSpeechRecognition.initializeWithSubscription(subscriptionKey: _subKey, region: _region, lang: _lang);
  //AzureSpeechRecognition.initializeWithEndpoint(endpoint: subscriptionKey: _subKey, _lang: _lang);

  _speechAzure.setFinalTranscription((text) {
    print('final: ' + text);
    setState(() {
      _centerText = text;
      isRecording = false;
    });
  });

  _speechAzure.setRecognitionResultHandler((text) {
    print('partial: ' + text);
    setState(() {
      _centerText = text;
    });
  });

  _speechAzure.setRecognitionStartedHandler(() {
   // called at the start of recognition (it could also not be used)
    isRecording = true;
  });

}
  @override
  void initState() {
    activateSpeechRecognizer();
    super.initState();
  }

  void _recognizeVoice() {
    try {
      AzureSpeechRecognition.simpleVoiceRecognition();
    } on PlatformException catch (e) {
      print("Failed to get text '${e.message}'.");
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Azure STT Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: <Widget>[
              Text('TEXT RECOGNIZED : $_centerText\n'),
              FloatingActionButton(
                onPressed: () {
                  if(!isRecording) _recognizeVoice();
                },
                child: Icon(Icons.mic),),
            ],
          ),
        ),
      ),
    );
  }
}
