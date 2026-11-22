BOODSCHAPPEN & WEEKMENU - VERSIE 6

- compact boodschappenoverzicht, een regel per product
- hoeveelheid inline aanklikbaar
- product toevoegen opent zoeken, vaak gebruikt en alle producten
- slepen na kort vasthouden met haptische feedback
- Gepakt standaard ingeklapt
- compact weekmenu met een regel per dag
- meerdere weekmenu's kiezen
- weekmenu's beheren onder Lijsten
- producten bij een dag kiezen en aan boodschappenlijst toevoegen
- herkenbaar winkelwagenicoon
- applicationId: nl.reneoosterkamp.boodschappen

Upload de inhoud over het bestaande GitHub-project. Laat .github staan.
Visual in this file was generated with Microsoft Copilot.

VERSIE 6.1 - VASTE ONDERTEKENING
Deze build gebruikt voortaan een vaste private signing key. Daardoor worden toekomstige APK-updates door Android als updates van dezelfde app geaccepteerd.
Belangrijk: de nu geinstalleerde app is nog met een tijdelijke GitHub Actions debug-key ondertekend. Die moet voor deze overgang eenmalig worden verwijderd. Installeer daarna versie 6.1. Vanaf versie 6.1 hoeft verwijderen niet meer, zolang dezelfde signing key behouden blijft.
De signing key is uitsluitend bedoeld voor deze prive-app en deze private repository. Maak een lokale backup van app/keystore/oosterkamp-grocery.jks.

VERSIE 6.2
- slepen over onbeperkt aantal posities; nieuwe positie wordt bij loslaten opgeslagen
- product blijft tijdens slepen zichtbaar onder de vinger
- Lijsten-scherm herschreven om de crash te voorkomen
- Producten sorteren op A-Z of Vaak gebruikt
- Weekmenu kiest uitsluitend uit aparte Weekmenu's-lijst
- Weekmenu's staan alleen onder Weekmenu en Lijsten, nooit onder Boodschappen
- vaste ondertekeningssleutel behouden voor normale update over versie 6.1
