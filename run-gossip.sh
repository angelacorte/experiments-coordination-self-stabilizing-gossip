#!/usr/bin/env sh
DESTINATION="$HOME/Downloads/gossip-basic-experiment-$(date -u "+%Y-%m-%d-%H.%M.%S")"
git clone https://github.com/angelacorte/experiments-coordination-self-stabilizing-gossip "$DESTINATION"
cd "$DESTINATION"
./gradlew runGossipGraphic