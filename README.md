# CrimsonBann - Professionelles Bann-System

CrimsonBann ist ein komplettes Bann-System für Minecraft Server mit allen wichtigen Funktionen die moderne Server benötigen. Das Plugin bietet einfache Handhabung bei gleichzeitiger Professionalität.

---

## Hauptfunktionen

### Bann-System
- Permanente und zeitlich begrenzte Banns
- IP-Banns zur Verhinderung von Umgehungen  
- Detaillierte Bann-Informationen mit Zeitstempeln
- Nachträgliche Änderung von Bann-Grund und Dauer
- Automatischer Admin-Display bei Bann-Aktionen

### Benutzeroberfläche
- Übersichtliche GUI mit echten Spielerköpfen
- Alle Banns auf einen Blick sichtbar
- Direkte Aktionen aus dem GUI heraus möglich
- Schneller Zugriff auf alle wichtigen Funktionen

### Discord Integration
- Automatische Benachrichtigungen bei allen Bann-Aktionen
- Farblich unterschiedliche Nachrichten für verschiedene Aktionen
- Voll anpassbare Webhook-Nachrichten
- Professionelle Darstellung in deinem Discord

### Konfiguration
- Vordefinierte Bann-Gründe für schnelle Anwendung
- Anpassbare Nachrichten und Texte
- Detaillierte Berechtigungs-Einstellungen

---

## Installation

### 1. Plugin kompilieren
Öffne das Maven Tool Fenster in IntelliJ und klicke auf "clean" dann "package". Die fertige Plugin-Datei findest du unter target/CrimsonBann.jar.

### 2. Server einrichten
Lade PaperMC 1.21.1 von papermc.io herunter und erstelle einen test-server Ordner. Kopiere die jar-Datei dorthin und starte den Server einmal mit:
```
java -Xms1G -Xmx2G -jar paper-1.21.1.jar nogui
```
Akzeptiere die EULA und stoppe den Server wieder.

### 3. Plugin installieren
Erstelle einen plugins Ordner und kopiere CrimsonBann.jar hinein. Starte den Server neu.

---

## Befehle

### Bann-Befehle
- `/ban <spieler> <grund>` - Permanent bannen
- `/tempban <spieler> <dauer> <grund>` - Zeitweiser Bann
- `/ipban <spieler> <dauer|perm> <grund>` - IP-Bann
- `/unban <spieler>` - Spieler entbannen

### Admin-Befehle
- `/bannlist` - Ban-Liste GUI öffnen
- `/editbanreason <spieler> <neuer grund>` - Bann-Grund ändern
- `/editbantime <spieler> <neue dauer|perm>` - Bann-Dauer ändern

### Beispiele
```
/ban Notch Hacking
/tempban Steve 30m Spam
/ipban Alex perm Werbung
/bannlist
/editbanreason Steve Toxisches Verhalten
/editbantime Steve 7d
```

---

## Konfiguration

Bearbeite die Datei plugins/CrimsonBann/config.yml:

```yaml
# Discord Webhook
discord:
  enabled: true
  url: "DEINE_WEBHOOK_URL_HIER"
  username: "CrimsonBann"

# Ban Templates
ban-templates:
  - name: "Hacking"
    reason: "Verwendung von verbotenen Hacks/Cheats"
    default-duration: "30d"
  - name: "Spam"
    reason: "Spam im Chat"
    default-duration: "1d"

# Nachrichten
messages:
  prefix: "&c[CrimsonBann]&r "
  ban-success: "&aSpieler &e{player} &awurde erfolgreich gebannt."
```

---

## Berechtigungen

```
crimisonbann.ban        # /ban, /tempban
crimisonbann.unban      # /unban  
crimisonbann.ipban      # /ipban
crimisonbann.bannlist   # /bannlist
crimisonbann.bypass     # Schutz vor Banns
crimisonbann.admin      # Admin-Display
```

---

## Funktionen

- Spieler permanent und zeitweise bannen
- IP-Banns mit Umgehungsschutz
- GUI mit echten Spielerköpfen
- Detaillierte Bann-Informationen
- Grund und Dauer nachträglich ändern
- Admin-Display bei Bann-Aktionen
- Discord Webhook Integration
- Granulare Berechtigungen

---

## Support

Für weitere Plugins und Support besuche unseren Discord Server:
discord.gg/crimsonservice

---

Dieses Plugin wurde von Nigtmehrooo für die CrimsonService Community entwickelt.
