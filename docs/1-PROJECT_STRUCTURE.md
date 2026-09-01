# 📁 Project Structure

```text
storybox/
├── pom.xml                               # Maven project definition (Spring Boot 3.4.0, Spring AI, Pi4J)
├── mvnw / mvnw.cmd                       # Maven wrapper scripts
├── CLAUDE.md                             # AI coding agent architecture & commands reference
├── LICENSE                               # Apache License 2.0
├── README.md                             # Project documentation
├── audio/                                # Audio library assets (resolved at runtime)
│   ├── cues/                             # Audio prompts for axis choices (e.g., hero-chevalier.mp3)
│   └── stories/                          # Story audio files (e.g., chevalier-dragon-chateau-epee.mp3)
├── docs/                                 # Technical documentation
│   ├── 1-PROJECT_STRUCTURE.md            # Project structure overview
│   ├── 2-SETUP_AND_RUN.md                # Setup and run commands guide
│   ├── 3-HARDWARE_AND_WIRING.md          # KY-040 rotary encoder pinout & notes
│   ├── 4-AUTOMATION_SCRIPTS.md           # Automation scripts guide
│   ├── 5-CONFIGURATION.md                # Environment variables & configuration properties
│   ├── 6-CONTENT_AUTHORING.md            # Content authoring and TTS generation workflow
│   ├── 7-TESTS.md                        # Test suite architecture and execution
│   └── sprints/                          # Project evolution & milestone specs
│       ├── Iteration-1.md ... Iteration-4.md
│       └── Roadmap.md
├── scripts/                              # Deployment and content generation tools
│   ├── setup-pi.sh                       # One-shot Raspberry Pi environment setup
│   ├── deploy.sh                         # Remote build, sync, and systemd deployment script
│   ├── storybox.service                  # Systemd user service definition for auto-start
│   ├── stories/                          # Batch story text generation (LLM)
│   │   ├── README.md                     # Documentation for text authoring
│   │   └── gen-stories.sh                # Ollama / Thaura batch generator
│   └── tts/                              # Batch audio synthesis (TTS)
│       ├── README.md                     # Documentation for TTS synthesis
│       ├── gen-audio.sh                  # Multi-engine audio batch generator
│       ├── kokoro/                       # Kokoro Python project (pyproject.toml, kokoro_tts.py)
│       └── chatterbox/                   # Chatterbox Python project (pyproject.toml, chatterbox_tts.py)
├── src/
│   ├── main/
│   │   ├── java/com/storybox/storybox/   # Spring Boot application source code
│   │   │   ├── StoryboxApplication.java  # Main application entry point
│   │   │   ├── StoryboxConfiguration.java# Configuration bean setup
│   │   │   ├── StoryboxProperties.java   # Type-safe @ConfigurationProperties (storybox.*)
│   │   │   ├── audio/                    # Audio playback subsystem
│   │   │   │   ├── AudioPlayer.java      # Process-based mpv player with barge-in
│   │   │   │   └── AudioController.java  # REST endpoint for audio testing (/audio/*)
│   │   │   ├── gpio/                     # Hardware GPIO subsystem (Pi4J)
│   │   │   │   ├── RotaryEncoderService.java # KY-040 encoder & button event listener
│   │   │   │   ├── RotaryEvent.java      # Encoder event model (CW, CCW, PRESS)
│   │   │   │   └── RotaryListener.java   # Event callback interface
│   │   │   ├── health/                   # Observability subsystem
│   │   │   │   └── StoryboxHealthIndicator.java # Custom Actuator health check
│   │   │   └── story/                    # Story engine & domain logic
│   │   │       ├── StoryLibrary.java     # JSON story pack loader and resolver
│   │   │       ├── controller/           # REST endpoints
│   │   │       │   ├── StoryController.java         # State machine simulator (/story/*)
│   │   │       │   └── StoryGeneratorController.java# AI text generator test harness (/generator/*)
│   │   │       ├── model/                # Domain models (StoryAxis, StoryChoice, StorySelection, etc.)
│   │   │       └── service/              # Core business services
│   │   │           ├── StoryEngine.java  # Finite state machine orchestrator
│   │   │           └── StoryGenerator.java # Spring AI ChatClient story text generator
│   │   └── resources/
│   │       ├── application.properties    # Base Spring Boot configuration
│   │       ├── application-dev.properties# Local development profile (GPIO disabled, local audio)
│   │       ├── application-pi.properties # Raspberry Pi profile (GPIO enabled, remote paths)
│   │       └── stories.json              # Story pack definitions (axes, choices, authored stories)
│   └── test/java/com/storybox/storybox/  # Automated test suite (JUnit 5, Mockito, WebMvcTest)
│       ├── StoryboxApplicationTests.java # Context loading test
│       ├── audio/AudioControllerTest.java# Audio REST controller tests
│       └── story/                        # Engine and controller unit/integration tests
└── stories-src/                          # Generated/edited raw story text files (.txt)
```
