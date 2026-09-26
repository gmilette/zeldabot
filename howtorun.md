# How to run Zeldabot

## Acquire Nintaco Emulator
* [download Nintaco](https://nintaco.com/)
* Unzip it

## Download Zeldabot
* Download the release you want to run
* Doubleclick to execute

## Start
* Run nintaco.jar by double clicking on it
* Load legend of zelda game
* Open Tools -> Start Program Server
* Click start server
* Then close the window

## Run from source (development)

Everything runs from the `paths/` directory — `DirectoryConstants` resolves output and
save-state paths relative to the working directory.

```
cd paths
./gradlew run --args="<experiment> dev"
```

* `<experiment>` is the first argument: an experiment name from `Experiments.kt`
  (`level1`, `all`, `level8`, …), `<level>_<mapLoc>` to start mid-plan at a screen, or
  `room_<level>_<mapLoc>` to repeat one room as a trial.
* `dev` enables CSV/log output under `../../botoutput/`. Without it nothing is written.
* `noui` skips the Compose window.

For repeated trials and A/B comparison see [NOTES-ab-testing.md](NOTES-ab-testing.md).
