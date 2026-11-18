# Privacy

Parked collects nothing and sends nothing anywhere. It has no internet permission, so it
cannot.

It asks for one permission, **location**, and uses it only when you press **Mark this spot** or
**How far?**: the phone's GPS is listened to from that press until it gives a position good to
about 15 m, or for 20 seconds after its first position, when the best one is kept; with nothing
after 45 seconds it gives up. Then the GPS is let go. It never reads location in the background, has no
location service, and never asks for location "all the time". If you allow only approximate location, it
says it needs precise and does nothing.

It keeps, in its own private storage on the phone: the last five spots you marked (latitude,
longitude, how exact the fix was, the time, and the note if you wrote one). Android does not back
it up and does not carry it to a new phone, and uninstalling the app removes all of it. **Forget
this spot** removes one sooner.

**How far?** compares a fresh position with a saved one on the phone and shows the answer. The
fresh position is not saved.

**Open in maps** and **Share** hand the spot to another app only when you press them, and what
that app does with it is that app's own business. The shared text holds the spot's name and
time, its note if it has one, the coordinates, a `geo:` link and an openstreetmap.org link;
whoever opens that link shows the place to OpenStreetMap.

## The parking meter (from 0.2.0)

The meter is a timer inside the app. It uses no location and nothing goes online. To ring on
time, and to ring at all, it needs these permissions, none of which you are asked for on the
Kompakt:

- **Alarms** (`SCHEDULE_EXACT_ALARM`): to ring at
  the minute the meter ends, and at the warning before it.
- **Notifications** (`POST_NOTIFICATIONS`): the ring shows as a notification, with the phone's alarm
  sound and a buzz (`VIBRATE`).
- **Showing over the lock screen** (`USE_FULL_SCREEN_INTENT`): the ringing screen comes up and
  turns the screen on, so the ring is seen as well as heard. It shows the note of the spot on
  show, if you wrote one, so anyone holding the locked phone while it rings can read it.
- **Ringing until you answer** (`FOREGROUND_SERVICE`, `WAKE_LOCK`): while the meter rings, and
  only then, a small service plays the alarm sound and keeps the phone awake for it, until you
  press Stop or two minutes go by.
- **Starting after a restart** (`RECEIVE_BOOT_COMPLETED`): an alarm does not survive the phone
  being switched off, so when it starts again the app sets a running meter's alarm again, and
  rings at once if the time came while it was off. The same happens when the clock or time zone
  is changed. Nothing else is done then.

The running meter (when it was set, when it ends, when it warns) is kept in the same private
storage as the spots and goes when the meter is canceled or cleared. The app also asks Android
whether DuraSpeed, the Kompakt's background manager, is installed, and reads Android's own record
of when the app was last stopped by the system, only to say whether DuraSpeed needs Parked
switched on in its list; neither leaves the phone.

The manifest declares the two location permissions Android asks for together
(`ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`), and one more that is not one you are asked
for: Android's support library adds it to every app built with it
(`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`), so that the app's own internal messages can reach
only the app itself.

The About dialog's source address opens GitHub in your browser, and **Feed the llamas** opens a
donation page run by Square, a payment company; each only when you press it. Nothing from the app
goes with either, and what you do there is under their own terms.

When Home is installed, the app reads Home's dark mode switch, a yes or no, so it is light or
dark with the rest of the phone, and sends Home nothing. A selection-menu entry hands the text
you selected in the note to the app you choose, and only then. Before opening a link the app
asks Android which browsers are installed, so the link opens in one.

Whether distances are in meters or feet follows the country of the phone's SIM card, or of its
language setting when there is no SIM; this is read on the phone and goes nowhere. Settings can
fix it to one or the other.

There are no ads, no analytics and no tracking.

<!-- shared-about privacy section -->
## Who we are

Parked is made by wander wildwood, an independent developer: one person, not a company. wander wildwood is the data controller for anything the app itself handles, and that is very little. Parked keeps what you put in it on your phone and sends nothing to the developer, so the developer holds no data about you.

## How to contact us

Write to email@wanderthe.dev about your data, this policy or anything else. The same address is in the app, under About.

## Your rights

If you are in the EU, the EEA or the UK, the GDPR gives you the right to:

- see the personal data held about you (access)
- have it corrected (rectification) or deleted (erasure)
- limit how it is used (restriction)
- object to its use (objection)
- get a copy to take elsewhere (portability)
- withdraw your consent at any time, where something rests on it
- complain to a data protection supervisory authority, for example the one where you live

Similar laws elsewhere, such as the CCPA in California, give you the right to know, delete and correct, and to opt out of the sale or sharing of your data. Nothing from Parked is sold or shared.

Since Parked keeps what it stores only on your phone, you can exercise most of these rights yourself: remove a spot with Forget this spot, cancel a running meter, clear the app's data in Android's settings, or uninstall the app, which deletes all of it. For anything you cannot do yourself, write to the address above.
<!-- /shared-about privacy section -->
