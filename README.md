# fluttie: Lottie for flutter
Fluttie is a [flutter](https://flutter.io/) plugin for displaying animations
created in [Lottie](http://airbnb.io/lottie/). Even complex animations can 
be displayed without having to write long custom rendering code.
The library can render the animations by piping the output from the Lottie
Android library into a Flutter texture.

**Please note:** At the moment, this plugin does not support iOS devices.

## Things to keep in mind when using this:
 - The plugin is in an early state and there will be breaking API changes
 - No iOS support yet
 - Loading animations from files is taking quite some time at the moment as the dart
   code needs to send the full animation declaration to the plugin. When
   [this issue](https://github.com/flutter/flutter/issues/11019) gets fixed, it might be faster.
 - Due to a delay between the dart code and the native backend, controlling
   multiple animations can be a bit laggy. Rendering multiple animations will
   also heavily reduce your apps framerate (Using multiple threads to circumvent this has already been implemented, but [crashes Flutter](https://github.com/flutter/flutter/issues/14169)).
 - Animation widgets need a fixed size for now
 - Do not re-use animations, as this can crash your app. Instead, save the output
   of `loadAnimationFromResource()` and construct a new animation whenever you need
   to.

## Getting Started

> The example included contains a throughoutly commented app showing how to load and
> display animations.

In order to use animations with fluttie, you will need to have a json file
containing the composition for your animation. You can either export it using
an AfterEffects plugin (see [instructions](http://airbnb.io/lottie/after-effects/getting-started.html))
or grab some at [lottiefiles.com](https://www.lottiefiles.com/).
Place the file in an folder inside your project and add it in the `assets` part of your `pubspec.yaml`.
In order to display animations in Flutter, you will have the plugin to load
a composition first:
```dart
var instance = new Fluttie();
var myComposition = await instance.loadAnimationFromResource(
    "resources/animations/emoji.json", //replace with your asset file
    bundle: DefaultAssetBundle.of(yourBuildContext)
);
```
You can then use that composition to create a new `AnimationController`,
like this:
```dart
emojiAnimation = await instance.prepareAnimation(emojiComposition)
```
You can also set the duration of your animation and configure looping while
preparing the animation by using the optional parameters of `prepareAnimation`.
After having your animation ready, you can include it as a widget:
```dart
Widget build(BuildContext context) =>
    new FluttieAnimation(emojiAnimation)
```
Don't forget to start your animation by calling `emojiAnimation.start()`!
