package com.bregant.azure_speech_recognition

import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.intent.LanguageUnderstandingModel
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.intent.IntentRecognitionResult
import com.microsoft.cognitiveservices.speech.intent.IntentRecognizer
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.CancellationDetails
import com.microsoft.cognitiveservices.speech.KeywordRecognitionModel
import android.app.Activity

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
//import androidx.core.app.ActivityCompat
import android.util.Log
import android.text.TextUtils
import java.net.URI

/** AzureSpeechRecognitionPlugin */
public class AzureSpeechRecognitionPlugin() : FlutterPlugin, Activity(), MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var azureChannel: MethodChannel
    private var microphoneStream: MicrophoneStream? = null
    private var handler: Handler

    private fun createMicrophoneStream(): MicrophoneStream {
        if (microphoneStream != null) {
            microphoneStream!!.close()
            microphoneStream = null
        }
        microphoneStream = MicrophoneStream()
        return microphoneStream!!
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        azureChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "azure_speech_recognition")
        azureChannel.setMethodCallHandler(this)
    }

    init {
        fun registerWith(registrar: Registrar) {
            this.azureChannel = MethodChannel(registrar.messenger(), "azure_speech_recognition")
            this.azureChannel.setMethodCallHandler(this)
        }
        handler = Handler(Looper.getMainLooper())
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "simpleVoice" -> {
                //_result = result
                //var permissionRequestId : Int = 5
                val speechSubscriptionKey: String = "" + call.argument("subscriptionKey")
                val serviceRegion: String = "" + call.argument("region")
                val lang: String = "" + call.argument("language")
                simpleSpeechRecognition(speechSubscriptionKey, serviceRegion, lang)
                result.success(true)
            }
            "micStreamFromSubscription" -> {
                //var permissionRequestId : Int = 5
                val speechSubscriptionKey: String = "" + call.argument("subscriptionKey")
                val serviceRegion: String = "" + call.argument("region")
                val lang: String = "" + call.argument("language")
                val config: SpeechConfig = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion)
                config.speechRecognitionLanguage = lang
                micStreamRecognition(config)
                result.success(true)
            }
            "micStreamFromEndpoint" -> {
                //var permissionRequestId : Int = 5
                val subscriptionKey: String = "" + call.argument("subscriptionKey")
                val endpoint: String = "" + call.argument("endpoint")
                val lang: String = "" + call.argument("language")
                val config: SpeechConfig = SpeechConfig.fromEndpoint(URI(endpoint), subscriptionKey)
                config.speechRecognitionLanguage = lang
                micStreamRecognition(config)
                result.success(true)
            }
            "continuousStream" -> {
                //var permissionRequestId : Int = 5
                val speechSubscriptionKey: String = "" + call.argument("subscriptionKey")
                val serviceRegion: String = "" + call.argument("region")
                val lang: String = "" + call.argument("language")
                micStreamContinuosly(speechSubscriptionKey, serviceRegion, lang)
                result.success(true)
            }
            "intentRecognizer" -> {
                //var permissionRequestId: Int = 5
                val speechSubscriptionKey: String = "" + call.argument("subscriptionKey")
                val serviceRegion: String = "" + call.argument("region")
                val appId: String = "" + call.argument("appId")
                val lang: String = "" + call.argument("language")
                recognizeIntent(speechSubscriptionKey, serviceRegion, appId, lang)
                result.success(true)
            }
            "keywordRecognizer" -> {
                //var permissionRequestId : Int = 5
                val speechSubscriptionKey: String = "" + call.argument("subscriptionKey")
                val serviceRegion: String = "" + call.argument("region")
                val lang: String = "" + call.argument("language")
                val kwsModel: String = "" + call.argument("kwsModel")
                 keywordRecognizer(speechSubscriptionKey, serviceRegion, lang, kwsModel)
                result.success(true)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        azureChannel.setMethodCallHandler(null)
    }

    private fun simpleSpeechRecognition(speechSubscriptionKey: String, serviceRegion: String, lang: String) {
        val logTag = "simpleVoice"
        try {
            val audioInput: AudioConfig = AudioConfig.fromStreamInput(createMicrophoneStream())
            val config: SpeechConfig = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion)
            config.speechRecognitionLanguage = lang
            val reco = SpeechRecognizer(config, audioInput)
            val task: Future<SpeechRecognitionResult> = reco.recognizeOnceAsync()
            invokeMethod("speech.onRecognitionStarted", null)
            setOnTaskCompletedListener(task) { result ->
                val s = result.text
                Log.i(logTag, "Recognizer returned: $s")
                if (result.reason == ResultReason.RecognizedSpeech) {
                    invokeMethod("speech.onFinalResponse", s)
                }
                reco.close()
            }
        } catch (exec: Exception) {
            invokeMethod("speech.onException", "Exception: " + exec.message)
        }
    }

    // Mic Streaming, it need the additional method implementend to get the data from the async task
    private fun micStreamRecognition(config: SpeechConfig) {
        val logTag: String = "micStream"
        try {
            val audioInput: AudioConfig = AudioConfig.fromStreamInput(createMicrophoneStream())
            val reco = SpeechRecognizer(config, audioInput)
            invokeMethod("speech.onRecognitionStarted", null)
            reco.recognizing.addEventListener { _, speechRecognitionResultEventArgs ->
                val s = speechRecognitionResultEventArgs.result.text
                Log.i(logTag, "Intermediate result received: $s")
                invokeMethod("speech.onSpeech", s)
            }
            val task: Future<SpeechRecognitionResult> = reco.recognizeOnceAsync()
            setOnTaskCompletedListener(task) { result ->
                val s = result.text
                reco.close()
                Log.i(logTag, "Recognizer returned: $s")
                invokeMethod("speech.onFinalResponse", s)
            }
        } catch (exec: Exception) {
            invokeMethod("speech.onException", "Exception: " + exec.message)
        }
    }

    // stream continuosly until you press the button to stop ! STILL NOT WORKING COMPLETELY
    private fun micStreamContinuosly(speechSubscriptionKey: String, serviceRegion: String, lang: String) {
        val logTag = "micStreamContinuos"
        var continuousListeningStarted = false
        lateinit var reco: SpeechRecognizer
        lateinit var audioInput: AudioConfig
        val content: ArrayList<String> = ArrayList<String>()
        if (continuousListeningStarted) {
            val task: Future<Void> = reco.stopContinuousRecognitionAsync()

            setOnTaskCompletedListener(task) {
                Log.i(logTag, "Continuous recognition stopped.")
                continuousListeningStarted = false
                azureChannel.invokeMethod("speech.onStartAvailable", null)
            }
            return
        }
        content.clear()
        try {
            audioInput = AudioConfig.fromStreamInput(createMicrophoneStream())
            val config: SpeechConfig = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion)
            config.speechRecognitionLanguage = lang
            reco = SpeechRecognizer(config, audioInput)
            invokeMethod("speech.onRecognitionStarted", null)

            reco.recognizing.addEventListener { _, speechRecognitionResultEventArgs ->
                val s = speechRecognitionResultEventArgs.result.text
                content.add(s)
                Log.i(logTag, "Intermediate result received: $s")
                invokeMethod("speech.onSpeech", TextUtils.join(" ", content))
                content.removeAt(content.size - 1)
            }

            reco.recognizing.addEventListener { _, speechRecognitionResultEventArgs ->
                val s = speechRecognitionResultEventArgs.result.text
                content.add(s)
                Log.i(logTag, "Final result received: $s")
                invokeMethod("speech.onFinalResponse", s)
            }
            val task: Future<Void> = reco.startContinuousRecognitionAsync()

            setOnTaskCompletedListener(task) {
                continuousListeningStarted = true
                invokeMethod("speech.onStopAvailable", null)
                println("Stopped")
            }
       } catch (exec: Exception) {
            invokeMethod("speech.onException", "Exception: " + exec.message)
        }
    }

    /// Recognize Intent method from microsoft sdk
    private fun recognizeIntent(speechSubscriptionKey: String, serviceRegion: String, appId: String, lang: String) {
        val logTag: String = "intent"
        val content: ArrayList<String> = ArrayList()
        try {
            val audioInput = AudioConfig.fromStreamInput(createMicrophoneStream())
            val config: SpeechConfig = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion)
            config.speechRecognitionLanguage = lang
            val reco = IntentRecognizer(config, audioInput)
            val intentModel: LanguageUnderstandingModel = LanguageUnderstandingModel.fromAppId(appId)
            reco.addAllIntents(intentModel)
            reco.recognizing.addEventListener { _, intentRecognitionResultEventArgs ->
                val s = intentRecognitionResultEventArgs.result.text
                content[0] = s
                Log.i(logTag, "Final result received: $s")
                invokeMethod("speech.onFinalResponse", TextUtils.join(System.lineSeparator(), content))
            }
            val task: Future<IntentRecognitionResult> = reco.recognizeOnceAsync()
            setOnTaskCompletedListener(task) { result ->
                Log.i(logTag, "Continuous recognition stopped.")
                var s = result.text
                if (result.reason != ResultReason.RecognizedIntent) {
                    val errorDetails = if (result.reason == ResultReason.Canceled)
                        CancellationDetails.fromResult(result).errorDetails else ""
                    s = "Intent failed with " + result.reason + ". Did you enter your Language Understanding subscription?" +
                            System.lineSeparator() + errorDetails
                }
                val intentId = result.intentId
                content[0] = s
                content[1] = "[intent: $intentId ]"
                invokeMethod("speech.onSpeech", TextUtils.join(System.lineSeparator(), content))
                println("Stopped")
            }
        } catch (exec: Exception) {
            //Log.e("SpeechSDKDemo", "unexpected " + exec.message)
            invokeMethod("speech.onException", "Exception: " + exec.message)
        }
    }

    private fun keywordRecognizer(speechSubscriptionKey: String, serviceRegion: String, lang: String, kwsModelFile: String) {
        val logTag: String = "keyword"
        var continuousListeningStarted: Boolean = false
        lateinit var reco: SpeechRecognizer
        lateinit var audioInput: AudioConfig
        val content: ArrayList<String> = ArrayList<String>()

        if (continuousListeningStarted) {
            if (reco != null) {
                val task: Future<Void> = reco.stopContinuousRecognitionAsync()

                setOnTaskCompletedListener(task) {
                    Log.i(logTag, "Continuous recognition stopped.")
                    continuousListeningStarted = false
                    azureChannel.invokeMethod("speech.onStartAvailable", null)
                }
            } else {
                continuousListeningStarted = false
            }
            return
        }
        content.clear()
        try {
            audioInput = AudioConfig.fromStreamInput(createMicrophoneStream())
            val config: SpeechConfig = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion)
            config.speechRecognitionLanguage = lang
            reco = SpeechRecognizer(config, audioInput)
            reco.recognizing.addEventListener { _, speechRecognitionResultEventArgs ->
                val s = speechRecognitionResultEventArgs.result.text
                content.add(s)
                Log.i(logTag, "Intermediate result received: $s")
                invokeMethod("speech.onSpeech", TextUtils.join(" ", content))
                content.removeAt(content.size - 1)
            }

            reco.recognizing.addEventListener { _, speechRecognitionResultEventArgs ->
                val s: String
                if (speechRecognitionResultEventArgs.result.reason == ResultReason.RecognizedKeyword) {
                    s = "Keyword: " + speechRecognitionResultEventArgs.result.text
                    Log.i(logTag, "Keyword recognized result received: $s")
                } else {
                    s = "Recognized: " + speechRecognitionResultEventArgs.result.text
                    Log.i(logTag, "Final result received: $s")
                }
                content.add(s)
                invokeMethod("speech.onSpeech", s)
            }

            val kwsModel = KeywordRecognitionModel.fromFile(copyAssetToCacheAndGetFilePath(kwsModelFile))
            val task: Future<Void> = reco.startKeywordRecognitionAsync(kwsModel)
            setOnTaskCompletedListener(task) {
                continuousListeningStarted = true
                invokeMethod("speech.onStopAvailable", null)
                println("Stopped")
            }
        } catch (exc: Exception) {
        }
    }

    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    private fun <T> setOnTaskCompletedListener(task: Future<T>, listener: (T) -> Unit) {
        executorService.submit {
            val result = task.get()
            listener(result)
        }
    }

    private interface OnTaskCompletedListener<T> {
        fun onCompleted(taskResult: T)
    }

    private fun setRecognizedText(s: String) {
        azureChannel.invokeMethod("speech.onSpeech", s)
    }

    private fun invokeMethod(method: String, arguments: Any?) {
        handler.post {
            azureChannel.invokeMethod(method, arguments)
        }
    }

    private fun copyAssetToCacheAndGetFilePath(filename: String): String {
        val cacheFile = File("$cacheDir/$filename")
        if (!cacheFile.exists()) {
            try {
                val iS: InputStream = assets.open(filename)
                val size: Int = iS.available()
                val buffer = ByteArray(size)
                iS.read(buffer)
                iS.close()
                val fos = FileOutputStream(cacheFile)
                fos.write(buffer)
                fos.close()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        return cacheFile.path
    }
}
