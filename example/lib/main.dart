import 'package:flutter/material.dart';
import 'package:fluttie/fluttie.dart';

void main() => runApp(MyApp());

/// An example app showcasing the features of fluttie animations. It should
/// show a emoji animation at the top and a bar of stars below it. When tapping
/// on a star, all the stars to the left should be filled in a beautiful animation.
class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {

  /// Animation to display a shocked emoji taken from lottiefiles.com
  FluttieAnimationController shockedEmoji;

  /// A list of animations representing the stars shown in a horizontal line.
  /// They will be started when the user tabs on one of them.
  List<FluttieAnimationController> starAnimations = [];
  /// The amount of stars currently selected in the line. Used to compute the
  /// difference on a tap.
  int currentlySelectedStars = 0;

  /// If we're ready to show the animations. Set to true after they have been
  /// loaded by the plugin.
  bool ready = false;

  /// The performance overlay can be triggered by tapping on the icon on the
  /// right-hand side of the toolbar. It can be used to invastigate the
  /// performance impact of running different animations.
  bool showPerformanceOverlay = false;

  @override
  initState() {
    super.initState();
    /// Load and prepare our animations after this widget has been added
    prepareAnimation();
  }

  @override
  dispose() {
    super.dispose();
    /// When this widget gets removed (in this app, that won't happen, but it
    /// can happen for widgets using animations in other situations), we should
    /// free the resources used by our animations.
    shockedEmoji?.dispose();
    starAnimations.forEach((anim) => anim.dispose());
  }

  // async because the plugin will have to do some background-work
  prepareAnimation() async {
    // Checks if the platform we're running on is supported by the animation plugin
    bool canBeUsed = await Fluttie.isAvailable();
    if (!canBeUsed) {
      print("Animations are not supported on this platform");
      return;
    }

    var instance = Fluttie();
    
    // Load our first composition for the emoji animation
    var emojiComposition = await instance.loadAnimationFromAsset(
      "assets/animations/emoji_shock.json");
    // And prepare its animation, which should loop infinitely and take 2s per
    // iteration. Instead of RepeatMode.START_OVER, we could have choosen 
    // REVERSE, which would play the animation in reverse on every second iteration.
    shockedEmoji = await instance.prepareAnimation(emojiComposition,
       duration: const Duration(seconds: 2),
       repeatCount: const RepeatCount.infinite(), repeatMode: RepeatMode.START_OVER);

    // Load the composition for our star animation. Notice how we only have to
    // load the composition once, even though we're using it for 5 animations!
    var composition = await instance.loadAnimationFromAsset(
      "assets/animations/star.json");

    // Create the star animation with the default setting. 5 times. The
    // preferredSize needs to be set because the original star animation is quite
    // small. See the documentation for the method prepareAnimation for details.
    for (int i = 0; i < 5; i++) {
      starAnimations.add(
        await instance.prepareAnimation(
          composition, preferredSize: Fluttie.kDefaultSize
        )
      );
    }

    // Loading animations may take quite some time. We should check that the
    // widget is still used before updating it, it might have been removed while
    // we were loading our animations!
    if (mounted) {
      setState(() {
        ready = true; // The animations have been loaded, we're ready
        shockedEmoji.start(); //start our looped emoji animation
      });
    }
  }

  void animateStarChange(int updated) {
    /* When the amount of stars that are selected changes, we will have to
       play or stop animations in order to reflect that in the UI. For that, we
       start the animation for each star that was not previously selected but
       should be selected now (i >= old) and (i < currentlySelectedStars).
       If the amount to show has been reduced, we can simply rewind the animation
       to the beginning, as that will show a hollow star not selected.

       Notice how i refers to the index starting at 0, while the amount should
       start at 1.
    */

    setState(() {
      int old = this.currentlySelectedStars;
      currentlySelectedStars = updated;
      
      for (int i = 0; i < 5; i++) {
        if (i < currentlySelectedStars) { //Star needs to be shown
          if (i >= old) //Star would otherwise already be shown
            starAnimations[i].start();
        } else { //Remove star, reset to beginning
          starAnimations[i].stopAndReset(rewind: true);
        }
      }
    });
  }

  /// Builds a widget animating the star at the specified index.
  Widget buildStar(int i) {
    return Flexible(
      child: GestureDetector(
        onTap: () {
          // Update the amount of stars that have been selected when this star
          // is tapped.
          int amountOfStars = i + 1;
          animateStarChange(amountOfStars);
        },
        child: FluttieAnimation(starAnimations[i])
      )
    );
  }

  /// When we're ready to show the animations, this method will create the main
  /// content showcasing them.
  Widget buildStarContent(BuildContext context) {
    return Column(
      children: [
        //Display the emoji animations at the top
        FluttieAnimation(shockedEmoji),
        //followed by a row of 5 stars that can be tapped
        Row(
          mainAxisSize: MainAxisSize.max,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            buildStar(0), buildStar(1), buildStar(2), buildStar(3), buildStar(4)
          ],
        ),
        // Use a Flexible widget so that the rest will be on the bottom
        Flexible(child: Container()),
        // And display some credits showing where the animations are taken from
        RichText(
          text: TextSpan(
            children: [
              TextSpan(
                text: "Animations taken from ", 
                style: Theme.of(context).textTheme.body1.copyWith(color: Colors.grey)
              ),
              TextSpan(
                text: "lottiefiles.com",
                style: Theme.of(context).textTheme.body2
              )
            ]
          ),
        ),
        Padding(padding: const EdgeInsets.only(bottom: 10.0)),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    // Display the main content, or - when we're not ready for that yet - a text
    // informing the user that the animations are being prepared.
    Widget content = ready ? buildStarContent(context) : Text("Loading animations");

    return MaterialApp(
      showPerformanceOverlay: this.showPerformanceOverlay,
      home: Scaffold(
        appBar: AppBar(
          title: Text('Fluttie example'),
          actions: [
            // Button to toggle the performance overlay
            IconButton(
              icon: const Icon(Icons.build),
              onPressed: () {
                setState(() {
                  showPerformanceOverlay = !showPerformanceOverlay;
                });
              },
            )
          ],
        ),
        body: Center(child: content)
      ),
    );
  }
}
