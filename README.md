# Warkanoid

Warkanoid est une relecture moderne d'Arkanoid propulsee par libGDX. Le projet vise une jouabilite arcade propre, un rendu soigne et un pipeline d'effets parametriques directement dans le jeu.

## Points clefs
- **Physique robuste** : collisions raycast sur les briques, boule, pala et murs, gestion sticky et reflexions precises.
- **Trail et ombres** : trainee dynamique du projectile, pipeline d'ombres unifie avec blur multi-pass parametre.
- **Power-ups completes** : multi-balle, lasers, paddle collant, modificateurs de taille et de vitesse.
- **Effets visuels CRT** : shader post-process parametrique (courbure, aberration, scanlines, vignette, bruit) avec HUD temps reel.
- **Feedbacks** : particules de destruction, bombes en chaine, lasers, HUD combo et debug (F9) pour le pipeline d'ombre.

## Lancer le jeu
```bash
./gradlew lwjgl3:run
```
Sous Windows :
```powershell
.\gradlew.bat lwjgl3:run
```
Le projet charge automatiquement les assets depuis `assets/`.

## Commandes par defaut
| Action | Touche |
| --- | --- |
| Deplacer le paddle | Souris ou fleches gauche/droite |
| Lancer / tirer (mode laser) | Espace |
| Tester un power-up | 1..8 |
| Switch niveaux | F1..F6 |
| Basculer modes de collisions | F7 |
| Debug ombres (normal / raw / blur / off) | F9 |
| HUD CRT (sliders) | F10 |
| Relancer apres Game Over | R |

## HUD CRT (F10)
Un overlay Scene2D permet d'ajuster en direct :
- Curvature (courbure de l'ecran)
- Aberration RGB (base / force)
- Amplitude, frequence, vitesse des scanlines
- Vignettage (scale, power, min/max)
- Bruit (amount, speed)

Les reglages sont exposes via `Main.CrtSettings`; le HUD ecrit dans cette structure en direct, ce qui simplifie le prototypage des valeurs par defaut.

## Debug ombres (F9)
Le raccourci bascule entre :
1. `NORMAL` : ombre finale noire diffuse.
2. `RAW MASK` : silhouettes brutes rouges (sans blur).
3. `BLURRED MASK` : masque floute cyan pour verifier le rayon.
4. `OFF` : pipeline desactive.

Pratique pour verifier l'effet du blur multi-pass et ajuster `SHADOW_BLUR_RADIUS` / iterations.

## Arborescence
```
core/
  src/main/java/io/github/warnotte/warkanoid/
    Main.java                <- boucle principale, pipeline render, HUDs
    ...                      <- entites (Ball, Paddle, PowerUp, etc.)
    ui/CrtSettingsOverlay.java
lwjgl3/                      <- lanceur desktop
html/                        <- cible GWT/Web (optionnelle)
assets/                      <- textures, sons, fonts
```

## Scripts utiles
- `./gradlew core:compileJava` : compile la logique principale.
- `./gradlew core:test` : lance les tests unitaires (si presents).
- `./gradlew lwjgl3:run -Pdebug=true` : possible de passer des system props pour activer des logs (ajustez selon vos besoins).

## Roadmap suggeree
- Ajouter un ecran de demarrage + options (volume, remapping touches).
- Sauvegarder les valeurs CRT dans un fichier de config utilisateur.
- Externaliser la definition des niveaux (JSON ou Tiled).
- Ajouter un mode scoring avec table des meilleurs scores.

## Credits
- Projet base sur un template `gdx-liftoff`.
- Sons retro dans `assets/sounds/` (licence a verifier selon usage).
- Merci a la communaute libGDX pour les retours, et aux references Arkanoid/Taito pour l'inspiration.

Retours et contributions bienvenus !
