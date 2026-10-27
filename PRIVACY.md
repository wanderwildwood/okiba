# Privacy

Parked collects nothing and sends nothing anywhere. It has no internet permission, so it
cannot.

It asks for one permission, **location**, and uses it only when you press **Mark this spot** or
**How far?**: the phone's GPS is listened to from that press until it gives a position good to
about 15 m, or for 20 seconds after its first position, when the best one is kept; with nothing
after 45 seconds it gives up. Then the GPS is let go. It does not run in the background, has no
service, and never asks for location "all the time". If you allow only approximate location, it
says it needs precise and does nothing.

It keeps, in its own private storage on the phone: the last five spots you marked (latitude,
longitude, how exact the fix was, the time, and the note if you wrote one). Android does not back
it up and does not carry it to a new phone, and uninstalling the app removes all of it. **Forget
this spot** removes one sooner.

**How far?** compares a fresh position with a saved one on the phone and shows the answer. The
fresh position is not saved.

**Open in maps** and **Share** hand the spot to another app only when you press them, and what
that app does with it is that app's own business.

The manifest declares the two location permissions Android asks for together
(`ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`), and one more that is not one you are asked
for: Android's support library adds it to every app built with it
(`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`), so that the app's own internal messages can reach
only the app itself.

The About dialog's source address and its last line open a web address in your browser, and
only when you press them.

Whether distances are in metres or feet follows the country of the phone's SIM card, or of its
language setting when there is no SIM; this is read on the phone and goes nowhere. Settings can
fix it to one or the other.

There are no ads, no analytics and no tracking.
