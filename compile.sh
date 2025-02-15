#!/bin/bash

# Charger la configuration
source config.conf

# Définir le dossier de destination
destinationFolder="$root/build/framework"

# Aller dans le répertoire src
cd "$root/framework/src" || exit

# Compiler les fichiers Java
javac -d "$destinationFolder" -cp "$root/lib/*" *.java

# Aller dans le dossier de build
cd "$root/build/framework" || exit

# Créer le fichier JAR
jar -cvf "$root/lib/framework.jar" .

echo "Processus terminé."
