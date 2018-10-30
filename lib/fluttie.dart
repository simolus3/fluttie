import 'dart:async';
import 'dart:ui' show window;

import 'package:flutter/widgets.dart';
import 'package:flutter/services.dart';

/// A widget used to show previously created fluttie animations
class FluttieAnimation extends StatelessWidget {
  /// The animation that this widget should display
  final FluttieAnimationController data;

  /// The maximum size that this widget may occupy. Please note that, if this
  /// option does not influence the size in which the animation is rendered.
  /// Choosing a size significantly bigger than the size of the animation as
  /// specified in the animation file will cause some ugly aliasing, but this
  /// is the libraries fault and it should be fixed in the future.
  final Size size;

  FluttieAnimation(this.data, {this.size = Fluttie.kDefaultSize});

  Widget build(BuildContext ctx) {
    return data == null
        ? Container()
        : Container(
            constraints: BoxConstraints.loose(size),
            child: Texture(textureId: data.id));
  }
}

class FluttieAnimationController {
  /// The id of this animation as controlled by the plugin's backend
  final int id;

  /// The Fluttie instance used to create this controller which will be responsible
  /// for dispatching actions (play, pause, etc.) to the backend.
  final Fluttie _handle;

  FluttieAnimationController(this.id, this._handle);

  /// Starts this animation from the beginning
  void start() => _handle._startAnimation(id);

  /// Starts this animation from where it was last paused
  void unpause() => _handle._resumeAnimation(id);

  /// Pauses the animation at its current progess
  void pause() => _handle._pauseAnimation(id);

  /// Stops the animation, settings its progress either all the way back
  /// (rewind = true) or skipping to the end (rewind = true)
  void stopAndReset({bool rewind: false}) =>
      _handle._endAnimation(id, rewind: rewind);

  /// Stops the animation and disposes resources its holding in the backend.
  /// After calling this method, users should not use instances of the animation
  /// anymore, this includes not using any widgets referencing this animation.
  void dispose() => _handle.disposeAnimation(id);
}

/// Specifies how this animation should behave when repeating.
enum RepeatMode {
  /// After a full duration, rewind the animation instantaneously and start again
  START_OVER,

  /// After a full duration, continue playing, but backwards. Then the start has
  /// been reached, it will continue playing forwards again.
  REVERSE
}

/// Specifies how often an animation should repeat
class RepeatCount {
  /// If specified, a natural number indicating how often it should repeat
  final int amount;

  /// Whether the animation should repeat continuously
  final bool isInfinite;

  int get _internalCount => isInfinite ? -1 : amount;

  const RepeatCount._(this.amount, this.isInfinite);

  /// The animation will halt at its end and not repeat itself.
  const RepeatCount.dontRepeat() : this._(0, false);

  /// The animation will be repeated n times, resulting in n+1 full durations.
  const RepeatCount.nTimes(int n) : this._(n, false);

  /// The animation will be repeated infinitely.
  const RepeatCount.infinite() : this._(-1, true);
}

class Fluttie {

  static const Size kDefaultSize = const Size.square(100.0);

  /// Returns true iff the backend is ready to play animations
  static Future<bool> isAvailable() {
    return _methods.invokeMethod("isAvailable").then((d) => d as bool);
  }

  static const MethodChannel _methods = const MethodChannel("fluttie/methods");

  /// Given the composition of an animation to display, create a animation
  /// with detailed settings like its duration or how it should repeat.
  ///
  /// The parameter preferredSize controls the size at which the animation
  /// should be rendered natively before being submitted to the flutter frontend.
  /// When smaller compositions are drawn to bigger widgets without setting
  /// preparedSize, the resulting animation will be of low quality as aliasing
  /// occurs while scaling. Note that, even though using a big preferredSize
  /// reduces that effect, it can reduce the performance of rendering animations.
  /// By default, animations won't be scaled and use the size defined in the
  /// Lottie definition, no matter at what size they will be displayed.
  Future<FluttieAnimationController> prepareAnimation(
    int preparationId, {
    RepeatMode repeatMode = RepeatMode.START_OVER,
    RepeatCount repeatCount = const RepeatCount.nTimes(0),
    Duration duration,
    Size preferredSize,
  }) async {
    var scale = window.devicePixelRatio;

    int animId = await _methods.invokeMethod("prepareAnimation", {
      "composition": preparationId,
      "repeat_count": repeatCount._internalCount,
      "repeat_reverse": repeatMode == RepeatMode.REVERSE,
      "duration": duration?.inMilliseconds ?? 0,
      "pref_size_h": scale * (preferredSize?.height ?? -1.0),
      "pref_size_w": scale * (preferredSize?.width ?? -1.0),
    });

    return FluttieAnimationController(animId, this);
  }

  void _startAnimation(int id) {
    _methods.invokeMethod("startAnimation", {"id": id});
  }

  void _resumeAnimation(int id) {
    _methods.invokeMethod("resumeAnimation", {"id": id});
  }

  void _pauseAnimation(int id) {
    _methods.invokeMethod("pauseAnimation", {"id": id});
  }

  void _endAnimation(int id, {bool rewind = false}) {
    _methods.invokeMethod("endAnimation", {"id": id, "reset_start": rewind});
  }

  void disposeAnimation(int id) {
    _methods.invokeMethod("disposeAnimation", {"id": id});
  }

  /// Load the composition of an animation from a json string
  Future<int> loadAnimationFromJson(String json) =>
      _loadAnimation("inline", json);

  /// Load the composition of an animation from an asset bundle and return the
  /// id of the animation parsed.
  ///
  /// Please notice that using the bundle parameter is no longer required as the
  /// plugin now uses loads the resource from the OEM side as intended.
  Future<int> loadAnimationFromAsset(String key) => _loadAnimation("asset", key);

  /// Load the composition of an animation from an asset bundle and return the
  /// id of the animation parsed.
  /// 
  /// Deprecated: To better align this plugin with the terminology used by
  /// Flutter, "resource" has been changed to "asset", so please use
  /// [loadAnimationFromAsset] instead.
  @deprecated
  Future<int> loadAnimationFromResource(String key,
    {@deprecated AssetBundle bundle}) => loadAnimationFromAsset(key);

  Future<int> _loadAnimation(String sourceType, String data) async {
    return await _methods.invokeMethod(
        "loadAnimation", {"source_type": sourceType, "source": data});
  }
}
