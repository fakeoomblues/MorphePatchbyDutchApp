# Google Maps

**Appversie:** 26.43.05.1002020710 · **Build:** 1068844719 · **Origineel bestand:** APK

[Gebruik je originele APK](../guides/downloads.md#local-original-inputs){ .md-button .md-button--primary }

Selecteer de volledige originele APK voor deze exacte versie in Morphe Manager. Android 10 of nieuwer is vereist.

## Native Maps in Gearslip en Android Auto

Onze patch **Native navigation for Gearslip and Android Auto** gebruikt in beide hosts de oorspronkelijke autokaart en navigatie-interface van de gepatchte app. Dit werkt ook met een gewijzigde pakketnaam, zodat Ungoogled Maps naast de originele Maps-app kan staan. DriveDeck wordt net als Gearslip geaccepteerd. Zoeken gebruikt het toetsenbord van de host. Het native Maps-scherm verzorgt resultaten, routevoorbeelden en navigatie-instructies.

![Ungoogled Maps met de native kaart in de officiële Android Auto Desktop Head Unit](../../assets/images/maps/native-android-auto.png){ loading=lazy }

*De hernoemde gepatchte app in Googles Android Auto Desktop Head Unit, met een testlocatie in Amsterdam.*

[Ungoogled Maps instellen voor Android Auto](../guides/android-auto.md){ .md-button }

### Waarom Maps een andere aanpak nodig had

Waze heeft een standaard navigatiedienst voor Android for Cars die autohosts kunnen vinden. De onderzochte Maps-versie gebruikt daarentegen Googles eigen projectiedienst. Na het hernoemen kon Gearslip de meldingsfunctie als berichtenapp herkennen en een leeg berichtenscherm openen. Alleen een navigatielabel toevoegen laat die twee interfaces nog niet met elkaar communiceren.

We bouwden een navigatieadapter die Gearslip en Android Auto verbindt met de eigen projectiedienst en oorspronkelijke autoweergave van de gepatchte versie. Maps laadt ook een deel van de auto-interface uit de volledige Android Auto-installatie. Daarom moet die app ook aanwezig zijn als je Gearslip gebruikt.

Het ondertekenen van de gepatchte APK veroorzaakt nog een probleem: Googles optionele privéclient voor voertuiggegevens weigert het nieuwe ondertekeningscertificaat. We hebben die client geïsoleerd zodat hij de kaart niet laat crashen, terwijl GPS-navigatie blijft werken. Zoeken vroeg om een aparte koppeling tussen het toetsenbord van de host en het native zoekveld van Maps. De kaart en het zoekveld moeten bovendien behouden blijven bij schermwijzigingen, het openen van het toetsenbord en opnieuw verbinden.

Native zoeken, een route starten, navigeren en opnieuw verbinden zijn gecontroleerd in Gearslip en Googles officiële Android Auto Desktop Head Unit. Daarmee is niet iedere telefoon, auto of rit op de weg getest. Het [technische verslag](https://codeberg.org/JohninNL/nl-morphe-patches/src/branch/main/docs/research/2026-11-02-maps-native-car-ui.md) beschrijft de uitvoering en testgrenzen.

## Gegevens van Waze en Flitsmeister in Maps

Onze uitbreiding op **Waze alerts** laat je gepatchte Waze of Flitsmeister kiezen als lokale bron van navigatiegegevens. Naast wegmeldingen op de kaart kan deze koppeling actuele snelheid, de maximumsnelheid en een waargenomen gemiddelde tijdens een trajectcontrole tonen, met voortgang en resterende afstand. Beschikbare meldingen kunnen ook voorwaarschuwingen geven.

Hiervoor is onze patch **Broadcast navigation data** in de bronapp nodig. Alleen de gewone Waze of Flitsmeister installeren is niet voldoende. De oorspronkelijke Waze-modus **Share alerts with Maps** blijft als aparte keuze voor het delen van meldingen beschikbaar.

[Waze of Flitsmeister koppelen aan Google Maps en Vela Maps](../guides/navigation-data.md){ .md-button .md-button--primary }

## Waar komen deze patches vandaan?

De 30 oorspronkelijke Maps-opties komen uit [bearinmind patches](https://github.com/bearinmindcat/morphe-patches) van bearinmindcat. We pasten ze aan voor Maps 26.43.05.1002020710, breidden **Waze alerts** uit met onze gedeelde navigatiegegevens, voegden lichte en donkere launchericonen toe aan **Change app name**, en voegden **Native navigation for Gearslip and Android Auto** toe. De overgenomen scherm-, offlinekaart-, privacy- en overige opties blijven aan de oorspronkelijke maker toegeschreven.

Kies bij het patchen van Maps alleen patches uit **Dutch App Patches**. Onze bundel bevat de aangepaste oorspronkelijke opties al. Ook overlappende Maps-patches uit bearinmind patches kiezen kan verschillende versies van dezelfde gedeelde koppelingen uitvoeren.

## Patches

### Toegevoegd door dit project

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Native navigation for Gearslip and Android Auto** | Aan | Voegt een navigatie-ingang toe die de oorspronkelijke Android Auto-kaartinterface van deze gepatchte app gebruikt, ook onder een aparte pakketnaam. Vereist de volledige Android Auto-app voor de auto-interface. |

### Uitgebreid door dit project

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Waze alerts** | Aan | Voegt Waze en Flitsmeister toe als lokale bronnen voor wegmeldingen, voorwaarschuwingen, snelheid, limieten en waargenomen trajectcontroles. Begint uit. |
| **Change app name** | Aan | Stelt de naam in de launcher en appkiezer in (standaard: Ungoogled Maps) en biedt zes meegeleverde icoonstijlen met een lichte of donkere achtergrond. |

### Aangepast vanuit bearinmind patches

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **120 refresh rate** | Aan | Laat Maps de ondersteunde verversingssnelheid van het scherm gebruiken. Zet dit aan in Customization; het kan meer batterij verbruiken. |
| **Account sheet cleanup** | Aan | Verwijdert de extra kop in het accountmenu en houdt het menu open bij terugkeer uit Settings of Customization. |
| **Add microG-RE support** | Uit | Voegt Google-aanmelding via microG-RE of ReVanced GmsCore toe en behoudt de naam uit Change app name. |
| **Add original microG support** | Uit | Gebruikt originele microG voor Google-aanmelding op een ROM met signature spoofing; behoudt de oorspronkelijke pakketnaam. |
| **Any media app** | Aan | Voegt geschikte muziek- en podcastapps voor Android Auto toe aan de keuze Default media app in Navigation. |
| **Avoid Flock cameras** | Aan | Biedt autoroutes om bekende Flock-camera’s heen via BRouter. Begint uit; routes met tussenstops blijven gelijk. |
| **Better offline maps** | Aan | Voegt een aanpasbaar selectievak voor offlinegebieden toe, verdeelt grote gebieden over downloads en toont gedownloade dekking. |
| **Black theme** | Aan | Gebruikt zwarte vlakken in donkere modus, ook tijdens navigatie. |
| **Blue pin** | Aan | Gebruikt een platte blauwe speld voor Maps-logo’s en het zoekpictogram. |
| **Bypass Play Services checks** | Aan | Laat de gepatchte app werken met ontbrekende of uitgeschakelde Play services en behoudt toegang tot kaarten, zoeken en routes. |
| **Change package name** | Aan | Installeert naast de oorspronkelijke Maps-app onder een andere pakketnaam. |
| **Customization screen** | Aan | Voegt Customization toe aan het accountmenu voor de beschikbare patchschakelaars. |
| **Flock cameras** | Aan | Toont bekende kentekencamera’s met een melding en geluid bij nadering. Begint uit; gebruikt OpenStreetMap-gegevens. |
| **Follow playing media** | Uit | Toont media van apps die niet rechtstreeks met Maps verbinden. Vereist meldingstoegang. |
| **Hide ads and clutter** | Aan | Verbergt gesponsorde spelden en zoekresultaten, AI-samenvattingen, bedrijfsrijen, de Explore-feed en onderste tabbladen. |
| **Legacy icon** | Aan | Herstelt de platte meerkleurige Maps-speld van vóór het verloopicoon, tenzij een expliciet App icon is gekozen in Change app name. |
| **Location provider toggle** | Aan | Voegt een keuze toe tussen locatie via Android, Google Play services of microG-RE, met terugval op netwerklocatie. |
| **Offline saved places** | Aan | Bewaart plaatsen en lijsten lokaal, importeert en exporteert back-ups en voegt een optionele lokale Timeline-opname toe. |
| **Power saving mode** | Aan | Voegt een zwart navigatiescherm toe en instellingen voor de knop, automatisch openen, snelheidsmeter en beeldsnelheid. |
| **Proxy** | Aan | Voegt een HTTP-proxyinstelling voor Maps-verkeer toe; kaarten laden niet door als de ingestelde proxy onbereikbaar is. |
| **Rectangle shapes** | Aan | Maakt afgeronde hoeken en navigatieknoppen rechthoekig. |
| **Remove permissions** | Aan | Verwijdert geselecteerde account-, advertentie- en sensorrechten, waaronder microfoon en camera. Spraakzoeken, Lens en Live View werken daardoor niet. |
| **Remove sign-in prompts** | Aan | Verbergt aanmeldverzoeken bij de eerste start, bij zoeken en in het accountmenu. |
| **Remove telemetry** | Aan | Vermindert advertentiemeldingen, gebruiksmetingen, prestatiemetingen, enquêtes en leerdiensten op de achtergrond. |
| **Speed cameras** | Aan | Toont flitsers en roodlichtcamera’s uit OpenStreetMap met een melding en geluid bij nadering. Begint uit. |
| **Speeding chime** | Aan | Voegt een geluid toe wanneer de snelheid de bekende limiet overschrijdt; stel de drempel in bij de externe gegevens. |
| **Trim account menu** | Aan | Verwijdert Timeline, Location sharing, Your data in Maps en Help & feedback uit het accountmenu. |
| **Zoom controls in navigation** | Aan | Voegt knoppen voor inzoomen, uitzoomen en herstellen toe terwijl de navigatiecamera blijft volgen. |

## Advertentie- en trackingdiensten

**Remove telemetry** en **Hide ads and clutter** verminderen de genoemde advertentie- en meetfuncties. Maps blijft Google-diensten gebruiken voor kaarten, zoeken, routes en ingeschakelde accountfuncties. De gekozen Waze- of Flitsmeister-bron blijft zijn eigen diensten gebruiken.

Externe navigatiegegevens worden lokaal ontvangen van de gekozen app op dezelfde telefoon. Cameralagen van OpenStreetMap downloaden cameraposities in de buurt. **Avoid Flock cameras** stuurt het begin en einde van de route naar `brouter.de` wanneer dit aanstaat. Deze functies beginnen uit in de instellingen voor externe gegevens.

## Voor gebruik

De koppeling met native FM-gebeurtenissen en spraak wordt nog integraal gevalideerd. De opname door de vastgelegde bronversie heeft de dekkingscontrole doorstaan; audio, routering en levensduur van de ontvanger worden verder gecontroleerd. De categorie-/audiokeuze, het geparkeerde voorbeeld en herstel staan in de [instellingen voor native waarschuwingen](../guides/navigation-data.md#native-waarschuwingsaudio-van-flitsmeister).

Bijgewerkte navigatiegegevenspatches voegen stil starten en controle van de bronmotor toe. Na eenmalige broninstellingen en locatietoegang start Maps/Vela de gekozen Waze- of Flitsmeister-motor zonder het scherm te openen en probeert herstelbare onderbrekingen opnieuw tijdens kaartgebruik. Patch beide kanten opnieuw voor de start-API; de oudere Share alerts-koppeling blijft handmatig starten vereisen. Zie de [installatiegids voor navigatiegegevens](../guides/navigation-data.md).

Laat **Change package name** geselecteerd om naast de oorspronkelijke Maps-app te installeren; de standaardpakketnaam is `org.ungoogled.android.apps.maps`. **Change app name** gebruikt standaard **Ungoogled Maps** en behoudt je gekozen naam bij beide microG-patches. **Add microG-RE support** wijzigt de standaardpakketnaam naar `org.ungoogled.android.apps.maps.microg`. De twee microG-patches zijn alternatieven: kies alleen de variant die bij je omgeving past. Originele microG vereist een ROM met signature spoofing en behoudt de oorspronkelijke Maps-pakketnaam.

Kies bij **App icon** in **Change app name** uit Classic pin, Outlined pin, Classic map tile, Raised map pin, Gradient pin of Folded map, elk met een lichte of donkere achtergrond. **Automatic** volgt de patch **Legacy icon**; **Original Google Maps** behoudt Googles oorspronkelijke launchericoon. Een expliciete keuze gaat vóór Legacy icon. De keuzes gelden voor het gewone en ronde launchericoon. Een expliciete keuze behoudt het oorspronkelijke opstartscherm; afbeeldingen in de app volgen je overige gekozen patches, zoals Blue pin. Bij Automatic behoudt Legacy icon zijn bestaande wijziging van het opstartscherm. Naam en icoon zijn onafhankelijk instelbaar; voor een wijziging moet de APK opnieuw worden gepatcht.

Laat voor native autonavigatie **Native navigation for Gearslip and Android Auto** geselecteerd, installeer de volledige Android Auto-app en geef de gepatchte Maps-app toegang tot de exacte locatie. Gearslip vindt dan één navigatietegel in plaats van Maps als berichtenapp te behandelen. Installeer de gepatchte APK voor Android Auto via KingInstaller en controleer het installatieresultaat; gebruik de Shizuku-modus als de normale methode niet de verwachte installer vastlegt. Kies **Ungoogled Maps** of je eigen appnaam in de autolauncher, zodat de sessie de gepatchte versie gebruikt.

Open **Customization** en **Waze & external data source** in het accountmenu om de gekozen patches in te stellen. **Aan** in de tabel betekent dat de patch standaard geselecteerd is tijdens het patchen; cameralagen en externe bronnen moeten daarnaast in de app worden aangezet. **120 refresh rate** begint ook uit in Customization.

Volg de [handleiding voor navigatiegegevens](../guides/navigation-data.md) om een bron te patchen, die in Maps te kiezen en de compacte snelheidsweergave, kaartspelden en voorwaarschuwingen in te stellen. Daar staan ook de aparte oude Waze-modus, gedeeltelijke gemiddelden en terugval bij verlopen gegevens uitgelegd.

**Remove permissions** schakelt spraakzoeken, Lens en Live View uit door de wijzigingen aan microfoon- en cameratoegang. De microG-RE-accountversie behoudt de bijbehorende accountrechten en -diensten. **Proxy** heeft een ondersteunde netwerkengine nodig; volg de beschikbaarheidsmelding in de app. **Follow playing media** vereist meldingstoegang en is niet bedoeld voor root mount-installaties. De rechtstreekse media-integratie van YouTube Music en Spotify blijft afhankelijk van Google’s eigen Maps; gebruik waar mogelijk de optie via meldingen.

[Installeren](../guides/install.md) · [Patches kiezen](../guides/choose-patches.md) · [Compatibiliteit en standaardkeuzes](../reference/catalogue.md)
