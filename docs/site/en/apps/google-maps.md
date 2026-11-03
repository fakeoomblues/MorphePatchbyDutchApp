# Google Maps

**App version:** 26.43.05.1002020710 · **Build:** 1068844719 · **Original file:** APK

[Use your original APK](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Select the complete original APK for this exact version in Morphe Manager. Android 10 or newer is required.

## Native Maps in Gearslip and Android Auto

Our **Native navigation for Gearslip and Android Auto** patch runs the patched app's original car map and navigation interface in both hosts. It works with a renamed package, so Ungoogled Maps can appear beside stock Maps. DriveDeck is accepted like Gearslip. Search uses the host's keyboard, and the native Maps screen handles results, route previews and turn-by-turn guidance.

![Ungoogled Maps running its native map in the official Android Auto Desktop Head Unit](../../assets/images/maps/native-android-auto.png){ loading=lazy }

*The renamed patched app in Google's Android Auto Desktop Head Unit, using a test location in Amsterdam.*

[Set up Ungoogled Maps for Android Auto](../guides/android-auto.md){ .md-button }

### Why Maps needed a different approach

Waze exposes a standard Android for Cars navigation service that car hosts can discover. The examined Maps version uses Google's private projection service instead. After renaming, Gearslip could mistake its notification capability for a messaging app and open an empty messages screen. Adding a navigation label would not make those two interfaces communicate.

We built a navigation adapter that connects Gearslip and Android Auto to the patched copy's own projection service and original car renderer. Maps also loads part of its car interface from the full Android Auto installation, which is why that app must be installed even when using Gearslip.

Signing the patched APK creates another problem: Google's optional private vehicle-data client rejects the new signing certificate. We isolated that client so it cannot crash the map, while retaining GPS navigation. Search needed a separate bridge between the host's keyboard and Maps' native editor. The map and editor also have to survive display resizing, keyboard changes and reconnects.

Native search, route start, guidance and reconnect were checked in Gearslip and Google's official Android Auto Desktop Head Unit. Those checks do not establish every phone, car or live-road setup. The [engineering report](https://codeberg.org/JohninNL/nl-morphe-patches/src/branch/main/docs/research/2026-11-02-maps-native-car-ui.md) records the implementation and test limits.

## Waze and Flitsmeister data inside Maps

Our extension to **Waze alerts** lets you choose patched Waze or Flitsmeister as a local navigation-data source. Alongside road-alert pins, it can show current speed, the road limit and an observed average through an average-speed zone, with progress and remaining distance. Available reports can also produce advance warnings.

This uses our **Broadcast navigation data** patch in the provider app. Installing ordinary Waze or Flitsmeister is not enough. The original Waze **Share alerts with Maps** mode remains available as a separate alert-sharing choice.

[Set up Waze or Flitsmeister data in Google Maps and Vela Maps](../guides/navigation-data.md){ .md-button .md-button--primary }

## Where these patches come from

The 30 original Maps controls come from [bearinmind patches](https://github.com/bearinmindcat/morphe-patches) by bearinmindcat. We adapted them for Maps 26.43.05.1002020710, extended **Waze alerts** with our shared navigation-data integration, added light/dark launcher choices to **Change app name**, and added **Native navigation for Gearslip and Android Auto**. The inherited display, offline-map, privacy and other controls keep their upstream credit.

Select Maps patches from **Dutch App Patches only** for a patching attempt. Our bundle already contains the adapted upstream controls. Selecting overlapping Maps patches from bearinmind patches as well can run incompatible versions of the same shared hooks.

## Patches

### Added by this project

| Patch | Default | What it does |
| --- | --- | --- |
| **Native navigation for Gearslip and Android Auto** | On | Adds a navigation entry that runs this patched app's original Android Auto map UI, including under its separate package name. Requires the full Android Auto app for its car UI resources. |

### Extended by this project

| Patch | Default | What it does |
| --- | --- | --- |
| **Waze alerts** | On | Adds Waze and Flitsmeister as local sources for road-alert pins, approach chimes, speed, limits and observed average-speed zones. Starts off. |
| **Change app name** | On | Sets the launcher and app-switcher name (default: Ungoogled Maps) and offers six supplied icon styles on light or dark backgrounds. |

### Adapted from bearinmind patches

| Patch | Default | What it does |
| --- | --- | --- |
| **120 refresh rate** | On | Lets Maps use the screen’s supported refresh rate. Enable it in Customization; it can use more battery. |
| **Account sheet cleanup** | On | Removes the extra account-menu heading and keeps the menu open when returning from Settings or Customization. |
| **Add microG-RE support** | Off | Adds Google sign-in through microG-RE or ReVanced GmsCore while keeping the name from Change app name. |
| **Add original microG support** | Off | Uses original microG for Google sign-in on a ROM with signature spoofing; keeps the stock package name. |
| **Any media app** | On | Adds compatible Android Auto music and podcast apps to Navigation’s Default media app choices. |
| **Avoid Flock cameras** | On | Offers driving detours around mapped Flock cameras using BRouter. Starts off; routes with stops are unchanged. |
| **Better offline maps** | On | Adds a resizable offline-area picker, splits large areas into downloads and shows downloaded coverage. |
| **Black theme** | On | Uses black surfaces in dark mode, including navigation. |
| **Blue pin** | On | Uses a flat blue pin for Maps product logos and the search icon. |
| **Bypass Play Services checks** | On | Lets the patched app run with unavailable or disabled Play services and preserves map, search and routing access. |
| **Change package name** | On | Installs alongside stock Maps under a separate package name. |
| **Customization screen** | On | Adds Customization to the account menu for the available in-app patch switches. |
| **Flock cameras** | On | Shows mapped number-plate reader cameras with approach notes and chimes. Starts off; uses OpenStreetMap data. |
| **Follow playing media** | Off | Shows media from apps that do not connect directly to Maps. Requires notification access. |
| **Hide ads and clutter** | On | Hides promoted pins, sponsored results, AI summaries, place-sheet business rows and the Explore feed and bottom tabs. |
| **Legacy icon** | On | Restores the flat multicolour Maps pin used before the gradient icon, unless an explicit App icon is selected in Change app name. |
| **Location provider toggle** | On | Adds a choice of Android, Google Play services or microG-RE location, with network-location fallback. |
| **Offline saved places** | On | Saves local places and lists, imports and exports backups, and adds an optional local Timeline recorder. |
| **Power saving mode** | On | Adds a black navigation screen and settings for its button, idle activation, speedometer and frame rate. |
| **Proxy** | On | Adds an HTTP proxy setting for Maps traffic; map loading stops if the configured proxy is unavailable. |
| **Rectangle shapes** | On | Squares off rounded interface corners and navigation buttons. |
| **Remove permissions** | On | Removes selected account, advertising and sensor permissions, including microphone and camera. Voice search, Lens and Live View stop working. |
| **Remove sign-in prompts** | On | Hides first-launch, search and account-menu sign-in reminders. |
| **Remove telemetry** | On | Reduces advertising pings, usage measurement, performance monitoring, surveys and background learning services. |
| **Speed cameras** | On | Shows OpenStreetMap speed and red-light cameras with approach notes and chimes. Starts off. |
| **Speeding chime** | On | Adds a chime when speed exceeds the known road limit; configure the threshold in the external-data settings. |
| **Trim account menu** | On | Removes Timeline, Location sharing, Your data in Maps and Help & feedback from the account menu. |
| **Zoom controls in navigation** | On | Adds zoom-in, zoom-out and reset buttons while the navigation camera keeps following. |

## Advertising and tracking services

**Remove telemetry** and **Hide ads and clutter** reduce the listed advertising and measurement features. Maps still uses Google’s services for maps, search, routes and any enabled account features. The selected Waze or Flitsmeister provider continues using its own services.

External navigation data is received locally from the selected app on the same phone. OpenStreetMap camera layers download nearby camera positions. **Avoid Flock cameras** sends the route’s start and end to `brouter.de` when enabled. These controls start off in the external-data settings.

## Before using

The native FM event/speech upgrade is still under integrated validation. Pinned producer capture has passed its coverage audit; receiving audio, routing and lifecycle acceptance continue. Its category/output review, parked preview and recovery controls are described in the [native-warning setup section](../guides/navigation-data.md#native-flitsmeister-warning-audio).

Updated navigation-data patches add silent provider startup and native-engine health checks. After one-time provider setup and location permission, Maps/Vela starts the selected Waze or Flitsmeister engine without opening its screen, and retries recoverable stoppages while the map is in use. Repatch both sides to add the startup API; the older Share alerts bridge keeps its manual startup requirement. See the [navigation-data setup guide](../guides/navigation-data.md).

Keep **Change package name** selected to install beside stock Maps; the default package is `org.ungoogled.android.apps.maps`. **Change app name** uses **Ungoogled Maps** by default and always keeps your chosen name with either microG support patch. **Add microG-RE support** changes the default package to `org.ungoogled.android.apps.maps.microg`. The two microG support patches are alternatives: choose only the one matching your setup. Original microG needs a ROM with signature spoofing and keeps the stock Maps package.

In **Change app name**, use **App icon** to choose Classic pin, Outlined pin, Classic map tile, Raised map pin, Gradient pin, or Folded map, each on a light or dark background. **Automatic** follows the **Legacy icon** patch; **Original Google Maps** keeps Google’s original launcher icon. An explicit choice takes precedence over Legacy icon. These choices apply to the regular and round launcher icons. Explicit choices leave the stock splash screen in place; in-app artwork still follows your other selected patches, such as Blue pin. With Automatic, Legacy icon keeps its existing splash-screen behavior. Name and icon can be configured independently, and changing them requires patching again.

For native car navigation, keep **Native navigation for Gearslip and Android Auto** selected, install the full Android Auto app and grant the patched Maps app precise location access. Gearslip then discovers one navigation tile instead of treating Maps as a messenger. For Android Auto, install the patched APK through KingInstaller and check its installation result; use its Shizuku mode if the normal method does not record the expected installer. Select **Ungoogled Maps** or your chosen app name in the car launcher so the session uses the patched copy.

Open the account menu’s **Customization** and **Waze & external data source** rows to configure the selected patches. A patch marked **On** above is selected by default when patching; camera layers and external sources still require enabling in the app. **120 refresh rate** also starts off in Customization.

Follow the [navigation-data setup guide](../guides/navigation-data.md) to patch a provider, select it in Maps and configure the compact speed display, map pins and advance chimes. It also explains the separate legacy Waze mode, partial averages and fallback when data expires.

**Remove permissions** disables voice search, Lens and Live View through its microphone/camera changes. The microG-RE account build leaves account-related permissions and services in place. **Proxy** needs a supported network engine; follow its in-app availability message. **Follow playing media** needs notification access and is not intended for root mount installs. YouTube Music and Spotify’s direct media integration still depends on Google’s own Maps; use the available notification-based option where supported.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md) · [Compatibility and defaults](../reference/catalogue.md)
