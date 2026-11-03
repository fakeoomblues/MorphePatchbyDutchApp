# Vela Maps

**App version:** 0.5.2621 · **Build:** 46210 · **Original file:** APK

[Download APK](https://buzzheavier.com/64jger382td4){ .md-button .md-button--primary }

Select the complete original APK for this exact version in Morphe Manager.

Use patched Waze or Flitsmeister as a local source for current speed, limits and available road warnings while Vela handles your route. The provider can remain in the background with its overlay off.

[Set up Waze or Flitsmeister data in Google Maps and Vela Maps](../guides/navigation-data.md){ .md-button }

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **External speed controls** | On | Adds De-Waze and Flitsmeister as speed/road-warning providers in Vela and Android Auto, with compact speed, limit, partial-average and advance-warning details. |

## Advertising and tracking services

This patch adds no separate account, advertising service or telemetry upload. The selected provider continues using its own maps, traffic/location exchange and reporting services. Vela keeps its existing online and location behavior.

Road-warning reports can include camera/incident positions. The integration receives them locally from the provider on the same device; it does not export Vela's device location to the provider.

## Before using

The native FM event/speech upgrade is still under integrated validation. Pinned producer capture has passed its coverage audit; receiving audio, routing and lifecycle acceptance continue. Its category/output review, parked preview and recovery controls are described in the [native-warning setup section](../guides/navigation-data.md#native-flitsmeister-warning-audio).

Updated navigation-data patches add silent provider startup and native-engine health checks. After one-time provider setup and location permission, Maps/Vela starts the selected Waze or Flitsmeister engine without opening its screen, and retries recoverable stoppages while the map is in use. Repatch both sides to add the startup API; the older Share alerts bridge keeps its manual startup requirement. See the [navigation-data setup guide](../guides/navigation-data.md).

Patch [Waze 5.26.90.901](waze.md) or [Flitsmeister 14.17](flitsmeister.md) with **Broadcast navigation data**, complete its first-run setup and grant its driving service location access once. Updated patches start its engine automatically when Vela needs it. Its overlay can stay off. The provider patch's optional **Receiver package** can be set to `app.vela`. Android 14 and newer allow Vela to verify the sending provider through Android; older versions lack that sender verification.

In Vela's navigation settings, open **Speed and road alerts** and choose **Vela (native data)**, **De-Waze** or **Flitsmeister**. The choice is saved. The same choice is available under **Navigation provider** in Android Auto settings. **Spoken road warnings** follows Vela's voice/mute settings; car details have their own switch.

With an external provider selected, the compact map display keeps current speed beside the speed-limit sign. An active average-speed zone adds the observed average, a progress bar, percentage and remaining distance below it. Averages cover only the observed portion, including when the provider is opened mid-zone. The average stays unknown until enough distance/time has been observed; approximate progress is marked with **≈**. Advance warnings use real pre-entry observations or fresh report positions. Available reports include static/mobile cameras, police, hazards, crashes, jams, closures and enabled provider Flock cameras. Coverage depends on the provider; estimated distances remain approximate.

Missing or expired values use Vela's own data where available. The provider status identifies fallback, and an unavailable current speed shows **—**. Changing provider, leaving a zone or losing its fresh observations removes that partial average. Data from the two providers is kept separate.

Selecting Flitsmeister keeps Vela's saved Flock display, voice and detour choices active. General FM speed, limit or warning data does not replace those independent features. De-Waze retains its existing handoff only while the selected Waze feed is fresh; an unselected feed cannot pause native Flock.

Phone settings, background reception, compact phone/car rendering and advance speech were checked on an Android 16 emulator using Gearslip as the car host. Physical driving, a physical head unit and Google’s Android Auto host have not been validated. An already-started native Flock refresh can finish while its display and alerts are paused.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md) · [Compatibility and defaults](../reference/catalogue.md)
