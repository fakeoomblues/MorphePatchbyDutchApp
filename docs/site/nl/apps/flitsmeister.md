# Flitsmeister

**Appversie:** 14.17 · **Build:** 1287 · **Origineel bestand:** APKS

**Ook ondersteund:** 15.0/1289. APKM- en APKS-invoer moeten het volledige splitarchief bevatten.

[Download ondersteund origineel van 14.17](https://buzzheavier.com/1esn82va0l3d){ .md-button .md-button--primary }

[Flitsmeister installeren voor Android Auto: stappen met screenshots](../guides/android-auto.md){ .md-button }

Selecteer het exacte originele bestand in Morphe Manager. Gebruik voor 15.0 het volledige archief van build 1289; beide onderzochte bundels met minimum-SDK 29 en 32 worden ondersteund. De download hierboven blijft het ondersteunde origineel van 14.17.

[Waze of Flitsmeister koppelen aan Google Maps en Vela Maps](../guides/navigation-data.md){ .md-button }

## Patches

| Patch | Standaard | Wat verandert er? |
| --- | --- | --- |
| **Broadcast navigation data** | Aan | Deelt actuele snelheid, geldende limieten, wegmeldingen en naderende/actieve trajectcontroles via hetzelfde lokale contract als Waze. Geen overlay nodig. |
| **Disable partner services** | Aan | Verwijdert Parkeren, Tol en Carwash en de bijbehorende instellingen. |
| **Disable tracking** | Aan | Stopt de ondersteunde optionele gebruiksstatistieken, advertentie-ID’s en diagnostiek. |
| **Fix PairIP startup** | Aan | Laat de gepatchte app na installatie starten en de ondersteunde functies gebruiken. Laat deze patch aan. |
| **Guest mode** | Aan | Voegt Doorgaan als gast toe, laat de app zonder inloggen openen en verbergt het grote accountaanmaakblok. |
| **Hide Pro notices** | Aan | Verbergt automatische Pro/Plus-aanbiedingen, badges en Android Auto-upgradeberichten. |
| **Hide ads** | Aan | Verwijdert commerciële locatiepromoties, hun afbeeldingen en reclamemeldingen. Verbergt in 15.0 aanbiedingen voor niet-gekochte betaalde soundpacks; bruikbare stemmen blijven selecteerbaar. Verkeerswaarschuwingen blijven. |
| **Support Gearslip and DriveDeck** | Aan | Accepteert Gearslip en DriveDeck als autohost op pakketnaam, met welke sleutel ze ook zijn ondertekend, zodat DriveDeck werkt zonder dat de originele Gearslip is geïnstalleerd. Andere hosts houden de Android Auto-validatie. |
| **Unlock Pro** | Aan | Schakelt lokaal geregelde Pro-opties in. De menu-avatar krijgt in 15.0 de eigen PRO-badge van de app als cosmetische wijziging. Functies waarvoor accountactivering nodig is blijven afhankelijk van de dienst. |

## Advertentie- en trackingdiensten

De advertentie- en trackingpatches beperken de ondersteunde optionele activiteit van: Firebase, Insider; automatische gebruiks- en locatiemetingen; commerciële locatiegebonden advertenties.

Metingen blokkeren blokkeert niet elke functie van dezelfde aanbieder. Noodzakelijke appdiensten kunnen nog verbinding maken.

## Voor gebruik

De koppeling met native FM-gebeurtenissen en spraak wordt nog integraal gevalideerd. Native gebeurtenisopname geldt voor 14.17 / 1287; 15.0 behoudt de bestaande gegevens-/startkoppeling en biedt dit gebeurtenisprotocol niet aan. De opname door de vastgelegde bronversie heeft de dekkingscontrole doorstaan; audio, routering en levensduur van de ontvanger worden verder gecontroleerd. De categorie-/audiokeuze, het geparkeerde voorbeeld en herstel staan in de [instellingen voor native waarschuwingen](../guides/navigation-data.md#native-waarschuwingsaudio-van-flitsmeister).

Het herstelde opstart-/nativeprofiel richt zich op ARM64. Op een Android 16-telefoon zijn kaartstart, HUD, pictogram wijzigen en opnieuw openen, de profielbadge en het verbergen van soundpack-aanbiedingen gecontroleerd. Serverrechten en een fysieke Android Auto-headunit blijven afzonderlijke controles.

Bijgewerkte navigatiegegevenspatches voegen stil starten en controle van de bronmotor toe. Na eenmalige broninstellingen en locatietoegang start Maps/Vela de gekozen Waze- of Flitsmeister-motor zonder het scherm te openen en probeert herstelbare onderbrekingen opnieuw tijdens kaartgebruik. Patch beide kanten opnieuw voor de start-API; de oudere Share alerts-koppeling blijft handmatig starten vereisen. Zie de [installatiegids voor navigatiegegevens](../guides/navigation-data.md).

**Broadcast navigation data** werkt zolang de rij-/locatiedienst met locatietoegang op de achtergrond draait. De overlay mag uit blijven. Ontvangende apps gebruiken dezelfde actie en velden voor Flitsmeister en Waze. De bron levert geschatte afstand vóór trajectinrit en beschikbare meldingen van vaste/mobiele flitsers, politie, gevaren, ongevallen, files en afsluitingen. Gemiddelden gaan alleen over het waargenomen deel; berekende voortgang wordt als schatting gemarkeerd. [Gedeeld broadcastcontract](https://codeberg.org/JohninNL/nl-morphe-patches/src/branch/main/docs/navigation-broadcasts.md).

Navigatie, live verkeers-/locatie-uitwisseling, accounts en meldingen gebruiken nog steeds online diensten. Unlock Pro activeert geen serverdiensten. Volg voor Android Auto de [KingInstaller-installatiestappen](../guides/android-auto.md).

Kies **Doorgaan als gast** om de introductie zonder account af te ronden. Je kiest zelf of je locatietoestemming geeft. Optioneel inloggen blijft beschikbaar. Gastmodus activeert geen accountdiensten, zoals ritgeschiedenis in de cloud of parkeren. De patch herstelt Google-inloggen niet en verwijdert de afzonderlijke accountvereiste van Android Auto niet.

Doorgaan als gast en de kaart openen zijn op één telefoon getest. Het verbergen van het accountaanmaakblok moet nog op een toestel worden getest.

[Installeren](../guides/install.md) · [Patches kiezen](../guides/choose-patches.md) · [Compatibiliteit en standaardkeuzes](../reference/catalogue.md)
