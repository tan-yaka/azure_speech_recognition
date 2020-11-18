#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint azure_speech_recognition.podspec' to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'azure_speech_recognition'
  s.version          = '0.0.1'
  s.summary          = 'Azure STT Plugin.'
  s.description      = <<-DESC
                            Azure STT Plugin.
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'
  s.dependency 'MicrosoftCognitiveServicesSpeech-iOS'
  s.platform = :ios, '9.0'

  # Flutter.framework does not contain a i386 slice. Only x86_64 simulators are supported.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'VALID_ARCHS[sdk=iphonesimulator*]' => 'x86_64', 'CLANG_ALLOW_NON_MODULAR_INCLUDES_IN_FRAMEWORK_MODULES' => 'YES' }
  #s.user_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'VALID_ARCHS[sdk=iphonesimulator*]' => 'x86_64', 'CLANG_ALLOW_NON_MODULAR_INCLUDES_IN_FRAMEWORK_MODULES' => 'YES' }
  s.swift_version = '5.0'
  s.preserve_paths = 'MicrosoftCognitiveServicesSpeech.framework'
  s.xcconfig = { 'OTHER_LDFLAGS' => '-framework MicrosoftCognitiveServicesSpeech' }
  s.vendored_frameworks = 'MicrosoftCognitiveServicesSpeech.framework'
end
