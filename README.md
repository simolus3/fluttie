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
 - No iOS support yet
 - Due to a delay between the dart code and the native backend, controlling
   multiple animations can be a bit laggy. Rendering multiple animations will
   reduce your app's framerate.
 - Animation widgets need a fixed size for now
 - Do not re-use animations, as this can crash your app. Instead, save the output
   of `loadAnimationFromAsset()` and construct a new animation whenever you need
   to.

## Getting Started

> The [example](https://github.com/simolus3/fluttie/tree/master/example) included
> contains a basic, but throughoutly commented, app illustrating how to include,
> load and show animations in Fluttie.

In order to use animations with fluttie, you will need a json file
containing the composition for your animation. You can either export it using
an AfterEffects plugin (see [instructions](http://airbnb.io/lottie/after-effects/getting-started.html))
or grab some at [lottiefiles.com](https://www.lottiefiles.com/). A
composition is a JSON file that describes everything in your animation,
like the different shapes, colors and movements.
Place the file in an folder inside your project and add it in the
`assets` part of your `pubspec.yaml`, like its done [here](https://github.com/simolus3/fluttie/blob/master/example/pubspec.yaml#L29-L31).
If you haven't already done so, also add Fluttie to your projects' `dependencies`:
```yaml
dependencies:
  fluttie: ^0.3.0
```
In order to display animations in Flutter, you will have the plugin load
a composition first. The plugin will parse the composition file so
that it can quickly display the animation later on.
```dart
var instance = Fluttie();
var emojiComposition = await instance.loadAnimationFromAsset(
    "assets/animations/emoji.json", //Replace this string with your actual file
);
```
In order to actually show the animation on screen, two parts are neccessary:
An `AnimationController`, which controls the instance of the animation
and contains methods to pause and resume it, and finally a widget displaying
the animation contained by the controller.
```dart
// This creates the controller
var emojiAnimation = await instance.prepareAnimation(emojiComposition)
```
You can also set the duration of your animation and configure looping while
preparing the animation by using the optional parameters of `prepareAnimation`, see
the docs fore more details.
After having your animation controller ready, you can include it as a widget:
```dart
Widget build(BuildContext context) =>
    FluttieAnimation(emojiAnimation)
```
Don't forget to start your animation by calling `emojiAnimation.start()`
on your controller!

## Questions and bugs

As the library is in an early version, there will be things not working like
they should. If you encounter a bug, have a question or want some features implemented,
please don't hesitate to [create an issue](https://github.com/simolus3/fluttie/issues/new).
Of course, I would absolutely appreciate any contributions as pull requests.
