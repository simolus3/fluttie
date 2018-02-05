# fluttie: Lottie for flutter
Fluttie ([github](https://github.com/simolus3/fluttie/), [pub](https://pub.dartlang.org/packages/fluttie)) 
is a [flutter](https://flutter.io/) plugin for displaying animations
created in [Lottie](http://airbnb.io/lottie/). Even complex animations can 
be displayed without having to write long custom rendering code.
The library can render the animations by piping the output from the Lottie
Android library into a Flutter texture.

You can [download](https://drive.google.com/file/d/1l3v6dLIXnR1M0ZIHwfnqHlVarlpGV1sp/view?usp=sharing)
a small Flutter app for Android showing what Fluttie looks like in action. This
app can also be used to easily preview animations from [lottiefiles.com](https://www.lottiefiles.com/popular),
which might be handy when deciding what animations to use.

**Please note:** At the moment, this plugin does not support iOS devices.

## Things to keep in mind when using this:
 - The plugin is in an early state and there will be breaking API changes
 - No iOS support yet
 - Loading animations from files is taking quite some time at the moment as the dart
   code needs to send the full animation declaration to the plugin. When
   [this issue](https://github.com/flutter/flutter/issues/11019) gets fixed, it might be faster.
   The time required also seems to be a lot shorter in non-debug builds.
 - Due to a delay between the dart code and the native backend, controlling
   multiple animations can be a bit laggy. Rendering multiple animations will
   also heavily reduce your apps framerate (Using multiple threads to circumvent this has already been implemented, but [crashes Flutter](https://github.com/flutter/flutter/issues/14169)).
 - Animation widgets need a fixed size for now
 - Do not re-use animations, as this can crash your app. Instead, save the output
   of `loadAnimationFromResource()` and construct a new animation whenever you need
   to.

## Getting Started

> The [example](https://github.com/simolus3/fluttie/tree/master/example) included
> contains a basic, but throughoutly commented, app illustrating how to include,
> load and show animations in Fluttie.

In order to use animations with fluttie, you will need a json file
containing the composition for your animation. You can either export it using
an AfterEffects plugin (see [instructions](http://airbnb.io/lottie/after-effects/getting-started.html))
or grab some at [lottiefiles.com](https://www.lottiefiles.com/).
Place the file in an folder inside your project and add it in the `assets` part of your `pubspec.yaml`.
If you haven't already done so, add Fluttie to your projects `dependencies`:
```yaml
dependencies:
  fluttie: ^0.1.2
```
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
After having your animation controller ready, you can include it as a widget:
```dart
Widget build(BuildContext context) =>
    new FluttieAnimation(emojiAnimation)
```
Don't forget to start your animation by calling `emojiAnimation.start()`!

## Questions and bugs

As the library is in an early version, there will be things not working like
they should. If you encounter a bug, have a question or want some features implemented,
please don't hesitate to [create an issue](https://github.com/simolus3/fluttie/issues/new).
Of course, would absolutely appreciate any contributions as pull requests.