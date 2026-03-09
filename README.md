<p align="center">
<img width="520" height="152" alt="image" src="https://github.com/user-attachments/assets/2efa3867-c90f-41cf-a469-b11688e308a1" />
</p>

<p align="center">
  <img src="https://img.shields.io/github/created-at/pablolird/blackjack"/>
  <img src="https://img.shields.io/github/contributors/pablolird/blackjack"/>
</p>

---

<p align="center">
  <img src="https://img.shields.io/badge/Java-ED8B00?logo=openjdk&logoColor=fff&style=for-the-badge" alt="Java Badge"/>
  <img src="https://img.shields.io/badge/libGDX-E74C3C?logoColor=fff&style=for-the-badge" alt="libGDX Badge"/>
  <img src="https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=fff&style=for-the-badge" alt="Gradle Badge"/>
  <img src="https://img.shields.io/badge/KryoNet-5C2D91?logoColor=fff&style=for-the-badge" alt="KryoNet Badge"/>
</p>

# 🃏 Blackjack

**Blackjack** is a fully-featured card game built with [libGDX](https://libgdx.com/), supporting both local multiplayer (same machine, multiple players) and networked multiplayer (host/join over LAN). Featuring card animations, a betting system, and a clean Entity Component System architecture.

---



https://github.com/user-attachments/assets/e9205575-d682-44c8-af90-ce2d8eb57cfb



---

## 🌟 Features

* **Local Multiplayer**: 2–7 players take turns on the same machine.
* **Network Multiplayer**: Host a game or join one over LAN using an IP address.
* **Betting System**: Each player starts with a balance and places bets each round.
* **Card Animations**: Smooth dealing and card movement sequences.
* **Turn Timer**: 15-second countdown per player action — no stalling.
* **Five Card Charlie**: Win automatically with 5 cards under 21.
* **Double Down**: Double your bet and take exactly one more card.
* **Restart / Exit**: Restart or exit the match at any time from in-game.
* **ECS Architecture**: Modular Entity Component System for clean game object management.

---

## 🎮 Game Modes

### 🏠 Local Game

<img width="2290" height="1276" alt="image" src="https://github.com/user-attachments/assets/9d310884-df25-4679-abb6-08b98b72bda8" />


Multiple players on a single machine take turns. Each player has their own hand, balance, and action controls.

### 🌐 Host a Game

<img width="1584" height="872" alt="image" src="https://github.com/user-attachments/assets/85b50907-bb1d-4700-9f20-55ed54eb0a5d" />

Create a server and wait for other players to connect over your local network. You control when the game starts.

### 🔗 Join a Game

<img width="1574" height="872" alt="image" src="https://github.com/user-attachments/assets/dd29721a-28d3-477f-8900-2fc9bce5eec7" />


Enter the host's IP address to connect and join the table as a client.

---

## 🛠️ Technologies Used

| Technology | Role |
|---|---|
| **Java 8+** | Core language |
| **libGDX 1.13.1** | 2D game framework (rendering, input, audio) |
| **LWJGL3** | Desktop platform backend (Windows, macOS, Linux) |
| **KryoNet 2.22.9** | TCP/UDP networking and packet serialization |
| **Scene2D** | UI layout system (stages, tables, skins) |
| **Gradle** | Build system |

---

## 📂 Project Structure

```
blackjack/
│
├── core/                          # Shared game logic (platform-independent)
│   └── src/main/java/com/badlogic/blackjack/
│       ├── animation/             # Animation sequencer and actions
│       ├── assets/                # Asset manager wrapper
│       ├── audio/                 # Sound and music management
│       ├── ecs/                   # Entity Component System
│       │   ├── components/        # CCard, CSprite, CAnimation, etc.
│       │   ├── systems/           # RenderSystem
│       │   ├── Entity.java
│       │   ├── EntityManager.java
│       │   └── ECS.java
│       ├── game/                  # Core blackjack logic
│       │   ├── BlackjackLogic.java
│       │   ├── Card.java / Deck.java
│       │   ├── Player.java / Dealer.java
│       │   └── GameState.java
│       ├── lobby/                 # Host and client lobby screens
│       ├── network/               # GameClient, GameServer, NetworkPacket
│       ├── ui/                    # UI components (PlayerWindow, GameButton, Timer)
│       ├── GameScreen.java        # Main in-game screen
│       ├── StartScreen.java       # Main menu
│       └── Main.java              # Entry point
│
├── lwjgl3/                        # Desktop launcher module
│   ├── src/.../Lwjgl3Launcher.java
│   └── icons/                     # App icons (.icns, .ico, .png)
│
├── assets/                        # Game assets
│   ├── *.png                      # Card sprites, backgrounds
│   ├── *.wav / *.mp3              # Sound effects and music
│   ├── fonts/                     # Bitmap fonts
│   └── SKIN_JSON2/                # UI skin definition
│
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## 🚀 Setup and Installation

### Prerequisites

* **Java JDK 8+** — [Download here](https://adoptium.net/)
* No other installs needed — Gradle wrapper is included.

### Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/pablolird/blackjack.git
   cd blackjack
   ```

2. **Run the game:**
   ```bash
   ./gradlew lwjgl3:run
   ```
   On Windows:
   ```bash
   gradlew.bat lwjgl3:run
   ```

3. **Build a standalone JAR:**
   ```bash
   ./gradlew lwjgl3:jar
   java -jar lwjgl3/build/libs/Blackjack-1.0.0.jar
   ```
   Or build for a specific platform:
   ```bash
   ./gradlew jarMac    # macOS
   ./gradlew jarLinux  # Linux
   ./gradlew jarWin    # Windows
   ```

---

## 🎮 How to Play

### Starting the Game

1. Launch the game with `./gradlew lwjgl3:run`.
2. From the main menu, choose a mode:
   * **Local Game** — select the number of players and start.
   * **Host Game** — create a server; share your local IP with others to join.
   * **Join Game** — enter the host's IP address to connect.

### Gameplay

1. **Betting Phase**: Each player places their bet using the bet controls.
2. **Dealing**: Cards are dealt to all players and the dealer.
3. **Player Turns**: On your turn, choose an action:
   * **Hit** — draw another card.
   * **Stand** — end your turn.
   * **Double Down** — double your bet and take one final card.
4. **Dealer Turn**: The dealer draws until reaching 17 or higher.
5. **Resolution**: Hands are compared. Bust = lose. Closer to 21 = win. Blackjack beats everything.
6. **Five Card Charlie**: Automatically win if you hold 5 cards without busting.

### Network Multiplayer Notes

* The host machine must have TCP port `54555` and UDP port `54777` accessible on the local network.
* Clients enter the host's LAN IP (e.g. `192.168.x.x`) to connect.
* The host starts the game from the lobby once all players have joined.

---

## ⌨️ IDE Setup

Generate project files for your preferred IDE:

```bash
./gradlew idea     # IntelliJ IDEA
./gradlew eclipse  # Eclipse
```

---

Enjoy the game — and may the dealer always bust! 🃏
