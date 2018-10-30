## 0.3.1
 * Simplified logic to handle composition parsing to fix a race condition
 * Temporarily disable this plugin on pre-Lollipop devices. A bug in the Flutter
   engine could otherwise crash your apps. This will be reverted once a version
   with a fix lands in flutter beta.
   
Again, thanks to [@kristoffer-zliide](https://github.com/kristoffer-zliide) for
these changes!

## 0.3
 * Updated the Lottie libray used, thus giving you access to the latest
   features including image support in animations. Thank you, [@kristoffer-zliide](https://github.com/kristoffer-zliide), for implementing this!

## 0.2
 * Make plugin ready for dart 2.

## 0.1.2
 * Make the plugin only react to activity lifecircle changes that involve the
   main flutter activity.

## 0.1.1
 * Fix crashes occuring when disposing animations
 * Ability to unpause animations without having to restart them

## 0.1.0
 * Render small animations in high quality by using the `preferredSize`
   parameter of `Fluttie.prepareAnimation`.
 * Reduce resource usage by pausing aimations while the app is in background
 * Implement multithreaded rendering, not yet enabled because of an issue in the
   Flutter framework.

## 0.0.1
* First release with basic functionality
