# croviera

```
the only tester for this app is my girlfriend. shoutout to her. i love you.
```

a local-first android music player with a terminal soul and an amoled problem.

croviera is what happens when someone looks at a music player and thinks
"what if every single pixel was configurable, and also what if it was held
together by hope and gradle". it plays music. it also plays with your patience.

---

## the honest disclaimer

this is a horrible project. it is a vibecoded mess. i am aware of this.
i have made peace with this. (´･ω･`)

it started as a prototype, got out of hand, and is now a full app with a
rebrand and a version number that finally starts at 1.0. please do not look
at the commit history. please do look at the settings menu. (￣▽￣)ゞ

if something breaks, it is probably a feature. if it works, it is definitely
an accident. (╯°□°)╯︵ ┻━┻

---

## what it does

- plays local audio files from folders you choose
- amoled-black croviera theme with white text and a quietly dark red accent
- system24 and a pile of other themes are still there if you want them
- now playing card that expands when you tap the empty space
- lyrics support with sidecar .lrc files and a metadata editor for assigning one
- playlists with rename, reorder, custom covers, and json import/export
- stats with song metadata instead of ugly file paths
- artist pages show all songs first, with an albums button on top
- home screen widget with album art and playback controls
- full backup and restore for settings, stats, playlists, covers, and fonts

## customization (yes, all of it)

the settings screen is less of a settings screen and more of a warning label.

- move the top buttons to the top, bottom, left, or right
- rename every button and give it a nerd font glyph
- toggle nerd font icons instead of text
- set corner radius and border width
- pick a custom gradient for the custom theme
- custom background images belong to the saved custom theme, not every theme
- choose the now playing card title, expanded cover size, queue, and lyrics
- pick a tab transition: fade, cmatrix, slides, zoom, or none
- adjust font size, line height, row padding, cover size, and panel opacity
- install .ttf fonts from a file instead of pasting a url
- import stats and playlists from a file instead of pasting json

## the .lrc thing

croviera looks for lyrics next to the song, in the app lyrics folder, and in
mediastore. if it still cannot find them, you can assign a .lrc file in the
metadata editor. the app will even say:

```
You actually have an .lrc file? Apply one in the metadata editor!
```

yes, that exact sentence. with the space. (๑•̀ㅂ•́)و✧

## install

download the latest apk from the releases page:

```
https://github.com/emperornyctos/croviera/releases
```

install it, grant storage/audio permission, pick your music folders, and
pretend everything is fine. (・ω・)ノ

## build

you need android studio or a working command line android sdk.

```
git clone https://github.com/emperornyctos/croviera.git
cd croviera
./gradlew assembleRelease
```

the apk lands in `app/build/outputs/apk/release/`.

## license

mit. take it, change it, break it, fix it. just do not blame me. (´；ω；`)
